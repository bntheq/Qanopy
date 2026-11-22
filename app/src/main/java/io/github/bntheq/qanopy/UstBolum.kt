package io.github.bntheq.qanopy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

// Baslik + sekme satirini her sayfanin kendi icine koymak icin.
val LocalUst = compositionLocalOf<@Composable () -> Unit> { {} }

@Composable
fun UstBolum() {
    LocalUst.current()
}
