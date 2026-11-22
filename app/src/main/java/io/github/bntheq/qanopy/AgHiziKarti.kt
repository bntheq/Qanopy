package io.github.bntheq.qanopy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val agKilidi = Mutex()

private const val AG_MOD = "network_traffic_mode"
private const val AG_BIRIM = "network_traffic_units"
private const val AG_GIZLE = "network_traffic_autohide"

@Composable
private fun SecenekGrubu(
    secenekler: List<Pair<String, String>>,
    secili: String?,
    aktif: Boolean,
    sec: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        secenekler.chunked(2).forEach { satir ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                satir.forEach { s ->
                    if (s.second == secili) {
                        Button(
                            onClick = { sec(s.second) },
                            enabled = aktif,
                            modifier = Modifier.weight(1f)
                        ) { Text(s.first, maxLines = 1) }
                    } else {
                        OutlinedButton(
                            onClick = { sec(s.second) },
                            enabled = aktif,
                            modifier = Modifier.weight(1f)
                        ) { Text(s.first, maxLines = 1) }
                    }
                }
                if (satir.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AgHiziKarti() {
    val ctx = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var degerler by remember { mutableStateOf<Map<String, AyarMotoru.Okuma>?>(null) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var yenile by remember { mutableIntStateOf(0) }

    val modlar = listOf(
        Pair(stringResource(R.string.ag42_mode_off), "0"),
        Pair(stringResource(R.string.ag42_mode_up), "1"),
        Pair(stringResource(R.string.ag42_mode_down), "2"),
        Pair(stringResource(R.string.ag42_mode_both), "3")
    )
    val birimler = listOf(
        Pair(stringResource(R.string.ag42_unit_kbit), "0"),
        Pair(stringResource(R.string.ag42_unit_mbit), "1"),
        Pair(stringResource(R.string.ag42_unit_kbyte), "2"),
        Pair(stringResource(R.string.ag42_unit_mbyte), "3"),
        Pair(stringResource(R.string.ag42_unit_auto), "4")
    )
    val ayarsiz = stringResource(R.string.ag42_unset)
    val tamam = stringResource(R.string.ag42_applied)

    LaunchedEffect(yenile) {
        degerler = withContext(Dispatchers.IO) {
            val m = LinkedHashMap<String, AyarMotoru.Okuma>()
            for (k in listOf(AG_MOD, AG_BIRIM, AG_GIZLE)) m[k] = AyarMotoru.hizliOku("secure", k)
            m
        }
    }

    val uygula: (String, String) -> Unit = { anahtar, yeni ->
        val mevcut = degerler
        if (mevcut != null && mevcut[anahtar]?.deger != yeni) {
            val kopya = LinkedHashMap<String, AyarMotoru.Okuma>(mevcut)
            kopya[anahtar] = AyarMotoru.Okuma(null, yeni)
            degerler = kopya
            mesaj = null
            kapsam.launch {
                val h = withContext(Dispatchers.IO) {
                    agKilidi.withLock {
                        AyarMotoru.yaz(ctx, "secure", anahtar, yeni)
                    }
                }
                if (h != null) {
                    mesaj = h
                    yenile++
                } else {
                    mesaj = tamam
                }
            }
        }
    }

    val d = degerler
    val hata = d?.values?.firstNotNullOfOrNull { it.hata }
    val mod = d?.get(AG_MOD)?.deger
    val birim = d?.get(AG_BIRIM)?.deger
    val gizle = d?.get(AG_GIZLE)?.deger
    val modAktif = d != null && hata == null
    val bagliAktif = modAktif && mod != "0"

    RenkKart(1) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.ag42_title), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
            Text(stringResource(R.string.ag42_help), style = MaterialTheme.typography.bodyMedium)
            mesaj?.let { Text(it) }
            if (d == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else if (hata != null) {
                Text(stringResource(R.string.ayar_error, hata))
            } else {
                Text(stringResource(R.string.ag42_mode_label), style = MaterialTheme.typography.labelLarge)
                SecenekGrubu(modlar, mod, modAktif) { uygula(AG_MOD, it) }
                if (mod == null) {
                    Text(ayarsiz, style = MaterialTheme.typography.bodySmall)
                } else if (modlar.none { it.second == mod }) {
                    Text(stringResource(R.string.ag42_custom, mod), style = MaterialTheme.typography.bodySmall)
                }

                Text(stringResource(R.string.ag42_unit_label), style = MaterialTheme.typography.labelLarge)
                SecenekGrubu(birimler, birim, bagliAktif) { uygula(AG_BIRIM, it) }
                if (birim == null) {
                    Text(ayarsiz, style = MaterialTheme.typography.bodySmall)
                } else if (birimler.none { it.second == birim }) {
                    Text(stringResource(R.string.ag42_custom, birim), style = MaterialTheme.typography.bodySmall)
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ag42_autohide))
                        Text(stringResource(R.string.ag42_autohide_help), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = gizle == "1",
                        enabled = bagliAktif,
                        onCheckedChange = { yeni -> uygula(AG_GIZLE, if (yeni) "1" else "0") }
                    )
                }
            }
        }
    }
}
