package io.github.bntheq.qanopy

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

private val QA_KREM = Color(0xFFFFE8AF)

object QaVarlik {
    lateinit var baglam: Context
    private val onbellek = HashMap<String, ImageBitmap>()

    // ACIK TEMA RENK TABLOSU (GECICI, sonra kullanici sececek)
    private val ACIK: Map<Int, Int> = mapOf(
        0xFF000000.toInt() to 0xFFFFE8AF.toInt(),
        0xFFFFE8AF.toInt() to 0xFF000000.toInt(),
        0xFFFFFFFF.toInt() to 0xFF000000.toInt(),
        0xFF323232.toInt() to 0xFFCCBA8C.toInt(),
        0xFF666666.toInt() to 0xFF998B69.toInt(),
        0xFF888888.toInt() to 0xFF786D52.toInt(),
        0xFFAEA181.toInt() to 0xFF5A5340.toInt()
    )

    private fun degisir(ad: String): Boolean =
        !(ad.startsWith("duvar") || ad.contains("ogeleri") || ad.contains("ogesi") || ad.startsWith("telefon_dis"))

    fun al(ad: String, acik: Boolean = false): ImageBitmap {
        val anahtar = ad + (if (acik) "|a" else "|k")
        val var1 = onbellek[anahtar]
        if (var1 != null) return var1
        val ayar = BitmapFactory.Options()
        ayar.inScaled = false
        ayar.inMutable = true
        val bmp = baglam.assets.open("qa/" + ad).use { BitmapFactory.decodeStream(it, null, ayar) }
            ?: throw IllegalStateException("resim okunamadi: " + ad)
        if (acik && degisir(ad)) {
            val w = bmp.width
            val h = bmp.height
            val px = IntArray(w * h)
            bmp.getPixels(px, 0, w, 0, 0, w, h)
            for (i in px.indices) {
                val y = ACIK[px[i]]
                if (y != null) px[i] = y
            }
            bmp.setPixels(px, 0, w, 0, 0, w, h)
        }
        val ib = bmp.asImageBitmap()
        onbellek[anahtar] = ib
        return ib
    }
}

object QaFont {
    const val SIRA = "ABCÇDEFGĞHIİJKLMNOÖPQRSŞTUÜVWXYZabcçdefgğhıijklmnoöpqrsştuüvwxyz1234567890:.,%/-()+_"

    private val veri: Pair<IntArray, IntArray> by lazy {
        val im = QaVarlik.al("qanopy_font.png")
        val w = im.width
        val h = im.height
        val px = IntArray(w * h)
        im.readPixels(px, 0, 0, w, h)
        val b = IntArray(SIRA.length)
        val g = IntArray(SIRA.length)
        for (i in SIRA.indices) {
            val x0 = (i % 12) * 9
            val y0 = (i / 12) * 20
            var mn = 99
            var mx = -1
            for (y in 0 until 18) {
                for (x in 0 until 7) {
                    val yy = y0 + y
                    val xx = x0 + x
                    if (yy < h && xx < w && (px[yy * w + xx] ushr 24) > 0) {
                        if (x < mn) mn = x
                        if (x > mx) mx = x
                    }
                }
            }
            if (mx >= 0) {
                b[i] = mn
                g[i] = mx - mn + 1
            }
        }
        Pair(b, g)
    }

    fun bas(i: Int): Int = veri.first[i]
    fun gen(i: Int): Int = veri.second[i]

    fun genislik(m: String): Int {
        var x = 0
        for (ch in m) {
            val i = SIRA.indexOf(ch)
            x += if (i < 0 || gen(i) == 0) 4 else gen(i) + 1
        }
        return maxOf(0, x - 1)
    }
}

