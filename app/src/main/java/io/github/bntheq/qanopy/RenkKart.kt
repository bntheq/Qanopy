package io.github.bntheq.qanopy

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun kartKap(ton: Int): Color {
    val cs = QTema.colorScheme
    return when (ton % 3) {
        0 -> cs.primaryContainer
        1 -> cs.secondaryContainer
        else -> cs.tertiaryContainer
    }
}

@Composable
fun kartIc(ton: Int): Color {
    val cs = QTema.colorScheme
    return when (ton % 3) {
        0 -> cs.onPrimaryContainer
        1 -> cs.onSecondaryContainer
        else -> cs.onTertiaryContainer
    }
}

fun kartTon(baslik: String): Int = Math.abs(baslik.hashCode() % 3)

@Composable
fun RenkKart(ton: Int, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = QTema.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = kartKap(ton), contentColor = kartIc(ton)),
        content = content
    )
}
