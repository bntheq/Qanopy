package io.github.bntheq.qanopy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Piksel sekilli oge seti. Ayni isimli fonksiyonlar paket icinde oldugu icin
// material3 importlari silinince butun cagrilar otomatik buraya baglanir.

private val PikselDugme = PikselSekil(12.dp)
private val PikselIz = PikselSekil(16.dp)
private val PikselTop = PikselSekil(8.dp)

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PikselDugme,
        content = content
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PikselDugme,
        content = content
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PikselDugme,
        content = content
    )
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    val iz = if (checked) cs.primary else cs.surfaceVariant
    val kenar = if (checked) cs.primary else cs.outline
    val top = if (checked) cs.onPrimary else cs.outline
    val a = if (enabled) 1f else 0.38f
    val m = if (onCheckedChange != null) {
        modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    } else modifier
    Box(
        modifier = m
            .size(52.dp, 32.dp)
            .alpha(a)
            .background(iz, PikselIz)
            .border(2.dp, kenar, PikselIz)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(24.dp)
                .background(top, PikselTop)
        )
    }
}

@Composable
private fun pikselZaman(sureMs: Long): State<Float> {
    val t = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { n ->
                t.value = ((n / 1_000_000L) % sureMs).toFloat() / sureMs.toFloat()
            }
        }
    }
    return t
}

@Composable
private fun PikselCubuk(modifier: Modifier, oran: Float?, color: Color, trackColor: Color) {
    val cs = MaterialTheme.colorScheme
    val dolu = if (color == Color.Unspecified) cs.primary else color
    val iz = if (trackColor == Color.Unspecified) cs.surfaceVariant else trackColor
    val t = if (oran == null) pikselZaman(1400L) else null
    Canvas(modifier.width(240.dp).height(8.dp)) {
        val c = 4.dp.toPx()
        val n = (size.width / c).toInt()
        drawRect(iz)
        if (oran != null) {
            val k = (oran.coerceIn(0f, 1f) * n).toInt()
            if (k > 0) drawRect(dolu, Offset.Zero, Size(k * c, size.height))
        } else {
            val seg = maxOf(n / 4, 3)
            val konum = ((n + seg) * (t?.value ?: 0f)).toInt() - seg
            val sol = maxOf(konum, 0)
            val sag = minOf(konum + seg, n)
            if (sag > sol) drawRect(dolu, Offset(sol * c, 0f), Size((sag - sol) * c, size.height))
        }
    }
}

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified
) {
    PikselCubuk(modifier, null, color, trackColor)
}

@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified
) {
    PikselCubuk(modifier, progress(), color, trackColor)
}

@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified
) {
    PikselCubuk(modifier, progress, color, trackColor)
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val renk = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val t = pikselZaman(1000L)
    Canvas(modifier.size(36.dp)) {
        val c = size.width / 3f
        val g = 2.dp.toPx()
        val yol = listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2, 1 to 2, 0 to 2, 0 to 1)
        val aktif = (t.value * 8f).toInt() % 8
        for (i in 0 until 8) {
            val uzak = (aktif - i + 8) % 8
            val al = maxOf(0.2f, 1f - uzak * 0.2f)
            drawRect(
                renk.copy(alpha = al),
                Offset(yol[i].first * c, yol[i].second * c),
                Size(c - g, c - g)
            )
        }
    }
}
