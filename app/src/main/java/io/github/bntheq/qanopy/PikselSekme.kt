package io.github.bntheq.qanopy

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun PikselSekmeSatiri(secili: Int, basliklar: List<Int>, sec: (Int) -> Unit) {
    val kay = rememberScrollState()
    val konum = remember { mutableStateMapOf<Int, Int>() }
    val pay = with(LocalDensity.current) { 24.dp.roundToPx() }
    val hedef = konum[secili]
    LaunchedEffect(secili, hedef) {
        if (hedef != null) kay.animateScrollTo((hedef - pay).coerceAtLeast(0))
    }
    Row(
        modifier = Modifier
            .horizontalScroll(kay)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        basliklar.forEachIndexed { i, b ->
            Box(Modifier.onGloballyPositioned { konum[i] = it.positionInParent().x.toInt() }) {
                if (i == secili) {
                    Button(onClick = { sec(i) }) { Text(stringResource(b)) }
                } else {
                    OutlinedButton(onClick = { sec(i) }) { Text(stringResource(b)) }
                }
            }
        }
    }
}
