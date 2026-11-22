package io.github.bntheq.qanopy

import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BuyukYazi(icerik: @Composable () -> Unit) {
    ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) { icerik() }
}
