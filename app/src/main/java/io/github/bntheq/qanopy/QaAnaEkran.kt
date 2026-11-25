package io.github.bntheq.qanopy

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

object QaVarlik {
    lateinit var baglam: Context
    private val onbellek = HashMap<String, ImageBitmap>()
    fun al(ad: String): ImageBitmap {
        val var1 = onbellek[ad]
        if (var1 != null) return var1
        val ayar = BitmapFactory.Options()
        ayar.inScaled = false
        val bmp = baglam.assets.open("qa/" + ad).use { BitmapFactory.decodeStream(it, null, ayar) }
            ?: throw IllegalStateException("resim okunamadi: " + ad)
        val ib = bmp.asImageBitmap()
        onbellek[ad] = ib
        return ib
    }
}

class QaOge(val ad: String, val dugmeIkon: String, val onizleme: String)

val QA_OGELER = listOf(
    QaOge("kilit_ekrani", "kilit_ekrani.png", "kilit_ekrani_ogeleri.png"),
    QaOge("ana_ekran", "ana_sayfa.png", "ana_ekran_ogeleri.png"),
    QaOge("durum_cubugu", "durum_cubugu.png", "durum_cubugu_ogeleri.png"),
    QaOge("navigasyon_cubugu", "navigasyon_cubugu.png", "navigasyon_cubugu_ogesi.png"),
    QaOge("uygulama_cekmecesi", "uygulama_cekmecesi.png", "uygulama_cekmecesi_ogeleri.png"),
    QaOge("kontrol_paneli", "kontrol_paneli.png", "kontrol_paneli_ogeleri.png")
)

const val QA_DUGME_SEKIL = 7
const val QA_BLOK = 462

private fun DrawScope.qaParca(im: ImageBitmap, sx: Int, sy: Int, sw: Int, sh: Int, dx: Int, dy: Int, dw: Int, dh: Int) {
    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) return
    drawImage(im, IntOffset(sx, sy), IntSize(sw, sh), IntOffset(dx, dy), IntSize(dw, dh), filterQuality = FilterQuality.None)
}

private fun DrawScope.qaResim(ad: String, x: Int, y: Int, o: Int) {
    val im = QaVarlik.al(ad)
    qaParca(im, 0, 0, im.width, im.height, x, y, im.width * o, im.height * o)
}

private fun DrawScope.qaDokuz(im: ImageBitmap, hx: Int, hy: Int, hs: Int, k: Int, x: Int, y: Int, w: Int, h: Int, o: Int) {
    val kd = k * o
    val m = hs - 2 * k
    qaParca(im, hx, hy, k, k, x, y, kd, kd)
    qaParca(im, hx + hs - k, hy, k, k, x + w - kd, y, kd, kd)
    qaParca(im, hx, hy + hs - k, k, k, x, y + h - kd, kd, kd)
    qaParca(im, hx + hs - k, hy + hs - k, k, k, x + w - kd, y + h - kd, kd, kd)
    qaParca(im, hx + k, hy, m, k, x + kd, y, w - 2 * kd, kd)
    qaParca(im, hx + k, hy + hs - k, m, k, x + kd, y + h - kd, w - 2 * kd, kd)
    qaParca(im, hx, hy + k, k, m, x, y + kd, kd, h - 2 * kd)
    qaParca(im, hx + hs - k, hy + k, k, m, x + w - kd, y + kd, kd, h - 2 * kd)
    qaParca(im, hx + k, hy + k, m, m, x + kd, y + kd, w - 2 * kd, h - 2 * kd)
}

private fun DrawScope.qaOnizleme(oge: QaOge, x: Int, y: Int, o: Int) {
    qaResim("duvar_kagidi.png", x, y, o)
    qaResim(oge.onizleme, x, y, o)
    qaResim("telefon_dis_cerceve_seffaf.png", x, y, o)
}

@Composable
fun QaAnaEkran(onEski: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val gw = constraints.maxWidth
        val gh = constraints.maxHeight
        val o = maxOf(1, gw / 270)
        val xk = (gw - 270 * o) / 2
        val yk = maxOf(0, (gh - QA_BLOK * o) / (2 * o)) * o
        Canvas(
            Modifier.fillMaxSize().pointerInput(o, xk, yk) {
                detectTapGestures(onLongPress = { p ->
                    val lx = xk + 71 * o
                    val ly = yk + 430 * o
                    if (p.x >= lx && p.x <= lx + 128 * o && p.y >= ly && p.y <= ly + 32 * o) onEski()
                })
            }
        ) {
            val simdi = 0
            if (simdi - 1 >= 0) qaOnizleme(QA_OGELER[simdi - 1], xk + 19 * o, yk + 17 * o, o)
            if (simdi + 1 < QA_OGELER.size) qaOnizleme(QA_OGELER[simdi + 1], xk + 155 * o, yk + 17 * o, o)
            qaOnizleme(QA_OGELER[simdi], xk + 87 * o, yk, o)

            val sekil = QaVarlik.al("qanopy_shapes_32x32.png")
            val hx = (QA_DUGME_SEKIL % 5) * 34
            val hy = (QA_DUGME_SEKIL / 5) * 34
            for (i in 0 until minOf(6, QA_OGELER.size)) {
                val bx = xk + (17 + (i % 3) * 85) * o
                val by = yk + (244 + (i / 3) * 84) * o
                qaDokuz(sekil, hx, hy, 32, 12, bx, by, 66 * o, 66 * o, o)
                qaResim(QA_OGELER[i].dugmeIkon, bx + 17 * o, by + 17 * o, o)
            }

            val sayfaSayisi = (QA_OGELER.size + 5) / 6
            val toplam = sayfaSayisi * 8 + (sayfaSayisi - 1) * 9
            val gx0 = (270 - toplam) / 2
            for (p in 0 until sayfaSayisi) {
                val ad = if (p == 0) "secili_sayfa_gostergesi_8x8.png" else "secili_olmayan_sayfa_gostergesi_8x8.png"
                qaResim(ad, xk + (gx0 + p * 17) * o, yk + 408 * o, o)
            }

            val k16 = QaVarlik.al("qanopy_shapes_16x16.png")
            qaParca(k16, 0, 0, 16, 16, xk + 17 * o, yk + 430 * o, 32 * o, 32 * o)
            qaResim("secenekler_16x16.png", xk + 25 * o, yk + 438 * o, o)
            qaParca(k16, 0, 0, 16, 16, xk + 221 * o, yk + 430 * o, 32 * o, 32 * o)
            qaResim("ayarlar_16x16.png", xk + 229 * o, yk + 438 * o, o)
            qaResim("qanopy_logo_128x32.png", xk + 71 * o, yk + 430 * o, o)
        }
    }
}