object QaAyar {
    private const val DOSYA = "qanopy_qa"
    var tema by mutableIntStateOf(0)
        private set
    var emin by mutableStateOf(true)
        private set
    var titresim by mutableStateOf(true)
        private set
    var kose by mutableIntStateOf(7)
        private set
    private var yuklendi = false
    private fun pref() = QaVarlik.baglam.getSharedPreferences(DOSYA, Context.MODE_PRIVATE)
    fun yukle(c: Context) {
        if (yuklendi) return
        val p = c.getSharedPreferences(DOSYA, Context.MODE_PRIVATE)
        tema = p.getInt("tema", 0).coerceIn(0, 2)
        emin = p.getBoolean("emin", true)
        titresim = p.getBoolean("titresim", true)
        kose = p.getInt("kose", 7).coerceIn(0, 8)
        yuklendi = true
    }
    fun temaYaz(v: Int) {
        tema = v
        pref().edit().putInt("tema", v).apply()
    }
    fun eminYaz(v: Boolean) {
        emin = v
        pref().edit().putBoolean("emin", v).apply()
    }
    fun titresimYaz(v: Boolean) {
        titresim = v
        pref().edit().putBoolean("titresim", v).apply()
    }
    fun koseYaz(v: Int) {
        kose = v
        pref().edit().putInt("kose", v).apply()
    }
}

class QaOge(val ad: String, val baslik: String, val dugmeIkon: String, val onizleme: String)
class QaIslem(val ad: String, val komut: String, val riskli: Boolean)

// YENI OZELLIK = BU LISTEYE BIR SATIR
val QA_OGELER = listOf(
    QaOge("kilit_ekrani", "KİLİT EKRANI", "kilit_ekrani.png", "kilit_ekrani_ogeleri.png"),
    QaOge("ana_ekran", "ANA EKRAN", "ana_sayfa.png", "ana_ekran_ogeleri.png"),
    QaOge("durum_cubugu", "DURUM ÇUBUĞU", "durum_cubugu.png", "durum_cubugu_ogeleri.png"),
    QaOge("navigasyon_cubugu", "NAVİGASYON ÇUBUĞU", "navigasyon_cubugu.png", "navigasyon_cubugu_ogesi.png"),
    QaOge("uygulama_cekmecesi", "UYGULAMA ÇEKMECESİ", "uygulama_cekmecesi.png", "uygulama_cekmecesi_ogeleri.png"),
    QaOge("kontrol_paneli", "KONTROL PANELİ", "kontrol_paneli.png", "kontrol_paneli_ogeleri.png")
)

// SECENEKLER LISTESI (KOMUTLAR DENENMEDI)
val QA_ISLEMLER = listOf(
    QaIslem("MAGISK AÇ", "monkey -p com.topjohnwu.magisk -c android.intent.category.LAUNCHER 1", false),
    QaIslem("SYSTEMUI YENİDEN BAŞLAT", "killall com.android.systemui", true),
    QaIslem("TELEFONU YENİDEN BAŞLAT", "reboot", true)
)

val QA_KOSELER = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
private val QA_TEMALAR = listOf("OTOMATİK", "AÇIK", "KOYU")

const val QA_DY = 22
const val QA_BLOK = 484

fun qaKokKontrol(sonuc: (Boolean) -> Unit) {
    thread {
        val ok = try {
            val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroy()
                false
            } else p.exitValue() == 0
        } catch (e: Exception) {
            false
        }
        Handler(Looper.getMainLooper()).post { sonuc(ok) }
    }
}

fun qaKokCalistir(komut: String) {
    thread {
        try {
            ProcessBuilder("su", "-c", komut).redirectErrorStream(true).start().waitFor()
        } catch (e: Exception) {
        }
    }
}

private fun qaRenk(acik: Boolean): Color = if (acik) Color.Black else QA_KREM

private val koseOnbellek = HashMap<Int, Int>()

private fun qaKose(sekil: Int): Int {
    val var1 = koseOnbellek[sekil]
    if (var1 != null) return var1
    val im = QaVarlik.al("qanopy_shapes_32x32.png")
    val hx = (sekil % 5) * 34
    val hy = (sekil / 5) * 34
    val b = 32
    val px = IntArray(b * b)
    im.readPixels(px, hx, hy, b, b)
    var sonuc = b / 2
    for (c in 1 until b / 2) {
        var ok = true
        for (y in c + 1..b - 1 - c) for (x in 0 until b) if (px[y * b + x] != px[c * b + x]) ok = false
        for (x in c + 1..b - 1 - c) for (y in 0 until b) if (px[y * b + x] != px[y * b + c]) ok = false
        if (ok) {
            sonuc = c
            break
        }
    }
    koseOnbellek[sekil] = sonuc
    return sonuc
}

