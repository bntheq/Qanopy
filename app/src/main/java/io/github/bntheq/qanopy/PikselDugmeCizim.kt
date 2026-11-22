package io.github.bntheq.qanopy

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Kullanicinin cizdigi dugme PNG'leri (32x14 hucre). Otomatik uretildi.
object DugmeCizimleri {
    val DOLU = listOf(
        ".....###########################",
        "..##############################",
        ".###############################",
        ".###############################",
        ".###############################",
        "################################",
        "################################",
        "################################",
        "################################",
        ".##############################.",
        ".##############################.",
        ".##############################.",
        "..############################..",
        ".....######################....."
    )
    val BOS = listOf(
        ".....###########################",
        "..##############################",
        ".####...........................",
        ".##...........................##",
        ".##...........................##",
        "##............................##",
        "##............................##",
        "##............................##",
        "##............................##",
        ".##..........................##.",
        ".##..........................##.",
        ".####......................####.",
        "..############################..",
        ".....######################....."
    )
}

private fun kaynakKol(x: Int, nx: Int): Int = when {
    x < 6 -> x
    x >= nx - 6 -> 32 - (nx - x)
    else -> 15
}

// 9 dilim: sol 6 + sag 6 sutun, ust 5 + alt 5 satir aynen; orta kisim uzar.
fun Modifier.pikselDugmeCiz(satirlar: List<String>, renk: Color): Modifier = this.drawWithCache {
    val c = PIKSEL_DP.dp.toPx()
    val w = size.width
    val h = size.height
    val nx = maxOf((w / c).roundToInt(), 12)
    val ny = maxOf((h / c).roundToInt(), 10)
    val yol = Path()
    for (y in 0 until ny) {
        val ks = when {
            y < 5 -> y
            y >= ny - 5 -> 14 - (ny - y)
            else -> 6
        }
        val s = satirlar[ks]
        val ty = (y * h / ny).roundToInt().toFloat()
        val by = ((y + 1) * h / ny).roundToInt().toFloat()
        var x = 0
        while (x < nx) {
            if (s[kaynakKol(x, nx)] == '#') {
                val bas = x
                while (x < nx && s[kaynakKol(x, nx)] == '#') x++
                val sol = (bas * w / nx).roundToInt().toFloat()
                val sag = (x * w / nx).roundToInt().toFloat()
                yol.addRect(Rect(sol, ty, sag, by))
            } else x++
        }
    }
    onDrawBehind { drawPath(yol, renk) }
}
