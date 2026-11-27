package io.github.bntheq.qanopy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.roundToInt

// ===== HARITA SAYFASI (sablon 256x480, ana ekran blogu 270 genis oldugu icin 7/2 kaydirilir)
private const val QH_DX = 7
private const val QH_DY = 2
private const val QH_BH = 30
private const val QH_BOSLUK = 4

// x, y, genislik, yukseklik ; 0-4 ince, 5 uzun, 6-8 kare ust satir, 9-11 kare alt satir
private val QH_YERLER: List<IntArray> = listOf(
    intArrayOf(128, 16, 112, 32),
    intArrayOf(128, 56, 112, 32),
    intArrayOf(128, 96, 112, 32),
    intArrayOf(128, 136, 112, 32),
    intArrayOf(128, 176, 112, 32),
    intArrayOf(16, 224, 224, 32),
    intArrayOf(16, 272, 64, 64),
    intArrayOf(96, 272, 64, 64),
    intArrayOf(176, 272, 64, 64),
    intArrayOf(16, 352, 64, 64),
    intArrayOf(96, 352, 64, 64),
    intArrayOf(176, 352, 64, 64)
)

class QaKutuTanim(val yer: Int, val ad: String, val pencere: Int)

// harita sirasi: 0 kilit, 1 ana ekran, 2 durum cubugu, 3 navigasyon, 4 cekmece, 5 kontrol paneli
private val QA_HARITA_KUTULARI: Map<Int, List<QaKutuTanim>> = mapOf(
    1 to listOf(QaKutuTanim(0, "YAZI BOYUTU", 11), QaKutuTanim(1, "DPI", 10)),
    2 to listOf(QaKutuTanim(0, "AĞ HIZI", 14), QaKutuTanim(6, "SİMGE\nGİZLE", 15)),
    4 to listOf(QaKutuTanim(0, "ANİMASYON", 12)),
    5 to listOf(QaKutuTanim(0, "YENİLEME", 13))
)

// zorluk: 0 yesil (kolay), 1 turuncu (orta), 2 kirmizi (zor). Simdilik hepsi turuncu.
private val QA_ZORLUK_RENK = arrayOf(Color(0xFF00A75C), Color(0xFFDE4600), Color(0xFFE02828))

fun qaZorluk(harita: Int, parca: Int): Int = 1

class QaHaritaVeri(val resim: ImageBitmap, val w: Int, val h: Int, val parcalar: List<IntArray>)

private val QH_ONBELLEK = HashMap<String, QaHaritaVeri>()

private fun qhSirala(kume: List<Int>, w: Int): IntArray {
    val kalan = HashSet<Int>(kume)
    var simdi = kume.min()
    kalan.remove(simdi)
    val sonuc = ArrayList<Int>()
    sonuc.add(simdi)
    while (kalan.isNotEmpty()) {
        var en = -1
        var enM = Int.MAX_VALUE
        val sx = simdi % w
        val sy = simdi / w
        for (b in kalan) {
            val dx = b % w - sx
            val dy = b / w - sy
            val m = dx * dx + dy * dy
            if (m < enM || (m == enM && b < en)) {
                enM = m
                en = b
            }
        }
        kalan.remove(en)
        sonuc.add(en)
        simdi = en
    }
    return sonuc.toIntArray()
}

private fun qhYukle(ad: String): QaHaritaVeri {
    try {
        val ayar = BitmapFactory.Options()
        ayar.inScaled = false
        ayar.inMutable = true
        val bmp = QaVarlik.baglam.assets.open("qa/" + ad).use { BitmapFactory.decodeStream(it, null, ayar) }
            ?: throw IllegalStateException("resim okunamadi: " + ad)
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val turuncu = 0xFFDE4600.toInt()
        val tur = BooleanArray(w * h)
        for (i in px.indices) {
            if (px[i] == turuncu) {
                tur[i] = true
                px[i] = 0
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        val parcalar = ArrayList<IntArray>()
        val gor = BooleanArray(w * h)
        for (s in 0 until w * h) {
            if (!tur[s] || gor[s]) continue
            val kume = ArrayList<Int>()
            val yigin = ArrayList<Int>()
            gor[s] = true
            yigin.add(s)
            while (yigin.isNotEmpty()) {
                val a = yigin.removeAt(yigin.size - 1)
                kume.add(a)
                val ax = a % w
                val ay = a / w
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val bx = ax + dx
                        val by = ay + dy
                        if (bx < 0 || by < 0 || bx >= w || by >= h) continue
                        val b = by * w + bx
                        if (tur[b] && !gor[b]) {
                            gor[b] = true
                            yigin.add(b)
                        }
                    }
                }
            }
            parcalar.add(qhSirala(kume, w))
        }
        return QaHaritaVeri(bmp.asImageBitmap(), w, h, parcalar)
    } catch (e: Exception) {
        return QaHaritaVeri(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap(), 1, 1, emptyList())
    }
}

