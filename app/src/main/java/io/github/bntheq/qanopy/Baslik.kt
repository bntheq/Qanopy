package io.github.bntheq.qanopy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QanopyBaslik(onAyar: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val aciklama = stringResource(R.string.ayar57_button_desc)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cs.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            stringResource(R.string.app_name),
            color = cs.onPrimaryContainer,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(52.dp)
                .clip(MaterialTheme.shapes.small)
                .background(cs.primary)
                .clickable(onClickLabel = aciklama, onClick = onAyar)
                .semantics { contentDescription = aciklama },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(25.dp)) {
                val c = size.width / 5f
                for (i in 0 until 3) {
                    drawRect(cs.onPrimary, Offset(0f, i * 2 * c), Size(size.width, c))
                }
            }
        }
    }
}
