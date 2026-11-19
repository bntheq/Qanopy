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
    class Kayit(val zaman: String, val tablo: String, val anahtar: String, val eski: String?, val yeni: String?)

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

    fun tabloOku(tablo: String): Liste {
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

    fun tekOku(tablo: String, anahtar: String): Okuma {
        val s = kabuk("content query --uri content://settings/$tablo --where ${sq("name='$anahtar'")} --projection value")
        val c = s.cikti.trimEnd('\n')
        return when {
            c.startsWith("No result found") -> Okuma(null, null)
            c.startsWith("Row: 0 value=") -> Okuma(null, c.removePrefix("Row: 0 value="))
            else -> Okuma("($tablo/$anahtar) " + c.take(300), null)
        }
    }

    private fun gecmisDosya(ctx: Context) = File(ctx.filesDir, "gecmis.txt")

    // yeni == null -> ayari sil. Basari: null, hata: mesaj
    fun yaz(ctx: Context, tablo: String, anahtar: String, yeni: String?): String? {
        if (tablo !in tablolar) return "Geçersiz tablo"
        if (!anahtarDesen.matches(anahtar)) return "Ad geçersiz (harf, rakam, _ . : - kullan, en çok 100 karakter)"
        if (yeni != null && !duzMu(yeni)) return "Değerde satır sonu veya sekme olamaz"
        val e = tekOku(tablo, anahtar)
        if (e.hata != null) return e.hata
        val eski = e.deger
        if (eski != null && !duzMu(eski)) return "Eski değer çok satırlı, güvenle geri alınamaz. Yazılmadı."
        val j = kabuk("mkdir -p /data/adb/qanopy && printf '%s\\n' ${sq("$tablo|$anahtar|${eski ?: "NULL"}")} >> $JURNAL")
        if (j.kod != 0) return "Günlüğe yazılamadı, değişiklik YAPILMADI: " + j.cikti.take(200)
        val w = if (yeni == null) {
            kabuk("content delete --uri content://settings/$tablo --where ${sq("name='$anahtar'")}")
        } else {
            kabuk("content insert --uri content://settings/$tablo --bind ${sq("name:s:$anahtar")} --bind ${sq("value:s:$yeni")}")
        }
        if (w.kod != 0) return "Yazma hatası: " + w.cikti.take(300)
        val d = tekOku(tablo, anahtar)
        if (d.hata != null) return d.hata
        if (d.deger != yeni) return "Doğrulama BAŞARISIZ: beklenen=${yeni ?: "(yok)"} okunan=${d.deger ?: "(yok)"}"
        val zaman = SimpleDateFormat("dd.MM HH:mm:ss", Locale.US).format(Date())
        gecmisDosya(ctx).appendText("$zaman\t$tablo\t$anahtar\t${eski ?: "NULL"}\t${yeni ?: "NULL"}\n")
        return null
    }

    fun gecmis(ctx: Context): List<Kayit> {
        val f = gecmisDosya(ctx)
        if (!f.exists()) return emptyList()
        return f.readLines().takeLast(100).reversed().mapNotNull { s ->
            val p = s.split("\t", limit = 5)
            if (p.size < 5) null
            else Kayit(p[0], p[1], p[2], if (p[3] == "NULL") null else p[3], if (p[4] == "NULL") null else p[4])
        }
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