fun qaHaritaVeri(ad: String): QaHaritaVeri {
    val var1 = QH_ONBELLEK[ad]
    if (var1 != null) return var1
    val v = qhYukle(ad)
    QH_ONBELLEK[ad] = v
    return v
}

fun qaHaritaPencere(harita: Int, yer: Int): Int {
    val t = QA_HARITA_KUTULARI[harita]?.firstOrNull { it.yer == yer }
    return t?.pencere ?: 0
}

fun qaHaritaHedef(lx: Int, ly: Int, harita: Int): Int {
    val x = lx - QH_DX
    val y = ly - QH_DY
    if (y >= 432 && y < 464) {
        if (x >= 16 && x < 48) return 100
        if (x >= 208 && x < 240) return 101
        if (x >= 64 && x < 192) return 102
    }
    val tanim = QA_HARITA_KUTULARI[harita] ?: return -1
    for (t in tanim) {
        val g = QH_YERLER[t.yer]
        if (x >= g[0] && x < g[0] + g[2] && y >= g[1] && y < g[1] + g[3]) return 400 + t.yer
    }
    return -1
}

private fun DrawScope.qaHaritaKutu(ad: String, x: Int, y: Int, w: Int, h: Int, o: Int, sekil: Int, ak: Boolean, bas: Boolean, etkin: Boolean) {
    val ters = bas && etkin
    val a = if (etkin) 1f else 0.35f
    qaCerceve(sekil, x, y, w, h, o, ak != ters, a)
    if (ad.isEmpty()) return
    val satirlar = ad.split("\n")
    val toplam = satirlar.size * 18 + (satirlar.size - 1) * 2
    val ilk = (h / o - toplam) / 2
    for (i in satirlar.indices) {
        qaYaziOrta(satirlar[i], x + w / 2, y + (ilk + i * 20) * o, o, qaRenk(ak != ters))
    }
}

private fun DrawScope.qaDugmeMetin(m: String, x: Int, y: Int, w: Int, h: Int, o: Int, sekil: Int, ak: Boolean, bas: Boolean) {
    qaCerceve(sekil, x, y, w, h, o, ak != bas)
    val tw = QaFont.genislik(m) * o
    qaYazi(m, x + (w - tw) / 2, y + ((h / o - 18) / 2) * o, o, qaRenk(ak != bas))
}

private fun DrawScope.qaHaritaCizgi(ad: String, px: Int, py: Int, o: Int, faz: Int, harita: Int) {
    val v = qaHaritaVeri(ad)
    qaParca(v.resim, 0, 0, v.w, v.h, px, py, v.w * o, v.h * o)
    for (pi in v.parcalar.indices) {
        val renk = QA_ZORLUK_RENK[qaZorluk(harita, pi).coerceIn(0, 2)]
        val dz = v.parcalar[pi]
        for (k in dz.indices) {
            val a = dz[k]
            val rx = px + (a % v.w) * o
            val ry = py + (a / v.w) * o
            val yanik = ((k - faz) % 4 + 4) % 4 < 2
            drawRect(
                color = renk,
                topLeft = Offset(rx.toFloat(), ry.toFloat()),
                size = Size(o.toFloat(), o.toFloat()),
                alpha = if (yanik) 1f else 0.3f
            )
        }
    }
}

