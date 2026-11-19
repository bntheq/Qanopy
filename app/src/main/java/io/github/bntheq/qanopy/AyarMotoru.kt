package io.github.bntheq.qanopy

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.TimeUnit

object AyarMotoru {
    val tablolar = listOf("global", "system", "secure")
    private const val JURNAL = "/data/adb/qanopy/journal.txt"
    private val anahtarDesen = Regex("^[A-Za-z0-9_.:-]{1,100}$")

    class Sonuc(val kod: Int, val cikti: String)
    class Liste(val veri: Map<String, String>, val hata: String?)
    class Okuma(val hata: String?, val deger: String?)
    class Kayit(
        val zaman: String,
        val tablo: String,
        val anahtar: String,
        val eski: String?,
        val yeni: String?,
        val no: Int = 0,
        val kaynak: String = "qanopy",
        val hedef: Int = 0,
        val geriAlindi: Boolean = false,
        val geriAlinabilir: Boolean = false
    )
    class FarkSatir(val tamAd: String, val tur: Char, val once: String?, val sonra: String?, val qanopy: Boolean)

    private fun sq(s: String) = "'" + s.replace("'", "'\\''") + "'"
    private fun duzMu(s: String) = !s.contains('\n') && !s.contains('\r') && !s.contains('\t')

    fun kabuk(komut: String): Sonuc {
        return try {
            val p = ProcessBuilder("su", "-c", komut).redirectErrorStream(true).start()
            val cikti = p.inputStream.bufferedReader().readText()
            if (!p.waitFor(20, TimeUnit.SECONDS)) {
                p.destroy()
                Sonuc(-1, "Zaman aşımı")
            } else {
                Sonuc(p.exitValue(), cikti)
            }
        } catch (e: Exception) {
            Sonuc(-1, e.message ?: "bilinmeyen hata")
        }
    }

    private fun tabloOkuYavas(tablo: String): Liste {
        val s = kabuk("content query --uri content://settings/$tablo --projection name:value")
        val harita = TreeMap<String, String>()
        val desen = Regex("^Row: \\d+ name=(.*?), value=(.*)$")
        var ad: String? = null
        var dg = StringBuilder()
        for (satir in s.cikti.trimEnd('\n').lines()) {
            val m = desen.find(satir)
            if (m != null) {
                if (ad != null) harita[ad] = dg.toString()
                ad = m.groupValues[1]
                dg = StringBuilder(m.groupValues[2])
            } else if (ad != null) {
                dg.append("\\n").append(satir)
            }
        }
        if (ad != null) harita[ad] = dg.toString()
        if (harita.isEmpty()) {
            val c = s.cikti.trim()
            if (c.isNotEmpty() && !c.startsWith("No result found")) {
                return Liste(emptyMap(), "($tablo) " + c.take(300))
            }
        }
        return Liste(harita, null)
    }

    // cmd settings list: content'ten ~14 kat hizli. Beklenmedik cikti olursa eski yola doner.
    // cmd "null" yazar, content "NULL" yazar; eski bicimle uyum icin "NULL" kullaniyoruz.
    fun tabloOku(tablo: String): Liste {
        if (tablo !in tablolar) return tabloOkuYavas(tablo)
        val s = kabuk("cmd settings list $tablo < /dev/null")
        if (s.kod != 0) return tabloOkuYavas(tablo)
        val harita = TreeMap<String, String>()
        val desen = Regex("^([A-Za-z0-9_.:-]{1,100})=(.*)$")
        var ad: String? = null
        var dg = StringBuilder()
        for (satir in s.cikti.trimEnd('\n').lines()) {
            val m = desen.find(satir)
            if (m != null) {
                if (ad != null) {
                    val v = dg.toString()
                    harita[ad] = if (v == "null") "NULL" else v
                }
                ad = m.groupValues[1]
                dg = StringBuilder(m.groupValues[2])
            } else if (ad != null) {
                dg.append("\\n").append(satir)
            }
        }
        if (ad != null) {
            val v = dg.toString()
            harita[ad] = if (v == "null") "NULL" else v
        }
        if (harita.size < 10) return tabloOkuYavas(tablo)
        return Liste(harita, null)
    }

    fun tekOku(tablo: String, anahtar: String): Okuma {
        val s = kabuk("cmd settings get $tablo ${sq(anahtar)} < /dev/null")
        val c = s.cikti.trimEnd('\n')
        return when {
            s.kod != 0 -> Okuma("($tablo/$anahtar) " + c.take(300), null)
            c == "null" -> Okuma(null, null)
            else -> Okuma(null, c)
        }
    }

    @Volatile
    var baglam: Context? = null