private class QaKutu(val id: Int, val x: Int, val y: Int, val w: Int, val h: Int)
private class QaPencere(val x: Int, val y: Int, val w: Int, val h: Int, val kutular: List<QaKutu>)

private fun qaPencere(tur: Int): QaPencere {
    val k = ArrayList<QaKutu>()
    if (tur == 1) {
        val ph = 170
        val py = (QA_BLOK - ph) / 2
        for (i in QA_ISLEMLER.indices) k.add(QaKutu(200 + i, 27, py + 40 + i * 42, 216, 34))
        return QaPencere(15, py, 240, ph, k)
    }
    if (tur == 2) {
        val ph = 212
        val py = (QA_BLOK - ph) / 2
        k.add(QaKutu(210, 143, py + 40, 100, 34))
        k.add(QaKutu(211, 203, py + 82, 40, 34))
        k.add(QaKutu(212, 203, py + 124, 40, 34))
        k.add(QaKutu(213, 199, py + 166, 44, 34))
        return QaPencere(15, py, 240, ph, k)
    }
    val ph = 134
    val py = (QA_BLOK - ph) / 2
    k.add(QaKutu(220, 27, py + 88, 104, 34))
    k.add(QaKutu(221, 139, py + 88, 104, 34))
    return QaPencere(15, py, 240, ph, k)
}

private fun qaGostergeSayisi(s: Int): Int = min(s, 5) + (if (s > 5) 1 else 0)

private fun qaGostergeX(s: Int, i: Int): Int {
    val c = qaGostergeSayisi(s)
    val t = c * 8 + (c - 1) * 9
    return (270 - t) / 2 + i * 17
}

private fun qaHedef(lx: Int, ly: Int, pencere: Int, harita: Int, sayfa: Int, sayfaSayisi: Int, n: Int, kokYok: Boolean): Int {
    if (harita >= 0) return -1
    if (pencere != 0) {
        for (k in qaPencere(pencere).kutular) {
            if (lx >= k.x && lx < k.x + k.w && ly >= k.y && ly < k.y + k.h) return k.id
        }
        return -1
    }
    for (i in 0 until 6) {
        val ix = sayfa * 6 + i
        if (ix >= n) continue
        val bx = 17 + (i % 3) * 85
        val by = QA_DY + 244 + (i / 3) * 84
        if (lx >= bx && lx < bx + 66 && ly >= by && ly < by + 66) return ix
    }
    for (p in 0 until min(sayfaSayisi, 5)) {
        val gx = qaGostergeX(sayfaSayisi, p)
        if (lx >= gx - 4 && lx < gx + 12 && ly >= QA_DY + 402 && ly < QA_DY + 422) return 300 + p
    }
    val ay = QA_DY + 430
    if (ly >= ay && ly < ay + 32) {
        if (lx >= 17 && lx < 49) return 100
        if (lx >= 221 && lx < 253) return 101
        if (lx >= 71 && lx < 199) return 102
    }
    if (kokYok && lx >= 0 && lx < 90 && ly >= 0 && ly < 20) return 103
    return -1
}

private fun DrawScope.qaParca(im: ImageBitmap, sx: Int, sy: Int, sw: Int, sh: Int, dx: Int, dy: Int, dw: Int, dh: Int, a: Float = 1f) {
    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) return
    drawImage(im, IntOffset(sx, sy), IntSize(sw, sh), IntOffset(dx, dy), IntSize(dw, dh), alpha = a, filterQuality = FilterQuality.None)
}

private fun DrawScope.qaResim(ad: String, x: Int, y: Int, o: Int, acik: Boolean, a: Float = 1f) {
    val im = QaVarlik.al(ad, acik)
    qaParca(im, 0, 0, im.width, im.height, x, y, im.width * o, im.height * o, a)
}

