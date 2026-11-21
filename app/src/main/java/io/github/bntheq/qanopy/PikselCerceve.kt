package io.github.bntheq.qanopy

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun yola(o: Outline): Path = when (o) {
    is Outline.Generic -> o.path
    is Outline.Rectangle -> Path().apply { addRect(o.rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
}

// Piksel cerceve: dis sekil eksi 1 hucre iceri cekilmis sekil.
// Cerceve kalinligi tam 1 piksel hucresi (PIKSEL_DP), basamaklarla birlikte cizilir.
fun Modifier.pikselCerceve(renk: Color, rad: Dp): Modifier = this.drawWithContent {
    drawContent()
    val c = PIKSEL_DP.dp.toPx()
    val w = size.width
    val h = size.height
    if (w > 2f * c && h > 2f * c) {
        val dis = yola(PikselSekil(rad).createOutline(size, layoutDirection, this))
        val r = (rad.toPx() - c).coerceAtLeast(0f)
        val ic = yola(
            PikselSekil(CornerSize(r), CornerSize(r), CornerSize(r), CornerSize(r))
                .createOutline(Size(w - 2f * c, h - 2f * c), layoutDirection, this)
        )
        ic.translate(Offset(c, c))
        val fark = Path.combine(PathOperation.Difference, dis, ic)
        drawPath(fark, renk)
    }
}

@Composable
fun Modifier.pikselCerceveRenk(aktif: Boolean, rad: Dp): Modifier {
    val cs = MaterialTheme.colorScheme
    val renk = if (aktif) cs.outline else cs.onSurface.copy(alpha = 0.12f)
    return this.pikselCerceve(renk, rad)
}
