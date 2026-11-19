package io.github.bntheq.qanopy

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Kart(baslik: String, deger: String, aciklama: String? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(baslik, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(deger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (aciklama != null) {
                Text(aciklama, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun Ekran() {
    val ctx = LocalContext.current
    val pano = LocalClipboardManager.current
    var deneme by remember { mutableIntStateOf(0) }
    var durum by remember { mutableStateOf<Durum?>(null) }

    LaunchedEffect(deneme) {
        durum = null
        durum = withContext(Dispatchers.IO) { Denetim.kontrolEt() }
    }

    val surum = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.surum_etiket, surum), style = MaterialTheme.typography.titleSmall)

        val d = durum
        if (d == null) {
            CircularProgressIndicator()
            Text(stringResource(R.string.kontrol_ediliyor))
        } else if (!d.cihazUygun) {
            Kart(
                stringResource(R.string.kilit_baslik),
                d.cihaz,
                stringResource(R.string.kilit_aciklama, d.cihaz)
            )
        } else {
            Kart(stringResource(R.string.cihaz_baslik), stringResource(R.string.cihaz_uygun, d.cihaz))
            if (d.rootVar == true) {
                Kart(stringResource(R.string.root_baslik), stringResource(R.string.root_var))
                Kart(
                    stringResource(R.string.magisk_baslik),
                    d.magisk ?: stringResource(R.string.magisk_bilinmiyor)
                )
                Kart(
                    stringResource(R.string.modul_baslik),
                    if (d.modulKurulu == true) stringResource(R.string.modul_kurulu)
                    else stringResource(R.string.modul_yok)
                )
            } else {
                Kart(
                    stringResource(R.string.root_baslik),
                    stringResource(R.string.root_yok),
                    stringResource(R.string.root_yok_aciklama)
                )
            }
        }

        if (d != null) {
            val kopyalandi = stringResource(R.string.rapor_kopyalandi)
            Button(onClick = { deneme++ }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tekrar_dene))
            }
            OutlinedButton(
                onClick = {
                    val rapor = "Qanopy " + surum +
                        " | cihaz=" + d.cihaz +
                        " | uygun=" + d.cihazUygun +
                        " | root=" + d.rootVar +
                        " | magisk=" + d.magisk +
                        " | modul=" + d.modulKurulu +
                        " | sdk=" + Build.VERSION.SDK_INT +
                        " | ayrinti=" + d.ayrinti
                    pano.setText(AnnotatedString(rapor))
                    Toast.makeText(ctx, kopyalandi, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.rapor_kopyala))
            }
        }
    }
}