private fun DrawScope.qaDaire(x: Int, y: Int, o: Int, acik: Boolean) {
    val im = QaVarlik.al("qanopy_shapes_32x32.png", acik)
    qaParca(im, 0, 0, 32, 32, x, y, 32 * o, 32 * o)
}

private fun DrawScope.qaDokuz(im: ImageBitmap, hx: Int, hy: Int, hs: Int, k: Int, x: Int, y: Int, w: Int, h: Int, o: Int, a: Float) {
    val kd = k * o
    val m = hs - 2 * k
    qaParca(im, hx, hy, k, k, x, y, kd, kd, a)
    qaParca(im, hx + hs - k, hy, k, k, x + w - kd, y, kd, kd, a)
    qaParca(im, hx, hy + hs - k, k, k, x, y + h - kd, kd, kd, a)
    qaParca(im, hx + hs - k, hy + hs - k, k, k, x + w - kd, y + h - kd, kd, kd, a)
    qaParca(im, hx + k, hy, m, k, x + kd, y, w - 2 * kd, kd, a)
    qaParca(im, hx + k, hy + hs - k, m, k, x + kd, y + h - kd, w - 2 * kd, kd, a)
    qaParca(im, hx, hy + k, k, m, x, y + kd, kd, h - 2 * kd, a)
    qaParca(im, hx + hs - k, hy + k, k, m, x + w - kd, y + kd, kd, h - 2 * kd, a)
    qaParca(im, hx + k, hy + k, m, m, x + kd, y + kd, w - 2 * kd, h - 2 * kd, a)
}

private fun DrawScope.qaCerceve(sekil: Int, x: Int, y: Int, w: Int, h: Int, o: Int, acik: Boolean, a: Float = 1f) {
    val im = QaVarlik.al("qanopy_shapes_32x32.png", acik)
    val hx = (sekil % 5) * 34
    val hy = (sekil / 5) * 34
    val k = minOf(qaKose(sekil), w / (2 * o), h / (2 * o))
    qaDokuz(im, hx, hy, 32, k, x, y, w, h, o, a)
}

private fun DrawScope.qaYazi(m: String, x: Int, y: Int, o: Int, renk: Color, a: Float = 1f) {
    val font = QaVarlik.al("qanopy_font.png")
    var cx = x
    for (ch in m) {
        val i = QaFont.SIRA.indexOf(ch)
        if (i < 0 || QaFont.gen(i) == 0) {
            cx += 4 * o
            continue
        }
        val g = QaFont.gen(i)
        drawImage(
            font,
            IntOffset((i % 12) * 9 + QaFont.bas(i), (i / 12) * 20),
            IntSize(g, 18),
            IntOffset(cx, y),
            IntSize(g * o, 18 * o),
            alpha = a,
            colorFilter = ColorFilter.tint(renk),
            filterQuality = FilterQuality.None
        )
        cx += (g + 1) * o
    }
}

private fun DrawScope.qaYaziOrta(m: String, cx: Int, y: Int, o: Int, renk: Color, a: Float = 1f) {
    qaYazi(m, cx - QaFont.genislik(m) * o / 2, y, o, renk, a)
}

private fun DrawScope.qaYaziDugme(m: String, x: Int, y: Int, w: Int, h: Int, o: Int, sekil: Int, ak: Boolean, bas: Boolean, etkin: Boolean) {
    val ters = bas && etkin
    val a = if (etkin) 1f else 0.4f
    qaCerceve(sekil, x, y, w, h, o, ak != ters, a)
    val tw = QaFont.genislik(m) * o
    qaYazi(m, x + (w - tw) / 2, y + 8 * o, o, qaRenk(ak != ters), a)
}

private fun DrawScope.qaOnizleme(oge: QaOge, x: Int, y: Int, o: Int) {
    qaResim("duvar_kagidi.png", x, y, o, false)
    qaResim(oge.onizleme, x, y, o, false)
    qaResim("telefon_dis_cerceve_seffaf.png", x, y, o, false)
}