fun DrawScope.qaHaritaSayfasi(harita: Int, xk: Int, yk: Int, o: Int, ak: Boolean, renk: Color, sekil: Int, basili: Int, faz: Int) {
    val ox = xk + QH_DX * o
    val oy = yk + QH_DY * o
    val oge = QA_OGELER[harita]
    val px = ox + 16 * o
    val py = oy + 16 * o
    qaResim("duvar_kagidi.png", px, py, o, false)
    qaHaritaCizgi(oge.ad + "_harita.png", px, py, o, faz, harita)
    qaResim("telefon_dis_cerceve_seffaf.png", px, py, o, false)

    val tanim = QA_HARITA_KUTULARI[harita] ?: emptyList()
    for (yer in 0 until 12) {
        val g = QH_YERLER[yer]
        val t = tanim.firstOrNull { it.yer == yer }
        val bas = t != null && basili == 400 + yer
        qaHaritaKutu(t?.ad ?: "", ox + g[0] * o, oy + g[1] * o, g[2] * o, g[3] * o, o, sekil, ak, bas, t != null)
    }

    val ay = oy + 432 * o
    val b1 = basili == 100
    qaDaire(ox + 16 * o, ay, o, ak != b1)
    qaResim("secenekler_16x16.png", ox + 24 * o, ay + 8 * o, o, ak != b1)
    qaResim("qanopy_logo_128x32.png", ox + 64 * o, ay, o, ak)
    val b2 = basili == 101
    qaDaire(ox + 208 * o, ay, o, ak != b2)
    qaResim("ayarlar_16x16.png", ox + 216 * o, ay + 8 * o, o, ak != b2)
}

// ===== 15 SN ONAY PENCERESI (DPI / yazi boyutu)
fun qaOnayPencere(yuk: Int, dx: Int): QaPencere {
    val py = (yuk - 134) / 2
    val k = ArrayList<QaKutu>()
    k.add(QaKutu(230, 27 + dx, py + 88, 104, 34))
    k.add(QaKutu(231, 139 + dx, py + 88, 104, 34))
    return QaPencere(15 + dx, py, 240, 134, k)
}

fun DrawScope.qaOnayCiz(yuk: Int, dx: Int, xk: Int, yk: Int, o: Int, ak: Boolean, renk: Color, sekil: Int, basili: Int) {
    drawRect(Color(0xAA000000))
    val w = qaOnayPencere(yuk, dx)
    val px0 = xk + w.x * o
    val py0 = yk + w.y * o
    qaCerceve(sekil, px0, py0, w.w * o, w.h * o, o, ak)
    val cx = xk + (w.x + w.w / 2) * o
    qaYaziOrta("ONAY", cx, py0 + 12 * o, o, renk)
    qaYaziOrta("KALAN: " + BoyutDurum.kalan, cx, py0 + 38 * o, o, renk)
    qaYaziOrta("SÜRE BİTERSE GERİ ALINIR", cx, py0 + 58 * o, o, renk)
    for (k in w.kutular) {
        qaDugmeMetin(if (k.id == 230) "ONAYLA" else "GERİ AL", xk + k.x * o, yk + k.y * o, k.w * o, k.h * o, o, sekil, ak, basili == k.id)
    }
}

// ===== AYAR PENCERELERI (10 DPI, 11 yazi boyutu, 12 animasyon, 13 yenileme, 14 ag hizi, 15 simge gizleme)
class QaAyarOge(val id: Int, val kod: Int, val x: Int, val y: Int, val w: Int, val h: Int, val metin: String, val secili: Boolean, val etkin: Boolean)
class QaAyarEtiket(val x: Int, val y: Int, val metin: String)
class QaAyarYerlesim(val yuk: Int, val baslik: String, val ogeler: List<QaAyarOge>, val etiketler: List<QaAyarEtiket>)

private val QA_SIMGELER = listOf(
    Pair("bluetooth", "BLUETOOTH"), Pair("nfc", "NFC"), Pair("hotspot", "HOTSPOT"),
    Pair("cast", "CAST"), Pair("ethernet", "ETHERNET"), Pair("data_saver", "DATA SAVER"),
    Pair("airplane", "UÇAK"), Pair("roaming", "ROAMING"), Pair("call_strength", "ÇAĞRI GÜCÜ"),
    Pair("vpn", "VPN"), Pair("vowifi", "VOWIFI"), Pair("volte", "VOLTE"), Pair("vonr", "VONR"),
    Pair("volume", "SES"), Pair("mute", "SESSİZ"), Pair("zen", "ZEN"), Pair("headset", "KULAKLIK"),
    Pair("alarm_clock", "ALARM"), Pair("managed_profile", "İŞ PROFİLİ"), Pair("firewall", "FIREWALL"),
    Pair("rotate", "DÖNDÜR")
)

