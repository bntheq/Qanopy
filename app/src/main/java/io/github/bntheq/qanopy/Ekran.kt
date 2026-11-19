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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Kart(baslik: String, deger: String, aciklama: String? = null, uyari: Boolean = false) {
    val renkler = if (uyari) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = renkler) {
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
fun LogKart(baslik: String, metin: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(baslik, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(metin, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun Ekran() {
    val ctx = LocalContext.current
    val pano = LocalClipboardManager.current
    val kapsam = rememberCoroutineScope()
    var deneme by remember { mutableIntStateOf(0) }
    var durum by remember { mutableStateOf<Durum?>(DurumOnbellek.son) }

    LaunchedEffect(deneme) {
        if (deneme == 0 && DurumOnbellek.son != null) return@LaunchedEffect
        durum = null
        val yeniDurum = withContext(Dispatchers.IO) { Denetim.kontrolEt() }
        DurumOnbellek.son = yeniDurum
        durum = yeniDurum
    }

    val surum = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }
    val raporKopyalandi = stringResource(R.string.rapor_kopyalandi)
    val logKopyalandi = stringResource(R.string.log_kopyalandi)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

                val modulMetin = if (d.modulKurulu != true) {
                    stringResource(R.string.modul_yok)
                } else if (d.modulKapali == true) {
                    stringResource(R.string.modul_kapali)
                } else {
                    stringResource(R.string.modul_kurulu)
                }
                val modulAciklama: String? = if (d.modulKurulu == true) {
                    stringResource(R.string.surum_etiket, d.modulSurum ?: "?") + "  |  " +
                        stringResource(R.string.sayac_etiket, d.sayac ?: "?")
                } else {
                    null
                }
                Kart(
                    stringResource(R.string.modul_baslik),
                    modulMetin,
                    modulAciklama,
                    uyari = d.modulKapali == true
                )

                if (d.modulKurulu == true) {
                    if (d.guvenliMod == true) {
                        Kart(
                            stringResource(R.string.guvenli_baslik),
                            stringResource(R.string.guvenli_aktif),
                            stringResource(R.string.guvenli_aciklama),
                            uyari = true
                        )
                        Button(
                            onClick = {
                                kapsam.launch {
                                    withContext(Dispatchers.IO) { Denetim.guvenliModdanCik() }
                                    deneme++
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.guvenli_cik))
                        }
                    } else {
                        Kart(
                            stringResource(R.string.guvenli_baslik),
                            stringResource(R.string.guvenli_pasif)
                        )
                    }

                    val logMetin = d.log?.takeIf { it.isNotBlank() } ?: stringResource(R.string.log_bos)
                    LogKart(stringResource(R.string.log_baslik), logMetin)
                    OutlinedButton(
                        onClick = {
                            pano.setText(AnnotatedString(logMetin))
                            Toast.makeText(ctx, logKopyalandi, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.log_kopyala))
                    }
                }
            } else {
                Kart(
                    stringResource(R.string.root_baslik),
                    stringResource(R.string.root_yok),
                    stringResource(R.string.root_yok_aciklama)
                )
            }

            Kart(
                stringResource(R.string.acil_baslik),
                stringResource(R.string.acil_ozet),
                stringResource(R.string.acil_metin)
            )
        }

        if (d != null) {
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
                        " | modulSurum=" + d.modulSurum +
                        " | kapali=" + d.modulKapali +
                        " | guvenli=" + d.guvenliMod +
                        " | sayac=" + d.sayac +
                        " | sdk=" + Build.VERSION.SDK_INT +
                        " | ayrinti=" + d.ayrinti
                    pano.setText(AnnotatedString(rapor))
                    Toast.makeText(ctx, raporKopyalandi, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.rapor_kopyala))
            }
        }
    }
}
