package io.github.bntheq.qanopy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// Bir sprite pikselinin ekranda kac fiziksel piksel oldugu (tam sayi).
const val SPRITE_OLCEK = 3
private const val HUCRE = 32
private val SPRITE_X = intArrayOf(0, 34, 68, 102, 136)
private val SPRITE_Y = intArrayOf(0, 34)

// Bir sekil: kose boyutu, iki cizgi katmani (beyaz maske) ve siluet profilleri.
class SpriteSekil(
    val kose: Int,
    val dis: ImageBitmap,
    val ic: ImageBitmap,
    val ustSol: IntArray,
    val ustSag: IntArray,
    val altSol: IntArray,
    val altSag: IntArray
)

// 9 dilimden birinin kaynak ve hedef dikdortgeni (bir kez hesaplanir).
class Dilim(
    val src: IntOffset,
    val srcBoy: IntSize,
    val dst: IntOffset,
    val dstBoy: IntSize
)

object PikselSprite {
    @Volatile private var sekiller: Array<SpriteSekil>? = null
    @Volatile private var denendi = false

    fun hazirla(ctx: Context) {
        if (denendi) return
        synchronized(this) {
            if (!denendi) {
                sekiller = yukle(ctx)
                denendi = true
            }
        }
    }

    fun al(no: Int): SpriteSekil? = sekiller?.getOrNull(no)

    // Sig an en buyuk tam sayi olcek (3, 2, 1).
    fun olcek(kose: Int, kucukBoyut: Float): Int {
        var o = SPRITE_OLCEK
        while (o > 1 && 2f * kose * o > kucukBoyut) o--
        return o
    }

    fun dilimle(kose: Int, w: Int, h: Int): List<Dilim> {
        val kucuk = minOf(w, h)
        val ol = olcek(kose, kucuk.toFloat())
        val dk = minOf(kose * ol, kucuk / 2)
        val orta = HUCRE - 2 * kose
        val kx = intArrayOf(0, kose, HUCRE - kose)
        val kb = intArrayOf(kose, orta, kose)
        val dx = intArrayOf(0, dk, w - dk)
        val dw = intArrayOf(dk, w - 2 * dk, dk)
        val dy = intArrayOf(0, dk, h - dk)
        val dh = intArrayOf(dk, h - 2 * dk, dk)
        val l = ArrayList<Dilim>()
        for (j in 0..2) {
            for (i in 0..2) {
                if (dw[i] > 0 && dh[j] > 0 && kb[i] > 0 && kb[j] > 0) {
                    l.add(
                        Dilim(
                            IntOffset(kx[i], kx[j]),
                            IntSize(kb[i], kb[j]),
                            IntOffset(dx[i], dy[j]),
                            IntSize(dw[i], dh[j])
                        )
                    )
                }
            }
        }
        return l
    }

    private fun yukle(ctx: Context): Array<SpriteSekil>? {
        return try {
            val o = BitmapFactory.Options()
            o.inScaled = false
            val b = BitmapFactory.decodeResource(ctx.resources, R.drawable.qanopy_shapes, o) ?: return null
            Array(10) { hucre(b, it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun hucre(b: Bitmap, no: Int): SpriteSekil {
        val px = IntArray(HUCRE * HUCRE)
        b.getPixels(px, 0, HUCRE, SPRITE_X[no % 5], SPRITE_Y[no / 5], HUCRE, HUCRE)
        val dis = IntArray(px.size)
        val ic = IntArray(px.size)
        for (i in px.indices) {
            val c = px[i]
            if ((c ushr 24) < 128) continue
            val r = (c shr 16) and 0xFF
            if (r >= 76) dis[i] = -1 else if (r >= 25) ic[i] = -1
        }
        // Kose boyutu: satirlar ve sutunlar ortadakiyle ayni olmaya basladigi en kucuk deger.
        var kose = 15
        for (kk in 1..15) {
            var tamam = true
            for (y in kk..(HUCRE - 1 - kk)) {
                for (x in 0 until HUCRE) {
                    if (px[y * HUCRE + x] != px[16 * HUCRE + x]) tamam = false
                }
            }
            for (x in kk..(HUCRE - 1 - kk)) {
                for (y in 0 until HUCRE) {
                    if (px[y * HUCRE + x] != px[y * HUCRE + 16]) tamam = false
                }
            }
            if (tamam) {
                kose = kk
                break
            }
        }
        fun sol(y: Int): Int {
            for (x in 0 until HUCRE) if ((px[y * HUCRE + x] ushr 24) >= 128) return x
            return 16
        }
        fun sag(y: Int): Int {
            for (x in HUCRE - 1 downTo 0) if ((px[y * HUCRE + x] ushr 24) >= 128) return HUCRE - 1 - x
            return 16
        }
        return SpriteSekil(
            kose,
            Bitmap.createBitmap(dis, HUCRE, HUCRE, Bitmap.Config.ARGB_8888).asImageBitmap(),
            Bitmap.createBitmap(ic, HUCRE, HUCRE, Bitmap.Config.ARGB_8888).asImageBitmap(),
            IntArray(16) { sol(it) },
            IntArray(16) { sag(it) },
            IntArray(16) { sol(HUCRE - 1 - it) },
            IntArray(16) { sag(HUCRE - 1 - it) }
        )
    }
}

fun DrawScope.katmanCiz(img: ImageBitmap, dilimler: List<Dilim>, cf: ColorFilter) {
    for (d in dilimler) {
        drawImage(
            img, d.src, d.srcBoy, d.dst, d.dstBoy,
            colorFilter = cf,
            filterQuality = FilterQuality.None
        )
    }
}
