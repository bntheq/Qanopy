package io.github.bntheq.qanopy

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun PikselSekmeSatiri(secili: Int, basliklar: List<Int>, sec: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        basliklar.forEachIndexed { i, b ->
            if (i == secili) {
                Button(onClick = { sec(i) }) { Text(stringResource(b)) }
            } else {
                OutlinedButton(onClick = { sec(i) }) { Text(stringResource(b)) }
            }
        }
    }
}
