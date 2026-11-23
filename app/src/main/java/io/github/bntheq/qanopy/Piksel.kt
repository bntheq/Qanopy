package io.github.bntheq.qanopy

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Bir piksel kutusunun boyu (dp).
const val PIKSEL_DP = 4f

// Eski dp degerlerini sprite sekil numarasina cevirir.
private fun dpNo(d: Dp): Int = when {
    d.value <= 16f -> 3
    d.value <= 20f -> 6
    else -> 7
}

// Sekil, qanopy_shapes.png icindeki bir hucredir (0..9). Siluet sprite'tan okunur.
class PikselSekil(
    ts: CornerSize,
    te: CornerSize,
    be: CornerSize,
    bs: CornerSize,
    val no: Int = 3
) : CornerBasedShape(ts, te, be, bs) {

    constructor(birim: Dp) : this(
        CornerSize(birim),
        CornerSize(birim),
        CornerSize(birim),
        CornerSize(birim),
        dpNo(birim)
    )

    constructor(no: Int) : this(
        CornerSize(0.dp),
        CornerSize(0.dp),
        CornerSize(0.dp),
        CornerSize(0.dp),
        no
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
        val sp = PikselSprite.al(no) ?: return Outline.Rectangle(Rect(0f, 0f, w, h))
        val kucuk = minOf(w, h)
        val ol = PikselSprite.olcek(sp.kose, kucuk)
        val olf = ol.toFloat()
        val kr = minOf(sp.kose, (kucuk / 2f / olf).toInt())
        val ltr = layoutDirection == LayoutDirection.Ltr
        val a = IntArray(kr) { (if (ltr) sp.ustSol else sp.ustSag)[it] * ol }
        val b = IntArray(kr) { (if (ltr) sp.ustSag else sp.ustSol)[it] * ol }
        val d = IntArray(kr) { (if (ltr) sp.altSag else sp.altSol)[it] * ol }
        val e = IntArray(kr) { (if (ltr) sp.altSol else sp.altSag)[it] * ol }
        val p = Path()
        // Sol ust: soldan yukari
        if (a.isNotEmpty()) p.moveTo(a[a.size - 1].toFloat(), a.size * olf) else p.moveTo(0f, 0f)
        for (k in a.size - 1 downTo 0) {
            p.lineTo(a[k].toFloat(), (k + 1) * olf)
            p.lineTo(a[k].toFloat(), k * olf)
        }
        // Sag ust: ustten asagi
        if (b.isEmpty()) p.lineTo(w, 0f)
        for (k in b.indices) {
            p.lineTo(w - b[k], k * olf)
            p.lineTo(w - b[k], (k + 1) * olf)
        }
        // Sag alt: sagdan asagi
        for (k in d.size - 1 downTo 0) {
            p.lineTo(w - d[k], h - (k + 1) * olf)
            p.lineTo(w - d[k], h - k * olf)
        }
        if (d.isEmpty()) p.lineTo(w, h)
        // Sol alt: alttan yukari
        for (k in e.indices) {
            p.lineTo(e[k].toFloat(), h - k * olf)
            p.lineTo(e[k].toFloat(), h - (k + 1) * olf)
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
    ): CornerBasedShape = PikselSekil(topStart, topEnd, bottomEnd, bottomStart, no)
}

// Rol -> sekil: kucuk/standart = Sekil3, kart = Sekil6, buyuk kart ve diyalog = Sekil7.
val PikselSekiller = QSekiller(
    extraSmall = PikselSekil(3),
    small = PikselSekil(3),
    medium = PikselSekil(6),
    large = PikselSekil(7),
    extraLarge = PikselSekil(7)
)
