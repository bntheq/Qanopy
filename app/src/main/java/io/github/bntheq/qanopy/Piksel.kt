package io.github.bntheq.qanopy

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class PikselSekil(
    private val adim: Int,
    ts: CornerSize,
    te: CornerSize,
    be: CornerSize,
    bs: CornerSize
) : CornerBasedShape(ts, te, be, bs) {

    constructor(birim: Dp, adim: Int) : this(
        adim,
        CornerSize(birim),
        CornerSize(birim),
        CornerSize(birim),
        CornerSize(birim)
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val w = size.width
        val h = size.height
        val n = adim
        val s = topStart / n
        val p = Path()
        p.moveTo(n * s, 0f)
        p.lineTo(w - n * s, 0f)
        for (i in 1..n) {
            p.lineTo(w - (n - i + 1) * s, i * s)
            p.lineTo(w - (n - i) * s, i * s)
        }
        p.lineTo(w, h - n * s)
        for (i in 1..n) {
            p.lineTo(w - (i - 1) * s, h - (n - i) * s)
            p.lineTo(w - i * s, h - (n - i) * s)
        }
        p.lineTo(n * s, h)
        for (i in 1..n) {
            p.lineTo((n - i + 1) * s, h - i * s)
            p.lineTo((n - i) * s, h - i * s)
        }
        p.lineTo(0f, n * s)
        for (i in 1..n) {
            p.lineTo(i * s, (n - i + 1) * s)
            p.lineTo(i * s, (n - i) * s)
        }
        p.close()
        return Outline.Generic(p)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ): CornerBasedShape = PikselSekil(adim, topStart, topEnd, bottomEnd, bottomStart)
}

val PikselSekiller = Shapes(
    extraSmall = PikselSekil(2.dp, 1),
    small = PikselSekil(3.dp, 2),
    medium = PikselSekil(4.dp, 2),
    large = PikselSekil(4.dp, 3),
    extraLarge = PikselSekil(4.dp, 3)
)

private val mono = FontFamily.Monospace

val PikselYazi: Typography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = mono),
        displayMedium = t.displayMedium.copy(fontFamily = mono),
        displaySmall = t.displaySmall.copy(fontFamily = mono),
        headlineLarge = t.headlineLarge.copy(fontFamily = mono),
        headlineMedium = t.headlineMedium.copy(fontFamily = mono),
        headlineSmall = t.headlineSmall.copy(fontFamily = mono),
        titleLarge = t.titleLarge.copy(fontFamily = mono),
        titleMedium = t.titleMedium.copy(fontFamily = mono),
        titleSmall = t.titleSmall.copy(fontFamily = mono),
        bodyLarge = t.bodyLarge.copy(fontFamily = mono),
        bodyMedium = t.bodyMedium.copy(fontFamily = mono),
        bodySmall = t.bodySmall.copy(fontFamily = mono),
        labelLarge = t.labelLarge.copy(fontFamily = mono),
        labelMedium = t.labelMedium.copy(fontFamily = mono),
        labelSmall = t.labelSmall.copy(fontFamily = mono)
    )
}