    private fun uygulamaOku(tablo: String, anahtar: String): String? {
        val c = baglam ?: return null
        return try {
            when (tablo) {
                "global" -> android.provider.Settings.Global.getString(c.contentResolver, anahtar)
                "system" -> android.provider.Settings.System.getString(c.contentResolver, anahtar)
                "secure" -> android.provider.Settings.Secure.getString(c.contentResolver, anahtar)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Once uygulama icinden (root yok, hizli), bos donerse eski content yoluna duser
    fun hizliOku(tablo: String, anahtar: String): Okuma {
        val h = uygulamaOku(tablo, anahtar)
        return if (h != null) Okuma(null, h) else tekOku(tablo, anahtar)
    }

    private fun gecmisDosya(ctx: Context) = File(ctx.filesDir, "gecmis.txt")

    // yeni == null -> ayari sil. Basari: null, hata: mesaj
    fun yaz(ctx: Context, tablo: String, anahtar: String, yeni: String?, kaynak: String = "qanopy", hedef: Int = 0): String? {
        if (tablo !in tablolar) return "Geçersiz tablo"
        if (!anahtarDesen.matches(anahtar)) return "Ad geçersiz (harf, rakam, _ . : - kullan, en çok 100 karakter)"
        if (yeni != null && !duzMu(yeni)) return "Değerde satır sonu veya sekme olamaz"
        val e = hizliOku(tablo, anahtar)
        if (e.hata != null) return e.hata
        val eski = e.deger
        if (eski != null && !duzMu(eski)) return "Eski değer çok satırlı, güvenle geri alınamaz. Yazılmadı."
        val onEk = "$tablo|$anahtar|"
        val j = kabuk("mkdir -p /data/adb/qanopy && { grep -qF ${sq(onEk)} $JURNAL 2>/dev/null || printf '%s\\n' ${sq("$tablo|$anahtar|${eski ?: "NULL"}")} >> $JURNAL; }")
        if (j.kod != 0) return "Günlüğe yazılamadı, değişiklik YAPILMADI: " + j.cikti.take(200)
        val w = if (yeni == null) {
            kabuk("cmd settings delete $tablo ${sq(anahtar)} < /dev/null")
        } else {
            kabuk("cmd settings put $tablo ${sq(anahtar)} ${sq(yeni)} < /dev/null")
        }
        if (w.kod != 0) return "Yazma hatası: " + w.cikti.take(300)
        var d = hizliOku(tablo, anahtar)
        if (d.hata == null && d.deger != yeni) d = tekOku(tablo, anahtar)
        if (d.hata != null) return d.hata
        if (d.deger != yeni) return "Doğrulama BAŞARISIZ: beklenen=${yeni ?: "(yok)"} okunan=${d.deger ?: "(yok)"}"
        val zaman = SimpleDateFormat("dd.MM HH:mm:ss", Locale.US).format(Date())
        gecmisDosya(ctx).appendText("$zaman\t$tablo\t$anahtar\t${eski ?: "NULL"}\t${yeni ?: "NULL"}\t$kaynak\t$hedef\n")
        return null
    }

    private fun gecmisOku(ctx: Context): List<Kayit> {
        val f = gecmisDosya(ctx)
        if (!f.exists()) return emptyList()
        val ham = ArrayList<Kayit>()
        var no = 0
        for (s in f.readLines()) {
            no++
            val p = s.split("\t", limit = 7)
            if (p.size < 5) continue
            val kaynak = p.getOrNull(5)?.takeIf { it.isNotEmpty() } ?: "qanopy"
            val hedef = p.getOrNull(6)?.toIntOrNull() ?: 0
            ham.add(
                Kayit(
                    p[0], p[1], p[2],
                    if (p[3] == "NULL") null else p[3],
                    if (p[4] == "NULL") null else p[4],
                    no, kaynak, hedef
                )
            )
        }
        val geri = HashSet<Int>()
        for (k in ham) if (k.kaynak == "geri" && k.hedef > 0) geri.add(k.hedef)
        val bas = HashMap<String, Int>()
        for (k in ham) if (k.kaynak != "geri" && k.no !in geri) bas[k.tablo + "/" + k.anahtar] = k.no
        return ham.map { k ->
            val alindi = k.no in geri
            val olur = k.kaynak != "geri" && !alindi && bas[k.tablo + "/" + k.anahtar] == k.no
            Kayit(k.zaman, k.tablo, k.anahtar, k.eski, k.yeni, k.no, k.kaynak, k.hedef, alindi, olur)
        }
    }

    fun gecmis(ctx: Context): List<Kayit> = gecmisOku(ctx).takeLast(100).reversed()

    fun geriAl(ctx: Context, k: Kayit): String? {
        val g = gecmisOku(ctx).firstOrNull { it.no == k.no }
        if (g == null || !g.geriAlinabilir) return "Bu kayıt artık geri alınamaz (zaten geri alınmış ya da daha yeni bir kayıt var)."
        val su = hizliOku(g.tablo, g.anahtar)
        if (su.hata != null) return su.hata
        if (su.deger != g.yeni) {
            return "Ayar sonradan başka yerde değişmiş (şu an: ${su.deger ?: "(yok)"}, kayıttaki: ${g.yeni ?: "(yok)"}). Başkasının değişikliğini ezmemek için geri alınmadı."
        }
        return yaz(ctx, g.tablo, g.anahtar, g.eski, "geri", g.no)
    }

    fun farkSatirlari(ctx: Context, once: Map<String, String>, sonra: Map<String, String>): List<FarkSatir> {
        val son = HashMap<String, String?>()
        for (k in gecmisOku(ctx)) son[k.tablo + "/" + k.anahtar] = k.yeni
        val c = ArrayList<FarkSatir>()
        fun ekle(ad: String, tur: Char, o: String?, s: String?) {
            c.add(FarkSatir(ad, tur, o, s, son.containsKey(ad) && son[ad] == s))
        }
        for ((k, v) in sonra) {
            val o = once[k]
            if (o == null) ekle(k, '+', null, v)
            else if (o != v) ekle(k, '~', o, v)
        }
        for ((k, v) in once) {
            if (!sonra.containsKey(k)) ekle(k, '-', v, null)
        }
        return c
    }

    fun farkMetni(s: FarkSatir): String = when (s.tur) {
        '+' -> "+ ${s.tamAd} = ${kisalt(s.sonra ?: "")}"
        '~' -> "~ ${s.tamAd}: ${kisalt(s.once ?: "")} -> ${kisalt(s.sonra ?: "")}"
        else -> "- ${s.tamAd} (eskiden: ${kisalt(s.once ?: "")})"
    }

    fun cokSatirli(s: FarkSatir): Boolean =
        (s.once?.contains("\\n") == true) || (s.sonra?.contains("\\n") == true)

    fun farkDondur(ctx: Context, s: FarkSatir): String? {
        val i = s.tamAd.indexOf('/')
        if (i <= 0) return "Ayar adı çözülemedi."
        if (cokSatirli(s)) return "Çok satırlı değer, güvenle döndürülemez."
        val tb = s.tamAd.substring(0, i)
        val an = s.tamAd.substring(i + 1)
        val su = hizliOku(tb, an)
        if (su.hata != null) return su.hata
        if (su.deger != s.sonra) return "Ayar, Fark'ın okuduğundan sonra tekrar değişmiş (şu an: ${su.deger ?: "(yok)"}). Ezmemek için döndürülmedi; Sonra'ya tekrar bas."
        return yaz(ctx, tb, an, s.once, "fark", 0)
    }

    fun yazmaTesti(): List<String> {
        val k = "qanopy_test_x2"
        val sonuc = mutableListOf<String>()
        for (t in tablolar) {
            val a = kabuk("content insert --uri content://settings/$t --bind ${sq("name:s:$k")} --bind ${sq("value:s:1")}")
            val o1 = tekOku(t, k)
            val b = kabuk("content delete --uri content://settings/$t --where ${sq("name='$k'")}")
            val o2 = tekOku(t, k)
            val tamam = o1.hata == null && o1.deger == "1" && o2.hata == null && o2.deger == null
            if (tamam) {
                sonuc.add("$t: yazma ve silme ÇALIŞTI")
            } else {
                sonuc.add("$t: BAŞARISIZ ekle=[${a.cikti.trim().take(120)}] okunan=[${o1.hata ?: o1.deger}] sil=[${b.cikti.trim().take(120)}] sonra=[${o2.hata ?: o2.deger ?: "yok"}]")
            }
        }
        return sonuc
    }

    fun goruntu(): Pair<Map<String, String>, String?> {
        val hepsi = TreeMap<String, String>()
        for (t in tablolar) {
            val l = tabloOku(t)
            if (l.hata != null) return Pair(emptyMap(), l.hata)
            for ((k, v) in l.veri) hepsi["$t/$k"] = v
        }
        return Pair(hepsi, null)
    }

    private fun onceDosya(ctx: Context) = File(ctx.filesDir, "onceki.txt")

    fun onceKaydet(ctx: Context, v: Map<String, String>) {
        onceDosya(ctx).writeText(v.entries.joinToString("\n") { it.key + "\t" + it.value })
    }

    fun onceOku(ctx: Context): Map<String, String>? {
        val f = onceDosya(ctx)
        if (!f.exists()) return null
        val m = TreeMap<String, String>()
        for (s in f.readLines()) {
            val p = s.split("\t", limit = 2)
            if (p.size == 2) m[p[0]] = p[1]
        }
        return m
    }

    fun onceBilgi(ctx: Context): String {
        val f = onceDosya(ctx)
        if (!f.exists()) return "Kayitli 'Once' yok."
        val z = SimpleDateFormat("dd.MM HH:mm:ss", Locale.US).format(Date(f.lastModified()))
        return "Kayitli 'Once': $z"
    }

    private fun kisalt(s: String) = if (s.length > 200) s.take(200) + "…" else s

    fun fark(once: Map<String, String>, sonra: Map<String, String>): List<String> {
        val c = mutableListOf<String>()
        for ((k, v) in sonra) {
            val o = once[k]
            if (o == null) c.add("+ $k = ${kisalt(v)}")
            else if (o != v) c.add("~ $k: ${kisalt(o)} -> ${kisalt(v)}")
        }
        for ((k, v) in once) {
            if (!sonra.containsKey(k)) c.add("- $k (eskiden: ${kisalt(v)})")
        }
        return c
    }
}
