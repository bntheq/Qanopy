package io.github.bntheq.qanopy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QanopyBaslik() {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cs.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Text(
            stringResource(R.string.app_name),
            color = cs.onPrimaryContainer,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Row(
            Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(24.dp).clip(MaterialTheme.shapes.extraSmall).background(cs.tertiary))
            Box(Modifier.size(24.dp).clip(MaterialTheme.shapes.extraSmall).background(cs.secondary))
            Box(Modifier.size(24.dp).clip(MaterialTheme.shapes.extraSmall).background(cs.primary))
        }
    }
}
