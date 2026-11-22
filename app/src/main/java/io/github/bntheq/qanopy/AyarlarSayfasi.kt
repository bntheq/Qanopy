package io.github.bntheq.qanopy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun SecimListesi(secenekler: List<String>, secili: Int, sec: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        secenekler.forEachIndexed { i, ad ->
            if (i == secili) {
                Button(onClick = { sec(i) }, modifier = Modifier.fillMaxWidth()) { Text(ad) }
            } else {
                OutlinedButton(onClick = { sec(i) }, modifier = Modifier.fillMaxWidth()) { Text(ad) }
            }
        }
    }
}

@Composable
private fun AyarBaslik(metin: String) {
    Text(metin, style = QTema.typography.titleLarge, fontWeight = FontWeight.Black)
}

@Composable
fun UygulamaAyarlari() {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val surum = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RenkKart(0) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AyarBaslik(stringResource(R.string.ayar57_theme))
                SecimListesi(
                    listOf(
                        stringResource(R.string.ayar57_theme_system),
                        stringResource(R.string.ayar57_theme_light),
                        stringResource(R.string.ayar57_theme_dark)
                    ),
                    Tercih.tema
                ) { Tercih.temaSec(ctx, it) }
            }
        }
        RenkKart(1) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AyarBaslik(stringResource(R.string.ayar57_color))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.ayar57_color_dynamic), modifier = Modifier.weight(1f))
                    Switch(checked = Tercih.dinamikRenk, onCheckedChange = { Tercih.renkSec(ctx, it) })
                }
                Text(stringResource(R.string.ayar57_color_help), style = QTema.typography.bodySmall)
            }
        }
        RenkKart(2) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AyarBaslik(stringResource(R.string.ayar57_font))
                SecimListesi(
                    listOf(
                        stringResource(R.string.ayar57_font_pixel),
                        stringResource(R.string.ayar57_font_system)
                    ),
                    if (Tercih.pikselYazi) 0 else 1
                ) { Tercih.yaziSec(ctx, it == 0) }
                Text(stringResource(R.string.ayar57_font_help), style = QTema.typography.bodySmall)
            }
        }
        RenkKart(0) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AyarBaslik(stringResource(R.string.ayar57_about))
                Text(stringResource(R.string.ayar57_version, surum))
                Text(stringResource(R.string.ayar57_license))
                OutlinedButton(
                    onClick = { uri.openUri("https://github.com/bntheq/Qanopy") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.ayar57_github)) }
            }
        }
    }
}
