package io.github.bntheq.qanopy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

// Sprite 9-dilim cerceve: dis cizgi verilen renkte, ic cizgi onun yari koyusu.
// Dilimler yalnizca boyut degisince hesaplanir; her karede sadece cizilir.
fun Modifier.pikselCerceve(renk: Color, sekil: PikselSekil): Modifier = this.drawWithCache {
    val sp = PikselSprite.al(sekil.no)
    val w = size.width.roundToInt()
    val h = size.height.roundToInt()
    val dilimler = if (sp != null && w > 0 && h > 0) PikselSprite.dilimle(sp.kose, w, h) else null
    val disF = ColorFilter.tint(renk)
    val icF = ColorFilter.tint(
        renk.copy(red = renk.red * 0.5f, green = renk.green * 0.5f, blue = renk.blue * 0.5f)
    )
    onDrawWithContent {
        drawContent()
        if (sp != null && dilimler != null) {
            katmanCiz(sp.dis, dilimler, disF)
            katmanCiz(sp.ic, dilimler, icF)
        }
    }
}

fun Modifier.pikselCerceve(renk: Color, rad: Dp): Modifier =
    this.pikselCerceve(renk, PikselSekil(rad))

@Composable
fun Modifier.pikselCerceveRenk(aktif: Boolean, rad: Dp): Modifier {
    val cs = QTema.colorScheme
    val renk = if (aktif) cs.outline else cs.onSurface.copy(alpha = 0.12f)
    return this.pikselCerceve(renk, rad)
}
