package io.github.bntheq.qanopy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DurumCubuguKarti() {
    val ctx = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var degerler by remember { mutableStateOf<Map<String, AyarMotoru.Okuma>?>(null) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }
    var yenile by remember { mutableIntStateOf(0) }

    val ogeler = listOf(
        Pair("status_bar_clock_seconds", stringResource(R.string.durum41_seconds)),
        Pair("status_bar_clock_bold", stringResource(R.string.durum41_bold)),
        Pair("status_bar_clock_date_display", stringResource(R.string.durum41_date)),
        Pair("status_bar_show_battery_percent", stringResource(R.string.durum41_percent)),
        Pair("statusbar_battery_bar", stringResource(R.string.durum41_bar)),
        Pair("statusbar_notif_count", stringResource(R.string.durum41_notif)),
        Pair("statusbar_colored_icons", stringResource(R.string.durum41_colored))
    )
    val ayarsiz = stringResource(R.string.durum41_unset)
    val tamam = stringResource(R.string.durum41_applied)

    LaunchedEffect(yenile) {
        degerler = withContext(Dispatchers.IO) {
            val m = LinkedHashMap<String, AyarMotoru.Okuma>()
            for (o in ogeler) m[o.first] = AyarMotoru.hizliOku("system", o.first)
            m
        }
    }

    val d = degerler

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.durum41_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.durum41_help), style = MaterialTheme.typography.bodyMedium)
            if (calisiyor) LinearProgressIndicator(Modifier.fillMaxWidth())
            mesaj?.let { Text(it) }
            if (d == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                ogeler.forEach { o ->
                    val okuma = d[o.first]
                    val hata = okuma?.hata
                    val v = okuma?.deger
                    val ozel = hata == null && v != null && v != "0" && v != "1"
                    val acik = v == "1"
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(o.second)
                            if (hata != null) {
                                Text(stringResource(R.string.ayar_error, hata), style = MaterialTheme.typography.bodySmall)
                            } else if (v == null) {
                                Text(ayarsiz, style = MaterialTheme.typography.bodySmall)
                            } else if (ozel) {
                                Text(stringResource(R.string.durum41_custom, v), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Switch(
                            checked = acik,
                            enabled = !calisiyor && hata == null && !ozel,
                            onCheckedChange = { yeni ->
                                calisiyor = true
                                mesaj = null
                                kapsam.launch {
                                    val h = withContext(Dispatchers.IO) {
                                        AyarMotoru.yaz(ctx, "system", o.first, if (yeni) "1" else "0")
                                    }
                                    mesaj = h ?: tamam
                                    yenile++
                                    calisiyor = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