private fun qhListe(s: String?): List<String> = (s ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }

private fun qhEkle(l: ArrayList<QaAyarOge>, kod: Int, x: Int, y: Int, w: Int, metin: String, secili: Boolean, etkin: Boolean) {
    l.add(QaAyarOge(500 + l.size, kod, x, y, w, QH_BH, metin, secili, etkin))
}

private fun qhIzgara(l: ArrayList<QaAyarOge>, kodTaban: Int, y0: Int, sutun: Int, metinler: List<String>, secili: Int, etkin: Boolean): Int {
    val w = (216 - (sutun - 1) * QH_BOSLUK) / sutun
    for (i in metinler.indices) {
        qhEkle(l, kodTaban + i, 12 + (i % sutun) * (w + QH_BOSLUK), y0 + (i / sutun) * (QH_BH + QH_BOSLUK), w, metinler[i], i == secili, etkin)
    }
    val satir = (metinler.size + sutun - 1) / sutun
    return y0 + satir * (QH_BH + QH_BOSLUK)
}

private fun qhMesaj(e: ArrayList<QaAyarEtiket>, ms: String, y: Int) {
    val s = ms.replace("\n", " ").chunked(25)
    for (i in 0 until minOf(2, s.size)) {
        e.add(QaAyarEtiket(12, y + i * 18, s[i]))
    }
}

object QaAyarDurum {
    var simdi by mutableIntStateOf(-1)
    var simdi2 by mutableIntStateOf(-1)
    var ozel by mutableIntStateOf(0)
    var ozelTur by mutableIntStateOf(0)
    var mesaj by mutableStateOf("")
    var calisiyor by mutableStateOf(false)
    var surum by mutableIntStateOf(0)
    var agMod by mutableStateOf<String?>(null)
    var agBirim by mutableStateOf<String?>(null)
    var agGizle by mutableStateOf<String?>(null)
    var simgeler by mutableStateOf<List<String>>(emptyList())

    private var sonTur = 0
    private val kilit = Any()
    private val DPI_HAZIR = listOf(320, 360, 400, 440)
    private val FONT_HAZIR = listOf(85, 100, 115, 130)
    private val ANIM_HAZIR = listOf(0, 5, 10, 15)
    private val REF_HAZIR = listOf(-1, 60, 90, 120)

    fun kapat() {
        sonTur = 0
    }

    // min, max, adim, varsayilan
    private fun aralik(tur: Int): IntArray = when (tur) {
        10 -> intArrayOf(280, 520, 10, 400)
        11 -> intArrayOf(70, 150, 5, 100)
        12 -> intArrayOf(0, 30, 1, 10)
        else -> intArrayOf(60, 120, 5, 60)
    }

    private fun ilkOzel(tur: Int): Int {
        val a = aralik(tur)
        return if (simdi >= a[0] && simdi <= a[1]) simdi else a[3]
    }

    fun yuzde(v: Int): String {
        val r = v % 100
        val t = v / 100
        return if (r % 10 == 0) {
            t.toString() + "." + (r / 10).toString()
        } else {
            t.toString() + "." + (if (r < 10) "0" else "") + r.toString()
        }
    }

    fun onda(v: Int): String = if (v == 0) "0" else (v / 10).toString() + "." + (v % 10).toString()

    fun yukle(tur: Int) {
        if (tur < 10) return
        if (sonTur != tur) {
            sonTur = tur
            simdi = -1
            simdi2 = -1
            ozelTur = 0
            mesaj = ""
        }
        Thread {
            try {
                yukleIc(tur)
            } catch (e: Exception) {
                mesaj = e.message ?: "HATA"
            }
            if (tur in 10..13 && ozelTur != tur) {
                ozelTur = tur
                ozel = ilkOzel(tur)
            }
        }.start()
    }

