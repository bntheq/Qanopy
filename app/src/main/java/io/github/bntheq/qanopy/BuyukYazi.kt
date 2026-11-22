package io.github.bntheq.qanopy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BuyukYazi(icerik: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalYaziStili provides LocalYaziStili.current.merge(TextStyle(fontWeight = FontWeight.Bold))
    ) { icerik() }
}
