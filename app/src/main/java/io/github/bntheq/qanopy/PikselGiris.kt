package io.github.bntheq.qanopy

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GirisSekil = PikselSekil(12.dp)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = if (selected) modifier else modifier.pikselCerceveRenk(enabled, 12.dp),
        enabled = enabled,
        shape = GirisSekil,
        border = null
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    val cs = MaterialTheme.colorScheme
    val renk = if (!enabled) cs.onSurface.copy(alpha = 0.12f) else if (odak) cs.primary else cs.outline
    androidx.compose.material3.TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.pikselCerceve(renk, 12.dp),
        enabled = enabled,
        label = label,
        singleLine = singleLine,
        shape = GirisSekil,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
        interactionSource = kaynak
    )
}