    private fun yukleIc(tur: Int) {
        if (tur == 10) {
            val d = BoyutMotoru.dpiOku()
            val dh = d.hata
            if (dh != null) {
                mesaj = dh
                return
            }
            simdi = d.gecerli ?: -1
        } else if (tur == 11) {
            val f = AyarMotoru.hizliOku("system", "font_scale")
            val fh = f.hata
            if (fh != null) {
                mesaj = fh
                return
            }
            val v = (f.deger ?: "1.0").toFloatOrNull()
            simdi = if (v == null) -1 else (v * 100f).roundToInt()
        } else if (tur == 12) {
            val r = EkranMotoru.animOku()
            val rh = r.second
            if (rh != null) {
                mesaj = rh
                return
            }
            val l = EkranMotoru.animAnahtar.map { (r.first[it] ?: "1.0").toFloatOrNull() ?: -1f }
            val ilk = l[0]
            simdi = if (ilk < 0f || l.any { Math.abs(it - ilk) > 0.001f }) -1 else (ilk * 10f).roundToInt()
        } else if (tur == 13) {
            val o = YenilemeMotoru.oku()
            val oh = o.hata
            if (oh != null) {
                mesaj = oh
                return
            }
            simdi = o.enAz?.toFloatOrNull()?.roundToInt() ?: -1
            simdi2 = o.tepe?.toFloatOrNull()?.roundToInt() ?: -1
        } else if (tur == 14) {
            val m = AyarMotoru.hizliOku("secure", "network_traffic_mode")
            val b = AyarMotoru.hizliOku("secure", "network_traffic_units")
            val g = AyarMotoru.hizliOku("secure", "network_traffic_autohide")
            val eh = m.hata ?: b.hata ?: g.hata
            if (eh != null) {
                mesaj = eh
                return
            }
            agMod = m.deger
            agBirim = b.deger
            agGizle = g.deger
        } else if (tur == 15) {
            val o = AyarMotoru.tekOku("secure", "icon_blacklist")
            val oh = o.hata
            if (oh != null) {
                mesaj = oh
                return
            }
            simgeler = qhListe(o.deger)
        }
    }

    private fun simdiMetin(tur: Int): String {
        if (simdi < 0) return "---"
        if (tur == 13) {
            if (simdi == 60 && simdi2 == 120) return "OTOMATİK"
            if (simdi == simdi2) return simdi.toString() + " HZ"
            return simdi.toString() + "-" + simdi2.toString() + " HZ"
        }
        if (tur == 10) return simdi.toString()
        if (tur == 11) return yuzde(simdi)
        return onda(simdi)
    }

    private fun ozelMetin(tur: Int): String {
        if (ozelTur != tur) return "..."
        if (tur == 10) return ozel.toString()
        if (tur == 11) return yuzde(ozel)
        if (tur == 12) return onda(ozel)
        return ozel.toString() + " HZ"
    }

    private fun sayisalYerlesim(tur: Int): QaAyarYerlesim {
        val l = ArrayList<QaAyarOge>()
        val e = ArrayList<QaAyarEtiket>()
        val baslik: String
        val hazir: List<String>
        val sutun: Int
        var secili = -1
        if (tur == 10) {
            baslik = "DPI"
            hazir = DPI_HAZIR.map { it.toString() }
            sutun = 4
            secili = DPI_HAZIR.indexOf(simdi)
        } else if (tur == 11) {
            baslik = "YAZI BOYUTU"
            hazir = FONT_HAZIR.map { yuzde(it) }
            sutun = 4
            secili = FONT_HAZIR.indexOf(simdi)
        } else if (tur == 12) {
            baslik = "ANİMASYON HIZI"
            hazir = listOf("KAPALI", "HIZLI", "NORMAL", "YAVAŞ")
            sutun = 2
            secili = ANIM_HAZIR.indexOf(simdi)
        } else {
            baslik = "YENİLEME HIZI"
            hazir = listOf("OTOMATİK", "60", "90", "120")
            sutun = 2
            secili = if (simdi == 60 && simdi2 == 120) 0 else if (simdi == simdi2) REF_HAZIR.indexOf(simdi) else -1
        }
        e.add(QaAyarEtiket(12, 34, "ŞİMDİ: " + simdiMetin(tur)))
        var y = qhIzgara(l, 0, 56, sutun, hazir, secili, true)
        e.add(QaAyarEtiket(12, y + 6, "ÖZEL"))
        qhEkle(l, 100, 56, y, 40, "-", false, true)
        qhEkle(l, 99, 100, y, 64, ozelMetin(tur), false, true)
        qhEkle(l, 101, 168, y, 40, "+", false, true)
        y += QH_BH + QH_BOSLUK
        qhEkle(l, 102, 12, y, 216, "UYGULA", false, true)
        y += QH_BH + QH_BOSLUK
        val ms = if (tur == 10 || tur == 11) (BoyutDurum.mesaj ?: "") else mesaj
        qhMesaj(e, ms, y + 2)
        return QaAyarYerlesim(y + 46, baslik, l, e)
    }

