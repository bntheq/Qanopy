package io.github.bntheq.qanopy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QanopyBaslik(yan: Dp = 12.dp, dikey: Dp = 8.dp, geri: Boolean = false, onAyar: () -> Unit) {
    val cs = QTema.colorScheme
    val aciklama = stringResource(if (geri) R.string.ayar58_back_desc else R.string.ayar57_button_desc)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = yan, vertical = dikey)
            .clip(QTema.shapes.extraLarge)
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
                .clip(QTema.shapes.small)
                .background(cs.primary)
                .clickable(onClickLabel = aciklama, onClick = onAyar)
                .semantics { contentDescription = aciklama },
            contentAlignment = Alignment.Center
        ) {
            PikselIkon(
                satirlar = if (geri) PikselIkonlar.GERI else PikselIkonlar.AYAR,
                renk = cs.onPrimary,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
