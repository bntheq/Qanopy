@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.bntheq.qanopy

import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

private fun anaIs(f: () -> Unit) {
    Handler(Looper.getMainLooper()).post { f() }
}

private fun kopyala(ctx: Context, metin: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("qanopy", metin))
    Toast.makeText(ctx, ctx.getString(R.string.ayar_copied), Toast.LENGTH_SHORT).show()
}

private fun onceZamani(ctx: Context): String? {
    val f = File(ctx.filesDir, "onceki.txt")
    if (!f.exists()) return null
    return SimpleDateFormat("dd.MM HH:mm:ss", Locale.US).format(Date(f.lastModified()))
}

class AyarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current
            val koyu = isSystemInDarkTheme()
            val renkler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (koyu) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            } else {
                if (koyu) darkColorScheme() else lightColorScheme()
            }
            MaterialTheme(colorScheme = renkler) {
                AyarEkran()
            }
        }
    }
}

@Composable
fun AyarEkran() {
    val onbellek = remember { mutableStateMapOf<String, Map<String, String>>() }
    val basliklar = listOf(
        R.string.ayar_tab_status,
        R.string.ekran31_tab,
        R.string.ayar_tab_list,
        R.string.ayar_tab_history,
        R.string.ayar_tab_diff,
        R.string.ayar_tab_test
    )
    val sayfa = androidx.compose.foundation.pager.rememberPagerState(pageCount = { basliklar.size })
    val kapsam = androidx.compose.runtime.rememberCoroutineScope()
    var ayarAcik by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = ayarAcik) { ayarAcik = false }
    val ust: @Composable () -> Unit = {
        Column {
            QanopyBaslik(0.dp, 0.dp) { ayarAcik = !ayarAcik }
            PikselSekmeSatiri(sayfa.currentPage, basliklar) {
                kapsam.launch { sayfa.animateScrollToPage(it) }
            }
        }
    }
    Scaffold { pad ->
        Column(Modifier.padding(pad).fillMaxSize().navigationBarsPadding()) {
            if (ayarAcik) {
                QanopyBaslik { ayarAcik = !ayarAcik }
                UygulamaAyarlari()
            } else {
                androidx.compose.runtime.CompositionLocalProvider(LocalUst provides ust) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = sayfa,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize()
                    ) { s ->
                        when (s) {
                            0 -> Ekran()
                            1 -> EkranAyarSekmesi()
                            2 -> AyarListeSekmesi(onbellek)
                            3 -> AyarGecmisSekmesi()
                            4 -> AyarFarkSekmesi()
                            else -> AyarTestSekmesi()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AyarListeSekmesi(onbellek: MutableMap<String, Map<String, String>>) {
    var tablo by remember { mutableStateOf("global") }
    var arama by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }
    var duzenle by remember { mutableStateOf<Pair<String, String>?>(null) }
    var yeniEkle by remember { mutableStateOf(false) }
    var mesaj by remember { mutableStateOf<String?>(null) }

    fun yukle() {
        yukleniyor = true
        hata = null
        val t = tablo
        thread {
            val l = AyarMotoru.tabloOku(t)
            onbellek[t] = l.veri
            hata = l.hata
            yukleniyor = false
        }
    }

    LaunchedEffect(tablo) {
        if (onbellek[tablo] == null) yukle()
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UstBolum()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AyarMotoru.tablolar.forEach { t ->
                FilterChip(selected = tablo == t, onClick = { tablo = t }, label = { Text(t) })
            }
        }
        OutlinedTextField(
            value = arama,
            onValueChange = { arama = it },
            label = { Text(stringResource(R.string.ayar_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { yukle() }) { Text(stringResource(R.string.ayar_refresh)) }
            OutlinedButton(onClick = { yeniEkle = true }) { Text(stringResource(R.string.ayar_add)) }
        }
        if (yukleniyor) LinearProgressIndicator(Modifier.fillMaxWidth())
        hata?.let { Text(stringResource(R.string.ayar_error, it), color = MaterialTheme.colorScheme.error) }
        mesaj?.let { Text(it) }
        val veri = onbellek[tablo] ?: emptyMap()
        val sonuc = remember(veri, arama) {
            if (arama.isBlank()) veri.entries.toList()
            else veri.entries.filter { it.key.contains(arama, true) || it.value.contains(arama, true) }
        }
        Text(stringResource(R.string.ayar_count, sonuc.size, veri.size), style = MaterialTheme.typography.labelLarge)
        LazyColumn(Modifier.weight(1f)) {
            items(sonuc, key = { it.key }) { e ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { duzenle = Pair(e.key, e.value) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(e.key, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(e.value.take(150).ifEmpty { "—" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    duzenle?.let { d ->
        AyarDialog(tablo, d.first, d.second, false) { m ->
            duzenle = null
            if (m != null) {
                mesaj = m
                yukle()
            }
        }
    }
    if (yeniEkle) {
        AyarDialog(tablo, "", null, true) { m ->
            yeniEkle = false
            if (m != null) {
                mesaj = m
                yukle()
            }
        }
    }
}

@Composable
fun AyarDialog(tablo: String, anahtar0: String, eski0: String?, yeniAnahtar: Boolean, bitti: (String?) -> Unit) {
    val ctx = LocalContext.current
    var anahtar by remember { mutableStateOf(anahtar0) }
    var deger by remember { mutableStateOf(eski0 ?: "") }
    var onay by remember { mutableStateOf(false) }
    var calisiyor by remember { mutableStateOf(false) }
    var hata by remember { mutableStateOf<String?>(null) }
    val yok = stringResource(R.string.ayar_none)

    AlertDialog(
        shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
        onDismissRequest = { if (!calisiyor) bitti(null) },
        title = {
            Text(if (onay) stringResource(R.string.ayar_sure) else stringResource(R.string.ayar_dialog_title, tablo))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!onay) {
                    if (yeniAnahtar) {
                        OutlinedTextField(
                            value = anahtar,
                            onValueChange = { anahtar = it },
                            label = { Text(stringResource(R.string.ayar_name)) },
                            singleLine = true
                        )
                    } else {
                        Text(anahtar, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.ayar_current, eski0 ?: yok))
                    }
                    OutlinedTextField(
                        value = deger,
                        onValueChange = { deger = it },
                        label = { Text(stringResource(R.string.ayar_new_value)) }
                    )
                    Text(stringResource(R.string.ayar_warning), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        stringResource(
                            R.string.ayar_confirm_body,
                            tablo,
                            anahtar.trim(),
                            if (yeniAnahtar) yok else (eski0 ?: yok),
                            deger
                        )
                    )
                    Text(stringResource(R.string.ayar_journal_note), style = MaterialTheme.typography.bodySmall)
                }
                hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !calisiyor,
                onClick = {
                    if (!onay) {
                        onay = true
                    } else {
                        calisiyor = true
                        hata = null
                        val a = anahtar.trim()
                        val d = deger
                        thread {
                            val h = AyarMotoru.yaz(ctx, tablo, a, d)
                            anaIs {
                                calisiyor = false
                                if (h == null) bitti(ctx.getString(R.string.ayar_written, tablo, a, d)) else hata = h
                            }
                        }
                    }
                }
            ) {
                Text(if (onay) stringResource(R.string.ayar_confirm_write) else stringResource(R.string.ayar_continue))
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !calisiyor,
                onClick = { if (onay) onay = false else bitti(null) }
            ) {
                Text(if (onay) stringResource(R.string.ayar_back) else stringResource(R.string.ayar_cancel))
            }
        }
    )
}

@Composable
fun AyarGecmisSekmesi() {
    val ctx = LocalContext.current
    var liste by remember { mutableStateOf(AyarMotoru.gecmis(ctx)) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }
    var onay by remember { mutableStateOf<AyarMotoru.Kayit?>(null) }
    val yok = stringResource(R.string.ayar_none)
    val silindi = stringResource(R.string.ayar_deleted)
    val kaynakAd = mapOf(
        "qanopy" to stringResource(R.string.ayar22_src_qanopy),
        "geri" to stringResource(R.string.ayar22_src_undo),
        "fark" to stringResource(R.string.ayar22_src_diff)
    )
    val aktifYazi = stringResource(R.string.ayar22_active)
    val geriYazi = stringResource(R.string.ayar22_reverted)
    val yeniVar = stringResource(R.string.ayar22_newer_exists)

    onay?.let { k ->
        AlertDialog(
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            onDismissRequest = { onay = null },
            title = { Text(stringResource(R.string.ayar22_confirm_title)) },
            text = { Text(stringResource(R.string.ayar22_confirm_text, k.tablo + "/" + k.anahtar, k.eski ?: yok)) },
            confirmButton = {
                Button(onClick = {
                    onay = null
                    calisiyor = true
                    thread {
                        val h = AyarMotoru.geriAl(ctx, k)
                        anaIs {
                            mesaj = h ?: ctx.getString(R.string.ayar_undone, k.tablo, k.anahtar)
                            liste = AyarMotoru.gecmis(ctx)
                            calisiyor = false
                        }
                    }
                }) { Text(stringResource(R.string.ayar22_confirm_yes)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { onay = null }) { Text(stringResource(R.string.ayar22_cancel)) }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UstBolum()
        Text(stringResource(R.string.ayar_history_title), style = MaterialTheme.typography.titleSmall)
        mesaj?.let { Text(it) }
        if (calisiyor) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (liste.isEmpty()) Text(stringResource(R.string.ayar_history_empty))
        LazyColumn(Modifier.weight(1f)) {
            items(liste) { k ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${k.zaman}  ${k.tablo}/${k.anahtar}", style = MaterialTheme.typography.titleSmall)
                        Text("${k.eski ?: yok}  →  ${k.yeni ?: silindi}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(kaynakAd[k.kaynak] ?: k.kaynak, style = MaterialTheme.typography.labelMedium)
                            Text(
                                if (k.geriAlindi) geriYazi else aktifYazi,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (k.geriAlindi) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (k.geriAlinabilir) {
                            OutlinedButton(enabled = !calisiyor, onClick = { onay = k }) {
                                Text(stringResource(R.string.ayar_undo))
                            }
                        } else if (!k.geriAlindi && k.kaynak != "geri") {
                            Text(yeniVar, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AyarFarkSekmesi() {
    val ctx = LocalContext.current
    var onceZ by remember { mutableStateOf(onceZamani(ctx)) }
    var sonuc by remember { mutableStateOf<List<AyarMotoru.FarkSatir>>(emptyList()) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }
    var onay by remember { mutableStateOf<AyarMotoru.FarkSatir?>(null) }
    val bitti = remember { mutableStateMapOf<String, Boolean>() }
    val yok = stringResource(R.string.ayar_none)
    val etiketQ = stringResource(R.string.ayar22_tag_qanopy)
    val etiketD = stringResource(R.string.ayar22_tag_external)
    val dondu = stringResource(R.string.ayar22_restored)
    val cokSatirYazi = stringResource(R.string.ayar22_multiline)

    onay?.let { s ->
        AlertDialog(
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            onDismissRequest = { onay = null },
            title = { Text(stringResource(R.string.ayar22_confirm_title)) },
            text = { Text(stringResource(R.string.ayar22_confirm_text, s.tamAd, s.once ?: yok)) },
            confirmButton = {
                Button(onClick = {
                    onay = null
                    calisiyor = true
                    thread {
                        val h = AyarMotoru.farkDondur(ctx, s)
                        anaIs {
                            if (h == null) {
                                bitti[s.tamAd] = true
                                mesaj = ctx.getString(R.string.ayar22_restore_done, s.tamAd)
                            } else {
                                mesaj = h
                            }
                            calisiyor = false
                        }
                    }
                }) { Text(stringResource(R.string.ayar22_confirm_yes)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { onay = null }) { Text(stringResource(R.string.ayar22_cancel)) }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UstBolum()
        Text(stringResource(R.string.ayar_diff_help))
        Text(
            onceZ?.let { stringResource(R.string.ayar_before_saved, it) } ?: stringResource(R.string.ayar_no_before),
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !calisiyor,
                onClick = {
                    calisiyor = true
                    mesaj = ctx.getString(R.string.ayar_reading)
                    thread {
                        val r = AyarMotoru.goruntu()
                        if (r.second != null) {
                            anaIs { mesaj = ctx.getString(R.string.ayar_error, r.second); calisiyor = false }
                        } else {
                            AyarMotoru.onceKaydet(ctx, r.first)
                            anaIs {
                                onceZ = onceZamani(ctx)
                                mesaj = ctx.getString(R.string.ayar_before_done, r.first.size)
                                calisiyor = false
                            }
                        }
                    }
                }
            ) { Text(stringResource(R.string.ayar_before)) }
            Button(
                enabled = !calisiyor,
                onClick = {
                    val once = AyarMotoru.onceOku(ctx)
                    if (once == null) {
                        mesaj = ctx.getString(R.string.ayar_need_before)
                    } else {
                        calisiyor = true
                        mesaj = ctx.getString(R.string.ayar_reading)
                        thread {
                            val r = AyarMotoru.goruntu()
                            if (r.second != null) {
                                anaIs { mesaj = ctx.getString(R.string.ayar_error, r.second); calisiyor = false }
                            } else {
                                val tum = AyarMotoru.farkSatirlari(ctx, once, r.first)
                                val gosterilen = tum.take(300)
                                val ek = if (tum.size > 300) " " + ctx.getString(R.string.ayar_diff_more, tum.size - 300) else ""
                                anaIs {
                                    bitti.clear()
                                    sonuc = gosterilen
                                    mesaj = if (tum.isEmpty()) ctx.getString(R.string.ayar_diff_none)
                                    else ctx.getString(R.string.ayar_diff_found, tum.size) + ek
                                    calisiyor = false
                                }
                            }
                        }
                    }
                }
            ) { Text(stringResource(R.string.ayar_after)) }
            if (sonuc.isNotEmpty()) {
                OutlinedButton(onClick = { kopyala(ctx, sonuc.joinToString("\n") { AyarMotoru.farkMetni(it) }) }) {
                    Text(stringResource(R.string.ayar_copy))
                }
            }
        }
        if (calisiyor) LinearProgressIndicator(Modifier.fillMaxWidth())
        mesaj?.let { Text(it) }
        LazyColumn(Modifier.weight(1f)) {
            items(sonuc) { s ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(AyarMotoru.farkMetni(s), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (s.qanopy) etiketQ else etiketD,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (s.qanopy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                            if (bitti[s.tamAd] == true) {
                                Text(dondu, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (bitti[s.tamAd] != true) {
                            if (AyarMotoru.cokSatirli(s)) {
                                Text(cokSatirYazi, style = MaterialTheme.typography.bodySmall)
                            } else {
                                OutlinedButton(enabled = !calisiyor, onClick = { onay = s }) {
                                    Text(stringResource(R.string.ayar22_restore))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AyarTestSekmesi() {
    val ctx = LocalContext.current
    var sonuc by remember { mutableStateOf<List<String>>(emptyList()) }
    var calisiyor by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UstBolum()
        Text(stringResource(R.string.ayar_test_help))
        Button(
            enabled = !calisiyor,
            onClick = {
                calisiyor = true
                sonuc = listOf(ctx.getString(R.string.ayar_working))
                thread {
                    val r = AyarMotoru.yazmaTesti()
                    anaIs { sonuc = r; calisiyor = false }
                }
            }
        ) { Text(stringResource(R.string.ayar_test_run)) }
        if (calisiyor) LinearProgressIndicator(Modifier.fillMaxWidth())
        sonuc.forEach { Text(it) }
        if (sonuc.isNotEmpty() && !calisiyor) {
            OutlinedButton(onClick = { kopyala(ctx, sonuc.joinToString("\n")) }) {
                Text(stringResource(R.string.ayar_copy))
            }
        }
    }
}