    private fun agYerlesim(): QaAyarYerlesim {
        val l = ArrayList<QaAyarOge>()
        val e = ArrayList<QaAyarEtiket>()
        e.add(QaAyarEtiket(12, 34, "MOD"))
        var y = qhIzgara(l, 0, 54, 2, listOf("KAPALI", "YUKARI", "AŞAĞI", "İKİSİ"), agMod?.toIntOrNull() ?: -1, true)
        val bagli = agMod != "0"
        e.add(QaAyarEtiket(12, y + 2, "BİRİM"))
        y = qhIzgara(l, 10, y + 22, 3, listOf("KBİT", "MBİT", "KBAYT", "MBAYT", "OTO"), agBirim?.toIntOrNull() ?: -1, bagli)
        e.add(QaAyarEtiket(12, y + 2, "OTOMATİK GİZLE"))
        val g = if (agGizle == "1") 0 else if (agGizle == "0") 1 else -1
        y = qhIzgara(l, 20, y + 22, 2, listOf("AÇIK", "KAPALI"), g, bagli)
        qhMesaj(e, mesaj, y + 2)
        return QaAyarYerlesim(y + 46, "AĞ HIZI", l, e)
    }

    private fun simgeYerlesim(): QaAyarYerlesim {
        val l = ArrayList<QaAyarOge>()
        val e = ArrayList<QaAyarEtiket>()
        e.add(QaAyarEtiket(12, 34, if (mesaj.isEmpty()) "DOLU OLAN GİZLİ" else mesaj.take(25)))
        val y0 = 54
        for (i in QA_SIMGELER.indices) {
            val gizli = QA_SIMGELER[i].first in simgeler
            qhEkle(l, i, 12 + (i % 2) * 110, y0 + (i / 2) * (QH_BH + QH_BOSLUK), 106, QA_SIMGELER[i].second, gizli, true)
        }
        val satir = (QA_SIMGELER.size + 1) / 2
        return QaAyarYerlesim(y0 + satir * (QH_BH + QH_BOSLUK) + 8, "SİMGE GİZLEME", l, e)
    }

    fun yerlesim(tur: Int): QaAyarYerlesim {
        if (tur in 10..13) return sayisalYerlesim(tur)
        if (tur == 14) return agYerlesim()
        return simgeYerlesim()
    }

    fun dokun(tur: Int, id: Int) {
        val y = yerlesim(tur)
        val i = id - 500
        if (i < 0 || i >= y.ogeler.size) return
        val g = y.ogeler[i]
        if (!g.etkin || g.kod == 99) return
        val c = QaVarlik.baglam.applicationContext
        if (tur in 10..13) {
            sayisalDokun(tur, g.kod, c)
        } else if (tur == 14) {
            agDokun(g.kod, c)
        } else if (tur == 15) {
            simgeDokun(g.kod, c)
        }
    }

    private fun hazirDeger(tur: Int, kod: Int): Int {
        if (tur == 10) return DPI_HAZIR[kod]
        if (tur == 11) return FONT_HAZIR[kod]
        if (tur == 12) return ANIM_HAZIR[kod]
        return REF_HAZIR[kod]
    }

    private fun sayisalDokun(tur: Int, kod: Int, c: Context) {
        if (ozelTur != tur) return
        val a = aralik(tur)
        if (kod == 100) {
            ozel = maxOf(a[0], ozel - a[2])
            return
        }
        if (kod == 101) {
            ozel = minOf(a[1], ozel + a[2])
            return
        }
        if (calisiyor) return
        val deger: Int
        if (kod == 102) {
            deger = ozel
        } else if (kod in 0..3) {
            deger = hazirDeger(tur, kod)
        } else {
            return
        }
        uygula(tur, deger, kod, c)
    }

