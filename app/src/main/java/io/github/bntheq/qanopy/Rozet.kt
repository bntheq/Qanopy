package io.github.bntheq.qanopy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SurumRozeti(yazi: String) {
    val cs = QTema.colorScheme
    Text(
        yazi,
        style = QTema.typography.labelLarge,
        color = cs.onTertiaryContainer,
        modifier = Modifier
            .clip(QTema.shapes.small)
            .background(cs.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun EkranGoruntuKarti(acik: Boolean, degis: (Boolean) -> Unit) {
    Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        RenkKart(2) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.ayar58_shot_title),
                        style = QTema.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(stringResource(R.string.ayar58_shot_help), style = QTema.typography.bodySmall)
                }
                Switch(checked = acik, onCheckedChange = degis)
            }
        }
    }
}
