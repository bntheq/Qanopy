package io.github.bntheq.qanopy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val GirisSekil = PikselSekil(12.dp)

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val cs = QTema.colorScheme
    val zemin = if (selected) cs.secondaryContainer else Color.Transparent
    val yazi = if (!enabled) cs.onSurface.copy(alpha = 0.38f)
    else if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant
    val m = modifier
        .heightIn(min = 40.dp)
        .clip(GirisSekil)
        .background(zemin)
        .let { if (selected) it else it.pikselCerceveRenk(enabled, 12.dp) }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 8.dp)
    Row(m, verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalIcerikRenk provides yazi) { label() }
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false
) {
    val kaynak = remember { MutableInteractionSource() }
    val odak by kaynak.collectIsFocusedAsState()
    val cs = QTema.colorScheme
    val tp = QTema.typography
    val renk = if (!enabled) cs.onSurface.copy(alpha = 0.12f) else if (odak) cs.primary else cs.outline
    val yaziRenk = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.38f)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .widthIn(min = 240.dp)
            .clip(GirisSekil)
            .background(cs.surfaceVariant)
            .pikselCerceve(renk, 12.dp),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = tp.bodyLarge.copy(color = yaziRenk),
        cursorBrush = SolidColor(cs.primary),
        interactionSource = kaynak,
        decorationBox = { inner ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (label != null) {
                    CompositionLocalProvider(
                        LocalIcerikRenk provides cs.onSurfaceVariant,
                        LocalYaziStili provides tp.labelMedium
                    ) { label() }
                }
                inner()
            }
        }
    )
}