    private fun uygula(tur: Int, deger: Int, kod: Int, c: Context) {
        if (tur == 10) {
            BoyutMotoru.uygula(c, if (deger == 400) "reset" else deger.toString(), null)
            return
        }
        if (tur == 11) {
            BoyutMotoru.uygula(c, null, yuzde(deger))
            return
        }
        calisiyor = true
        mesaj = ""
        Thread {
            var h: String? = null
            try {
                if (tur == 12) {
                    h = EkranMotoru.animUygula(c, onda(deger))
                } else {
                    var a = deger.toFloat()
                    var b = deger.toFloat()
                    if (kod == 0) {
                        a = 60f
                        b = 120f
                    }
                    h = YenilemeMotoru.uygula(c, a, b)
                }
            } catch (e: Exception) {
                h = e.message ?: "HATA"
            }
            mesaj = h ?: "UYGULANDI"
            surum = surum + 1
            calisiyor = false
        }.start()
    }

    private fun yaz(c: Context, anahtar: String, deger: String) {
        calisiyor = true
        mesaj = ""
        Thread {
            try {
                val h = AyarMotoru.yaz(c, "secure", anahtar, deger)
                mesaj = h ?: ""
            } catch (e: Exception) {
                mesaj = e.message ?: "HATA"
            }
            surum = surum + 1
            calisiyor = false
        }.start()
    }

    private fun agDokun(kod: Int, c: Context) {
        if (calisiyor) return
        val anahtar: String
        val deger: String
        if (kod in 0..3) {
            anahtar = "network_traffic_mode"
            deger = kod.toString()
            agMod = deger
        } else if (kod in 10..14) {
            anahtar = "network_traffic_units"
            deger = (kod - 10).toString()
            agBirim = deger
        } else if (kod == 20) {
            anahtar = "network_traffic_autohide"
            deger = "1"
            agGizle = deger
        } else if (kod == 21) {
            anahtar = "network_traffic_autohide"
            deger = "0"
            agGizle = deger
        } else {
            return
        }
        yaz(c, anahtar, deger)
    }

    private fun simgeDokun(kod: Int, c: Context) {
        if (kod < 0 || kod >= QA_SIMGELER.size) return
        val ad = QA_SIMGELER[kod].first
        val gizle = ad !in simgeler
        simgeler = if (gizle) simgeler + ad else simgeler.filter { it != ad }
        Thread {
            try {
                synchronized(kilit) {
                    val o = AyarMotoru.tekOku("secure", "icon_blacklist")
                    val oh = o.hata
                    if (oh != null) {
                        mesaj = oh
                    } else {
                        val l = qhListe(o.deger)
                        val y = if (gizle) (if (ad in l) l else l + ad) else l.filter { it != ad }
                        if (y != l) {
                            val h = AyarMotoru.yaz(c, "secure", "icon_blacklist", y.joinToString(","))
                            mesaj = h ?: ""
                        }
                    }
                    Unit
                }
            } catch (e: Exception) {
                mesaj = e.message ?: "HATA"
            }
            surum = surum + 1
        }.start()
    }
}

fun qaAyarPencere(tur: Int, dx: Int, yuk: Int): QaPencere {
    val y = QaAyarDurum.yerlesim(tur)
    val py = (yuk - y.yuk) / 2
    val k = ArrayList<QaKutu>()
    for (g in y.ogeler) {
        if (g.etkin && g.kod != 99) k.add(QaKutu(g.id, 15 + dx + g.x, py + g.y, g.w, g.h))
    }
    return QaPencere(15 + dx, py, 240, y.yuk, k)
}

fun DrawScope.qaAyarCiz(tur: Int, wx: Int, wy: Int, xk: Int, yk: Int, o: Int, ak: Boolean, renk: Color, sekil: Int, basili: Int) {
    val y = QaAyarDurum.yerlesim(tur)
    qaYaziOrta(y.baslik, xk + (wx + 120) * o, yk + (wy + 12) * o, o, renk)
    for (e in y.etiketler) {
        qaYazi(e.metin, xk + (wx + e.x) * o, yk + (wy + e.y) * o, o, renk)
    }
    for (g in y.ogeler) {
        val bx = xk + (wx + g.x) * o
        val by = yk + (wy + g.y) * o
        val bas = basili == g.id && g.kod != 99
        val ters = g.secili || bas
        val a = if (g.etkin) 1f else 0.4f
        qaCerceve(sekil, bx, by, g.w * o, g.h * o, o, ak != ters, a)
        val tw = QaFont.genislik(g.metin) * o
        qaYazi(g.metin, bx + (g.w * o - tw) / 2, by + ((g.h - 18) / 2) * o, o, qaRenk(ak != ters), a)
    }
}
