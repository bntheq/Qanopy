package io.github.bntheq.qanopy

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object YenilemeMotoru {
    private const val TEPE = "peak_refresh_rate"
    private const val EN_AZ = "min_refresh_rate"

    class Deger(val enAz: String?, val tepe: String?, val hata: String?)
    class Aralik(val enAz: Float, val tepe: Float)

    fun oku(): Deger {
        val m = AyarMotoru.hizliOku("system", EN_AZ)
        if (m.hata != null) return Deger(null, null, m.hata)
        val t = AyarMotoru.hizliOku("system", TEPE)
        if (t.hata != null) return Deger(null, null, t.hata)
        return Deger(m.deger, t.deger, null)
    }

    fun ayni(a: String?, b: Float): Boolean {
        val x = a?.toFloatOrNull() ?: return false
        return Math.abs(x - b) < 0.01f
    }

    fun sistemAraligi(): Aralik? {
        val s = AyarMotoru.kabuk("dumpsys display")
        val m = Regex("primary=physical: \\(([0-9.]+) ([0-9.]+)\\)").find(s.cikti) ?: return null
        val a = m.groupValues[1].toFloatOrNull() ?: return null
        val b = m.groupValues[2].toFloatOrNull() ?: return null
        return Aralik(a, b)
    }

    // En az degeri en yuksekten buyuk olmasin diye yazma sirasi hesaplanir
    fun uygula(ctx: Context, enAz: Float, tepe: Float): String? {
        val o = oku()
        if (o.hata != null) return o.hata
        val eskiTepe = o.tepe?.toFloatOrNull() ?: 9999f
        val sEnAz = enAz.toString()
        val sTepe = tepe.toString()
        val sira = if (enAz > eskiTepe) {
            listOf(Pair(TEPE, sTepe), Pair(EN_AZ, sEnAz))
        } else {
            listOf(Pair(EN_AZ, sEnAz), Pair(TEPE, sTepe))
        }
        for (p in sira) {
            val simdi = AyarMotoru.hizliOku("system", p.first)
            if (simdi.hata != null) return simdi.hata
            if (ayni(simdi.deger, p.second.toFloat())) continue
            val h = AyarMotoru.yaz(ctx, "system", p.first, p.second)
            if (h != null) return p.first + ": " + h
        }
        Thread.sleep(1500)
        return null
    }
}

@Composable
fun YenilemeKarti() {
    val ctx = LocalContext.current
    val kapsam = rememberCoroutineScope()
    var deger by remember { mutableStateOf<YenilemeMotoru.Deger?>(null) }
    var rapor by remember { mutableStateOf<YenilemeMotoru.Aralik?>(null) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }
    var yenile by remember { mutableIntStateOf(0) }

    val secenekler = listOf(
        Triple(stringResource(R.string.yenileme32_auto), 60f, 120f),
        Triple(stringResource(R.string.yenileme32_60), 60f, 60f),
        Triple(stringResource(R.string.yenileme32_90), 90f, 90f),
        Triple(stringResource(R.string.yenileme32_120), 120f, 120f)
    )
    val ozel = stringResource(R.string.yenileme32_custom)
    val ayarsiz = stringResource(R.string.yenileme32_unset)
    val bilinmiyor = stringResource(R.string.yenileme32_unknown)
    val tamam = stringResource(R.string.yenileme32_ok)

    LaunchedEffect(yenile) {
        deger = withContext(Dispatchers.IO) { YenilemeMotoru.oku() }
        rapor = withContext(Dispatchers.IO) { YenilemeMotoru.sistemAraligi() }
    }

    val d = deger
    val eslesen = if (d == null || d.hata != null) null else secenekler.firstOrNull {
        YenilemeMotoru.ayni(d.enAz, it.second) && YenilemeMotoru.ayni(d.tepe, it.third)
    }

    val uygula: (Float, Float) -> Unit = { a, b ->
        calisiyor = true
        mesaj = null
        kapsam.launch {
            val h = withContext(Dispatchers.IO) { YenilemeMotoru.uygula(ctx, a, b) }
            if (h != null) {
                mesaj = h
            } else {
                val r = withContext(Dispatchers.IO) { YenilemeMotoru.sistemAraligi() }
                mesaj = if (r == null) {
                    ctx.getString(R.string.yenileme32_noreport)
                } else if (Math.abs(r.enAz - a) < 0.5f && Math.abs(r.tepe - b) < 0.5f) {
                    tamam
                } else {
                    ctx.getString(R.string.yenileme32_diff, r.enAz.toString() + " - " + r.tepe.toString())
                }
            }
            yenile++
            calisiyor = false
        }
    }

    RenkKart(2) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.yenileme32_title), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
            Text(stringResource(R.string.yenileme32_help), style = MaterialTheme.typography.bodyMedium)
            if (calisiyor) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (d != null && d.hata != null) {
                Text(stringResource(R.string.ayar_error, d.hata))
            } else if (d != null) {
                Text(
                    stringResource(R.string.ekran31_current, eslesen?.first ?: ozel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.yenileme32_values, d.enAz ?: ayarsiz, d.tepe ?: ayarsiz),
                    style = MaterialTheme.typography.bodySmall
                )
                val r = rapor
                Text(
                    stringResource(
                        R.string.yenileme32_report,
                        if (r == null) bilinmiyor else r.enAz.toString() + " - " + r.tepe.toString()
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            mesaj?.let { Text(it) }
            secenekler.forEach { s ->
                val secili = eslesen != null && eslesen.first == s.first
                if (secili) {
                    Button(
                        onClick = { uygula(s.second, s.third) },
                        enabled = !calisiyor && d != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(s.first) }
                } else {
                    OutlinedButton(
                        onClick = { uygula(s.second, s.third) },
                        enabled = !calisiyor && d != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(s.first) }
                }
            }
        }
    }
}
