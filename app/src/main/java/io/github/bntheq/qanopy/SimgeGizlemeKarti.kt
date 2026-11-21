package io.github.bntheq.qanopy

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private val simgeKilidi = Mutex()
private const val SIMGE_ANAHTAR = "icon_blacklist"

private class SimgeOge(val ad: String, @StringRes val etiket: Int, val kritik: Boolean = false)
private class SimgeGrup(@StringRes val baslik: Int, val ogeler: List<SimgeOge>)

private val simgeGruplari = listOf(
    SimgeGrup(
        R.string.simge43_group_conn,
        listOf(
            SimgeOge("bluetooth", R.string.simge43_bluetooth),
            SimgeOge("nfc", R.string.simge43_nfc),
            SimgeOge("hotspot", R.string.simge43_hotspot),
            SimgeOge("cast", R.string.simge43_cast),
            SimgeOge("ethernet", R.string.simge43_ethernet),
            SimgeOge("data_saver", R.string.simge43_data_saver),
            SimgeOge("airplane", R.string.simge43_airplane),
            SimgeOge("roaming", R.string.simge43_roaming),
            SimgeOge("call_strength", R.string.simge43_call_strength),
            SimgeOge("vpn", R.string.simge43_vpn),
            SimgeOge("vowifi", R.string.simge43_vowifi),
            SimgeOge("volte", R.string.simge43_volte),
            SimgeOge("vonr", R.string.simge43_vonr)
        )
    ),
    SimgeGrup(
        R.string.simge43_group_sound,
        listOf(
            SimgeOge("volume", R.string.simge43_volume),
            SimgeOge("mute", R.string.simge43_mute),
            SimgeOge("zen", R.string.simge43_zen),
            SimgeOge("headset", R.string.simge43_headset)
        )
    ),
    SimgeGrup(
        R.string.simge43_group_other,
        listOf(
            SimgeOge("alarm_clock", R.string.simge43_alarm_clock),
            SimgeOge("managed_profile", R.string.simge43_managed_profile),
            SimgeOge("firewall", R.string.simge43_firewall),
            SimgeOge("rotate", R.string.simge43_rotate)
        )
    ),
    SimgeGrup(
        R.string.simge43_group_critical,
        listOf(
            SimgeOge("wifi", R.string.simge43_wifi, true),
            SimgeOge("battery", R.string.simge43_battery, true),
            SimgeOge("clock", R.string.simge43_clock, true),
            SimgeOge("mobile", R.string.simge43_mobile, true)
        )
    )
)

private val bilinenAdlar: Set<String> = simgeGruplari.flatMap { g -> g.ogeler }.map { it.ad }.toSet()

private fun listeAyir(s: String?): List<String> =
    (s ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }

@Composable
fun SimgeGizlemeKarti() {
    val ctx = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var okuma by remember { mutableStateOf<AyarMotoru.Okuma?>(null) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var yenile by remember { mutableIntStateOf(0) }
    var onay by remember { mutableStateOf<SimgeOge?>(null) }
    val tamam = stringResource(R.string.simge43_applied)

    LaunchedEffect(yenile) {
        okuma = withContext(Dispatchers.IO) { AyarMotoru.tekOku("secure", SIMGE_ANAHTAR) }
    }

    val degistir: (String, Boolean) -> Unit = { ad, gizle ->
        mesaj = null
        val simdi = okuma
        if (simdi != null && simdi.hata == null) {
            val l = listeAyir(simdi.deger)
            val y = if (gizle) (if (ad in l) l else l + ad) else l.filter { it != ad }
            okuma = AyarMotoru.Okuma(null, y.joinToString(","))
        }
        kapsam.launch {
            val h = withContext(Dispatchers.IO) {
                simgeKilidi.withLock {
                    val o = AyarMotoru.tekOku("secure", SIMGE_ANAHTAR)
                    if (o.hata != null) {
                        o.hata
                    } else {
                        val l = listeAyir(o.deger)
                        val y = if (gizle) (if (ad in l) l else l + ad) else l.filter { it != ad }
                        if (y == l) null else AyarMotoru.yaz(ctx, "secure", SIMGE_ANAHTAR, y.joinToString(","))
                    }
                }
            }
            mesaj = h ?: tamam
            yenile++
        }
    }

    val bekleyen = onay
    if (bekleyen != null) {
        AlertDialog(
            onDismissRequest = { onay = null },
            title = { Text(stringResource(R.string.simge43_confirm_title)) },
            text = { Text(stringResource(R.string.simge43_confirm_text, stringResource(bekleyen.etiket))) },
            confirmButton = {
                TextButton(onClick = {
                    onay = null
                    degistir(bekleyen.ad, true)
                }) { Text(stringResource(R.string.simge43_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { onay = null }) { Text(stringResource(R.string.simge43_confirm_no)) }
            }
        )
    }

    val o = okuma
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.simge43_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.simge43_help), style = MaterialTheme.typography.bodyMedium)
            mesaj?.let { Text(it) }
            if (o == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                val hata = o.hata
                if (hata != null) {
                    Text(stringResource(R.string.ayar_error, hata))
                } else {
                    val liste = listeAyir(o.deger)
                    simgeGruplari.forEach { g ->
                        Text(
                            stringResource(g.baslik),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        g.ogeler.forEach { oge ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(oge.etiket))
                                    Text(oge.ad, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = oge.ad !in liste,
                                    onCheckedChange = { acik ->
                                        if (!acik && oge.kritik) onay = oge else degistir(oge.ad, !acik)
                                    }
                                )
                            }
                        }
                    }
                    val bilinmeyen = liste.filter { it !in bilinenAdlar }
                    if (bilinmeyen.isNotEmpty()) {
                        Text(
                            stringResource(R.string.simge43_unknown, bilinmeyen.joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
