package io.github.bntheq.qanopy

import android.content.res.Resources
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
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Bir piksel kutusunun boyu (dp). Buyutursan daha kaba, kucultursen daha ince olur.
private const val PIKSEL_DP = 3f

// Yaricapi r olan bir dairenin kosesini c boyunda piksellere oturtur.
// Donen dizi: ustten baslayarak her satirin kenardan ne kadar iceri girdigi.
private fun basamaklar(r: Float, c: Float): FloatArray {
    val m = (r / c).roundToInt()
    if (m <= 0) return FloatArray(0)
    if (m == 1) return floatArrayOf(c)
    val rr = m * c
    return FloatArray(m) { k ->
        val dy = rr - (k + 0.5f) * c
        val x = rr - sqrt(rr * rr - dy * dy)
        (x / c).roundToInt() * c
    }
}

class PikselSekil(
    ts: CornerSize,
    te: CornerSize,
    be: CornerSize,
    bs: CornerSize
) : CornerBasedShape(ts, te, be, bs) {

    constructor(birim: Dp) : this(
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
        val c = PIKSEL_DP * Resources.getSystem().displayMetrics.density
        val lim = minOf(w, h) / 2f
        val ltr = layoutDirection == LayoutDirection.Ltr
        val rTL = minOf(if (ltr) topStart else topEnd, lim)
        val rTR = minOf(if (ltr) topEnd else topStart, lim)
        val rBR = minOf(if (ltr) bottomEnd else bottomStart, lim)
        val rBL = minOf(if (ltr) bottomStart else bottomEnd, lim)
        val a = basamaklar(rTL, c)
        val b = basamaklar(rTR, c)
        val d = basamaklar(rBR, c)
        val e = basamaklar(rBL, c)
        val p = Path()
        if (a.isNotEmpty()) p.moveTo(a[a.size - 1], a.size * c) else p.moveTo(0f, 0f)
        for (k in a.size - 1 downTo 0) {
            p.lineTo(a[k], (k + 1) * c)
            p.lineTo(a[k], k * c)
        }
        if (b.isEmpty()) p.lineTo(w, 0f)
        for (k in b.indices) {
            p.lineTo(w - b[k], k * c)
            p.lineTo(w - b[k], (k + 1) * c)
        }
        for (k in d.size - 1 downTo 0) {
            p.lineTo(w - d[k], h - (k + 1) * c)
            p.lineTo(w - d[k], h - k * c)
        }
        if (d.isEmpty()) p.lineTo(w, h)
        for (k in e.indices) {
            p.lineTo(e[k], h - k * c)
            p.lineTo(e[k], h - (k + 1) * c)
        }
        if (e.isEmpty()) p.lineTo(0f, h)
        p.close()
        return Outline.Generic(p)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ): CornerBasedShape = PikselSekil(topStart, topEnd, bottomEnd, bottomStart)
}

val PikselSekiller = Shapes(
    extraSmall = PikselSekil(4.dp),
    small = PikselSekil(8.dp),
    medium = PikselSekil(12.dp),
    large = PikselSekil(16.dp),
    extraLarge = PikselSekil(28.dp)
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
