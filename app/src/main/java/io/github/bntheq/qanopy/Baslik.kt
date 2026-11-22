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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QanopyBaslik(yan: Dp = 12.dp, dikey: Dp = 8.dp, geri: Boolean = false, onAyar: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val aciklama = stringResource(if (geri) R.string.ayar58_back_desc else R.string.ayar57_button_desc)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = yan, vertical = dikey)
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
                if (geri) {
                    val hucre = listOf(2 to 0, 1 to 1, 0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 1 to 3, 2 to 4)
                    hucre.forEach { (x, y) ->
                        drawRect(cs.onPrimary, Offset(x * c, y * c), Size(c, c))
                    }
                } else {
                    for (i in 0 until 3) {
                        drawRect(cs.onPrimary, Offset(0f, i * 2 * c), Size(size.width, c))
                    }
                }
            }
        }
    }
}
