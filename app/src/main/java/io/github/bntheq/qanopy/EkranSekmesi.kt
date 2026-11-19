package io.github.bntheq.qanopy

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object EkranMotoru {
    val animAnahtar = listOf(
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale"
    )

    fun animOku(): Pair<Map<String, String?>, String?> {
        val m = LinkedHashMap<String, String?>()
        for (k in animAnahtar) {
            val o = AyarMotoru.hizliOku("global", k)
            if (o.hata != null) return Pair(emptyMap(), o.hata)
            m[k] = o.deger
        }
        return Pair(m, null)
    }

    // ayarlanmamis = sistem varsayilani 1.0
    fun ayniMi(a: String?, b: String): Boolean {
        val x = (a ?: "1.0").toFloatOrNull() ?: return false
        val y = b.toFloatOrNull() ?: return false
        return x == y
    }

    fun animUygula(ctx: Context, deger: String): String? {
        for (k in animAnahtar) {
            val o = AyarMotoru.hizliOku("global", k)
            if (o.hata != null) return o.hata
            if (o.deger != null && ayniMi(o.deger, deger)) continue
            val h = AyarMotoru.yaz(ctx, "global", k, deger)
            if (h != null) return "$k: $h"
        }
        return null
    }
}

@Composable
fun EkranAyarSekmesi() {
    val ctx = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var degerler by remember { mutableStateOf<Map<String, String?>?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }
    var yenile by remember { mutableIntStateOf(0) }

    val secenekler = listOf(
        Pair(stringResource(R.string.ekran31_off), "0"),
        Pair(stringResource(R.string.ekran31_fast), "0.5"),
        Pair(stringResource(R.string.ekran31_normal), "1.0"),
        Pair(stringResource(R.string.ekran31_slow), "1.5")
    )
    val etiketler = listOf(
        Pair("window_animation_scale", stringResource(R.string.ekran31_window)),
        Pair("transition_animation_scale", stringResource(R.string.ekran31_transition)),
        Pair("animator_duration_scale", stringResource(R.string.ekran31_animator))
    )
    val ayarsiz = stringResource(R.string.ekran31_unset)
    val ozelYazi = stringResource(R.string.ekran31_custom)
    val tamamYazi = stringResource(R.string.ekran31_applied)

    LaunchedEffect(yenile) {
        val r = withContext(Dispatchers.IO) { EkranMotoru.animOku() }
        if (r.second != null) {
            hata = r.second
            degerler = null
        } else {
            hata = null
            degerler = r.first
        }
    }

    val d = degerler
    val h = hata
    val eslesen = if (d == null) null else secenekler.firstOrNull { s ->
        EkranMotoru.animAnahtar.all { k -> EkranMotoru.ayniMi(d[k], s.second) }
    }

    val uygula: (String) -> Unit = { deger ->
        calisiyor = true
        mesaj = null
        kapsam.launch {
            val sonuc = withContext(Dispatchers.IO) { EkranMotoru.animUygula(ctx, deger) }
            mesaj = sonuc ?: tamamYazi
            yenile++
            calisiyor = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoyutKarti()
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ekran31_anim_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.ekran31_anim_help), style = MaterialTheme.typography.bodyMedium)
                if (calisiyor) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (h != null) {
                    Text(stringResource(R.string.ayar_error, h))
                } else if (d != null) {
                    Text(
                        stringResource(R.string.ekran31_current, eslesen?.first ?: ozelYazi),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    etiketler.forEach { e ->
                        Text(e.second + ": " + (d[e.first] ?: ayarsiz), style = MaterialTheme.typography.bodySmall)
                    }
                }
                mesaj?.let { Text(it) }
                secenekler.forEach { s ->
                    val secili = eslesen != null && eslesen.second == s.second
                    if (secili) {
                        Button(
                            onClick = { uygula(s.second) },
                            enabled = !calisiyor && d != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(s.first) }
                    } else {
                        OutlinedButton(
                            onClick = { uygula(s.second) },
                            enabled = !calisiyor && d != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(s.first) }
                    }
                }
            }
        }
        YenilemeKarti()
    }
}
