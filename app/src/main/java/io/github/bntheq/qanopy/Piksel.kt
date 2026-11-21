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
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Bir piksel kutusunun boyu (dp). 1 = yaricap kac dp ise o kadar piksel.
private const val PIKSEL_DP = 1f

// Rasterizasyon: yaricapi R piksel olan dairenin kosesi.
// Her satir k (0 = en dis satir) icin, piksel merkezi daire icinde mi diye bakilir.
// Donen dizi: satirin kenardan kac piksel iceride basladigi.
private fun kose(rDeger: Float, c: Float): IntArray {
    val r = (rDeger / c).roundToInt()
    if (r <= 0) return IntArray(0)
    val rf = r.toFloat()
    return IntArray(r) { k ->
        val dy = rf - k - 0.5f
        val dx = sqrt(rf * rf - dy * dy)
        val ic = ceil(rf - 0.5f - dx).toInt()
        if (ic < 0) 0 else ic
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
        // Tum koordinatlar fiziksel piksel cizgisine oturur (kenarlar bulanmasin).
        fun q(v: Float): Float = Math.round(v).toFloat()
        val lim = minOf(w, h) / 2f
        val ltr = layoutDirection == LayoutDirection.Ltr
        val a = kose(minOf(if (ltr) topStart else topEnd, lim), c)
        val b = kose(minOf(if (ltr) topEnd else topStart, lim), c)
        val d = kose(minOf(if (ltr) bottomEnd else bottomStart, lim), c)
        val e = kose(minOf(if (ltr) bottomStart else bottomEnd, lim), c)
        val p = Path()
        // Sol ust: soldan yukari
        if (a.isNotEmpty()) p.moveTo(q(a[a.size - 1] * c), q(a.size * c)) else p.moveTo(0f, 0f)
        for (k in a.size - 1 downTo 0) {
            p.lineTo(q(a[k] * c), q((k + 1) * c))
            p.lineTo(q(a[k] * c), q(k * c))
        }
        // Sag ust: ustten asagi
        if (b.isEmpty()) p.lineTo(w, 0f)
        for (k in b.indices) {
            p.lineTo(w - q(b[k] * c), q(k * c))
            p.lineTo(w - q(b[k] * c), q((k + 1) * c))
        }
        // Sag alt: sagdan asagi
        for (k in d.size - 1 downTo 0) {
            p.lineTo(w - q(d[k] * c), h - q((k + 1) * c))
            p.lineTo(w - q(d[k] * c), h - q(k * c))
        }
        if (d.isEmpty()) p.lineTo(w, h)
        // Sol alt: alttan yukari
        for (k in e.indices) {
            p.lineTo(q(e[k] * c), h - q(k * c))
            p.lineTo(q(e[k] * c), h - q((k + 1) * c))
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
