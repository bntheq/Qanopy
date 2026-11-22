package io.github.bntheq.qanopy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Secenekleri yan yana (sutun sayisi kadar) dizer; secili olan dolu, digerleri cerceveli.
@Composable
fun <T> SecenekSatirlari(
    secenekler: List<T>,
    sutun: Int,
    secili: (T) -> Boolean,
    etiket: (T) -> String,
    aktif: Boolean,
    sec: (T) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        secenekler.chunked(sutun).forEach { sira ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sira.forEach { s ->
                    if (secili(s)) {
                        Button(
                            onClick = { sec(s) },
                            enabled = aktif,
                            modifier = Modifier.weight(1f)
                        ) { Text(etiket(s)) }
                    } else {
                        OutlinedButton(
                            onClick = { sec(s) },
                            enabled = aktif,
                            modifier = Modifier.weight(1f)
                        ) { Text(etiket(s)) }
                    }
                }
                repeat(sutun - sira.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
