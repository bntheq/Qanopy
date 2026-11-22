package io.github.bntheq.qanopy

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

// Bu dosya CEOQ klasorundeki 24x24 PNG'lerden otomatik uretildi.
object PikselIkonlar {
    val AYAR = arrayOf(
        "........................",
        "........................",
        "...........##...........",
        "...##......##......##...",
        "...###.....##.....###...",
        "....###...####...###....",
        ".....#..###########.....",
        ".......###....####......",
        "......##...##...##......",
        "......##.######.##......",
        ".....##..##..##..##.....",
        "..#####.##....##.#####..",
        "..#####.##....##.#####..",
        ".....##..##..##..##.....",
        "......##.######.##......",
        "......##...##...##......",
        "......####....###.......",
        ".....###########..#.....",
        "....###...####...###....",
        "...###.....##.....###...",
        "...##......##......##...",
        "...........##...........",
        "........................",
        "........................"
    )
    val GERI = arrayOf(
        "........................",
        "........................",
        "........................",
        "........................",
        "........................",
        "........................",
        "........##..............",
        ".......###..............",
        "......###...............",
        ".....###................",
        "....###.................",
        "...###..................",
        "..####################..",
        "..####################..",
        "........................",
        "........##############..",
        "........................",
        "..............########..",
        "........................",
        "........................",
        "........................",
        "........................",
        "........................",
        "........................"
    )
}

// Her hucre tam sayi piksel; ikon alanin ortasina oturur.
@Composable
fun PikselIkon(satirlar: Array<String>, renk: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = satirlar.size
        val c = (minOf(size.width, size.height) / n).toInt().coerceAtLeast(1)
        val ox = ((size.width - c * n) / 2f).toInt().toFloat()
        val oy = ((size.height - c * n) / 2f).toInt().toFloat()
        for (y in 0 until n) {
            val s = satirlar[y]
            var x = 0
            while (x < s.length) {
                if (s[x] == '#') {
                    var b = x
                    while (b < s.length && s[b] == '#') b++
                    drawRect(renk, Offset(ox + x * c, oy + y * c), Size(((b - x) * c).toFloat(), c.toFloat()))
                    x = b
                } else {
                    x++
                }
            }
        }
    }
}
