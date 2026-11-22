package io.github.bntheq.qanopy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalYaziStili.current
) {
    val renk = if (color != Color.Unspecified) color
    else if (style.color != Color.Unspecified) style.color
    else LocalIcerikRenk.current
    val son = style.merge(
        TextStyle(
            color = renk,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight
        )
    )
    BasicText(
        text,
        modifier = modifier,
        style = son,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines
    )
}

class QKartRenkleri(val containerColor: Color, val contentColor: Color)
class QGolge

object CardDefaults {
    @Composable
    fun cardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): QKartRenkleri {
        val cs = QTema.colorScheme
        return QKartRenkleri(
            if (containerColor != Color.Unspecified) containerColor else cs.surfaceContainerHighest,
            if (contentColor != Color.Unspecified) contentColor else cs.onSurface
        )
    }

    fun cardElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp,
        draggedElevation: Dp = 0.dp,
        disabledElevation: Dp = 0.dp
    ): QGolge = QGolge()
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = QTema.shapes.medium,
    colors: QKartRenkleri = CardDefaults.cardColors(),
    elevation: QGolge? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val m = modifier.clip(shape).background(colors.containerColor)
    Column(if (border != null) m.border(border, shape) else m) {
        CompositionLocalProvider(LocalIcerikRenk provides colors.contentColor) { content() }
    }
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = QTema.shapes.medium,
    colors: QKartRenkleri = CardDefaults.cardColors(),
    elevation: QGolge? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val m = modifier.clip(shape).background(colors.containerColor).clickable(onClick = onClick)
    Column(if (border != null) m.border(border, shape) else m) {
        CompositionLocalProvider(LocalIcerikRenk provides colors.contentColor) { content() }
    }
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = QTema.shapes.extraLarge
) {
    val cs = QTema.colorScheme
    val tp = QTema.typography
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .clip(shape)
                .background(cs.surface)
                .pikselCerceve(cs.outline, 32.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompositionLocalProvider(LocalIcerikRenk provides cs.onSurface) {
                if (title != null) {
                    CompositionLocalProvider(LocalYaziStili provides tp.headlineSmall) { title() }
                }
                if (text != null) {
                    Box(Modifier.weight(1f, fill = false)) {
                        CompositionLocalProvider(LocalYaziStili provides tp.bodyMedium) { text() }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (dismissButton != null) dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun QanopyZemin(content: @Composable (PaddingValues) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(QTema.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        content(PaddingValues(0.dp))
    }
}