@Composable
fun QaAnaEkran(onEski: () -> Unit) {
    val view = LocalView.current
    val koyu = when (QaAyar.tema) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val zemin = if (koyu) Color.Black else QA_KREM

    var onizleme by remember { mutableIntStateOf(0) }
    var uc by remember { mutableIntStateOf(0) }
    var ucSayac by remember { mutableIntStateOf(0) }
    var harita by remember { mutableIntStateOf(-1) }
    var pencere by remember { mutableIntStateOf(0) }
    var bekleyen by remember { mutableIntStateOf(-1) }
    var basili by remember { mutableIntStateOf(-1) }
    var kok by remember { mutableStateOf<Boolean?>(null) }

    val n = QA_OGELER.size
    val sayfaSayisi = (n + 5) / 6

    fun titre() {
        if (QaAyar.titresim) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
    fun ileri() {
        if (onizleme < n - 1) {
            onizleme++
            uc = 0
        } else if (uc == 1) {
            onizleme = 0
            uc = 0
            ucSayac++
        } else {
            uc = 1
            ucSayac++
        }
    }
    fun geri() {
        if (onizleme > 0) {
            onizleme--
            uc = 0
        } else if (uc == -1) {
            onizleme = n - 1
            uc = 0
            ucSayac++
        } else {
            uc = -1
            ucSayac++
        }
    }
    fun sayfaGit(p: Int) {
        if (p in 0 until sayfaSayisi) {
            onizleme = p * 6
            uc = 0
        }
    }

    LaunchedEffect(Unit) { qaKokKontrol { kok = it } }
    LaunchedEffect(ucSayac) {
        if (uc != 0) {
            delay(1500)
            uc = 0
        }
    }
    BackHandler(enabled = pencere != 0 || harita >= 0) {
        if (pencere != 0) {
            pencere = 0
            bekleyen = -1
        } else {
            harita = -1
        }
    }

    val konum by animateFloatAsState(targetValue = onizleme.toFloat(), animationSpec = tween(200), label = "konum")

    BoxWithConstraints(Modifier.fillMaxSize().background(zemin)) {
        val gw = constraints.maxWidth
        val gh = constraints.maxHeight
        val o = maxOf(1, gw / 270)
        val xk = (gw - 270 * o) / 2
        val yk = maxOf(0, (gh - QA_BLOK * o) / (2 * o)) * o

        fun hedefBul(p: Offset): Int {
            val lx = floor((p.x - xk) / o).toInt()
            val ly = floor((p.y - yk) / o).toInt()
            return qaHedef(lx, ly, pencere, harita, onizleme / 6, sayfaSayisi, n, kok == false)
        }

        fun dokun(p: Offset) {
            val lx = floor((p.x - xk) / o).toInt()
            val ly = floor((p.y - yk) / o).toInt()
            val h = hedefBul(p)
            if (pencere != 0) {
                if (h < 0) {
                    val w = qaPencere(pencere)
                    if (lx < w.x || lx >= w.x + w.w || ly < w.y || ly >= w.y + w.h) {
                        pencere = 0
                        bekleyen = -1
                    }
                } else if (h in 200..202) {
                    if (kok == true) {
                        val islem = QA_ISLEMLER[h - 200]
                        if (islem.riskli && QaAyar.emin) {
                            bekleyen = h - 200
                            pencere = 3
                        } else {
                            pencere = 0
                            qaKokCalistir(islem.komut)
                        }
                    }
                } else if (h == 210) {
                    QaAyar.temaYaz((QaAyar.tema + 1) % 3)
                } else if (h == 211) {
                    QaAyar.eminYaz(!QaAyar.emin)
                } else if (h == 212) {
                    QaAyar.titresimYaz(!QaAyar.titresim)
                } else if (h == 213) {
                    QaAyar.koseYaz((QaAyar.kose + 1) % QA_KOSELER.size)
                } else if (h == 220) {
                    if (bekleyen >= 0) qaKokCalistir(QA_ISLEMLER[bekleyen].komut)
                    bekleyen = -1
                    pencere = 0
                } else if (h == 221) {
                    bekleyen = -1
                    pencere = 0
                }
                return
            }
            if (harita >= 0) return
            if (h in 0 until 100) {
                harita = h
            } else if (h == 100) {
                pencere = 1
            } else if (h == 101) {
                pencere = 2
            } else if (h == 103) {
                kok = null
                qaKokKontrol { kok = it }
            } else if (h in 300..399) {
                sayfaGit(h - 300)
            }
        }

        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(o, xk, yk) {
                    detectTapGestures(
                        onPress = { p ->
                            val h = hedefBul(p)
                            if (h >= 0) {
                                basili = h
                                titre()
                            }
                            tryAwaitRelease()
                            basili = -1
                        },
                        onLongPress = { p -> if (hedefBul(p) == 102) onEski() },
                        onTap = { p -> dokun(p) }
                    )
                }
                .pointerInput(o, xk, yk) {
                    var toplam = 0f
                    var bolge = 0
                    detectHorizontalDragGestures(
                        onDragStart = { p ->
                            toplam = 0f
                            val ly = (p.y - yk) / o
                            bolge = if (pencere != 0 || harita >= 0) 0
                            else if (ly < QA_DY + 230) 1
                            else if (ly < QA_DY + 410) 2
                            else 0
                        },
                        onDragEnd = {
                            if (bolge == 1) {
                                if (toplam < -16f * o) ileri() else if (toplam > 16f * o) geri()
                            } else if (bolge == 2) {
                                if (toplam < -16f * o) sayfaGit(onizleme / 6 + 1)
                                else if (toplam > 16f * o) sayfaGit(onizleme / 6 - 1)
                            }
                        },
                        onHorizontalDrag = { _, dx -> toplam += dx }
                    )
                }
        ) {
            val ak = !koyu
            val renk = qaRenk(ak)
            val sekil = QA_KOSELER[QaAyar.kose.coerceIn(0, QA_KOSELER.size - 1)]

            if (harita >= 0) {
                val d = QA_OGELER[harita]
                qaResim(d.dugmeIkon, xk + 119 * o, yk + (QA_DY + 150) * o, o, ak)
                qaYaziOrta(d.baslik, xk + 135 * o, yk + (QA_DY + 200) * o, o, renk)
                qaYaziOrta("yakında", xk + 135 * o, yk + (QA_DY + 230) * o, o, renk)
                return@Canvas
            }

            // 1) uyari satiri
            if (kok == false) qaYazi("root yok", xk + 17 * o, yk + 2 * o, o, renk)
            if (uc != 0) qaYaziOrta("bitti", xk + 135 * o, yk + 2 * o, o, renk)

            // 2) onizlemeler
            clipRect(left = xk.toFloat(), top = 0f, right = (xk + 270 * o).toFloat(), bottom = size.height) {
                for (i in QA_OGELER.indices.sortedByDescending { abs(it - konum) }) {
                    val d = i - konum
                    if (abs(d) < 2f) {
                        val lx = (87 + d * 68f).roundToInt()
                        val ly = QA_DY + (min(abs(d), 1f) * 17f).roundToInt()
                        qaOnizleme(QA_OGELER[i], xk + lx * o, yk + ly * o, o)
                    }
                }
            }

            // 3) 2x3 dugmeler
            val sayfa = onizleme / 6
            for (i in 0 until 6) {
                val ix = sayfa * 6 + i
                if (ix >= n) continue
                val bx = xk + (17 + (i % 3) * 85) * o
                val by = yk + (QA_DY + 244 + (i / 3) * 84) * o
                val b = basili == ix
                qaCerceve(sekil, bx, by, 66 * o, 66 * o, o, ak != b)
                qaResim(QA_OGELER[ix].dugmeIkon, bx + 17 * o, by + 17 * o, o, ak != b)
            }

            // 4) sayfa gostergesi
            for (p in 0 until min(sayfaSayisi, 5)) {
                val ad = if (p == sayfa) "secili_sayfa_gostergesi_8x8.png" else "secili_olmayan_sayfa_gostergesi_8x8.png"
                qaResim(ad, xk + qaGostergeX(sayfaSayisi, p) * o, yk + (QA_DY + 408) * o, o, ak)
            }
            if (sayfaSayisi > 5) {
                qaResim("daha_fazla_sayfa_gostergesi_8x8.png", xk + qaGostergeX(sayfaSayisi, 5) * o, yk + (QA_DY + 408) * o, o, ak)
            }

            // 5) alt satir
            val ay = yk + (QA_DY + 430) * o
            val b1 = basili == 100
            qaDaire(xk + 17 * o, ay, o, ak != b1)
            qaResim("secenekler_16x16.png", xk + 25 * o, ay + 8 * o, o, ak != b1)
            qaResim("qanopy_logo_128x32.png", xk + 71 * o, ay, o, ak)
            val b2 = basili == 101
            qaDaire(xk + 221 * o, ay, o, ak != b2)
            qaResim("ayarlar_16x16.png", xk + 229 * o, ay + 8 * o, o, ak != b2)

            // 6) pencereler
            if (pencere != 0) {
                drawRect(Color(0xAA000000))
                val w = qaPencere(pencere)
                val px0 = xk + w.x * o
                val py0 = yk + w.y * o
                qaCerceve(sekil, px0, py0, w.w * o, w.h * o, o, ak)
                if (pencere == 1) {
                    qaYaziOrta("SEÇENEKLER", xk + (w.x + w.w / 2) * o, py0 + 12 * o, o, renk)
                    for (k in w.kutular) {
                        qaYaziDugme(
                            QA_ISLEMLER[k.id - 200].ad, xk + k.x * o, yk + k.y * o, k.w * o, k.h * o,
                            o, sekil, ak, basili == k.id, kok == true
                        )
                    }
                } else if (pencere == 2) {
                    qaYaziOrta("AYARLAR", xk + (w.x + w.w / 2) * o, py0 + 12 * o, o, renk)
                    val satirlar = listOf("TEMA", "EMİN MİSİN", "TİTREŞİM", "KÖŞE")
                    for (r in 0 until 4) {
                        qaYazi(satirlar[r], px0 + 12 * o, yk + (w.y + 40 + r * 42 + 8) * o, o, renk)
                    }
                    for (k in w.kutular) {
                        val kx = xk + k.x * o
                        val ky = yk + k.y * o
                        if (k.id == 210) {
                            qaYaziDugme(QA_TEMALAR[QaAyar.tema.coerceIn(0, 2)], kx, ky, k.w * o, k.h * o, o, sekil, ak, basili == k.id, true)
                        } else if (k.id == 211) {
                            qaResim(if (QaAyar.emin) "switch_on.png" else "switch_off.png", kx + 4 * o, ky + 10 * o, o, ak)
                        } else if (k.id == 212) {
                            qaResim(if (QaAyar.titresim) "switch_on.png" else "switch_off.png", kx + 4 * o, ky + 10 * o, o, ak)
                        } else if (k.id == 213) {
                            qaYaziDugme(QA_KOSELER[QaAyar.kose.coerceIn(0, QA_KOSELER.size - 1)].toString(), kx, ky, k.w * o, k.h * o, o, sekil, ak, basili == k.id, true)
                        }
                    }
                } else {
                    qaYaziOrta("EMİN MİSİN", xk + (w.x + w.w / 2) * o, py0 + 12 * o, o, renk)
                    if (bekleyen >= 0) {
                        qaYaziOrta(QA_ISLEMLER[bekleyen].ad, xk + (w.x + w.w / 2) * o, py0 + 44 * o, o, renk)
                    }
                    for (k in w.kutular) {
                        qaYaziDugme(
                            if (k.id == 220) "EVET" else "HAYIR",
                            xk + k.x * o, yk + k.y * o, k.w * o, k.h * o, o, sekil, ak, basili == k.id, true
                        )
                    }
                }
            }
        }
    }
}
