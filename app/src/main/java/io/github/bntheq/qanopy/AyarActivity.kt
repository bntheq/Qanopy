package io.github.bntheq.qanopy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.unit.dp
import kotlin.concurrent.thread

private fun anaIs(f: () -> Unit) {
    Handler(Looper.getMainLooper()).post { f() }
}

private fun kopyala(ctx: Context, metin: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("qanopy", metin))
    Toast.makeText(ctx, "Kopyalandi", Toast.LENGTH_SHORT).show()
}

class AyarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val renkler = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = renkler) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                        AyarEkran()
                    }
                }
            }
        }
    }
}

@Composable
fun AyarEkran() {
    var sekme by remember { mutableIntStateOf(0) }
    val onbellek = remember { mutableStateMapOf<String, Map<String, String>>() }
    val basliklar = listOf("Liste", "Gecmis", "Fark", "Test")
    TabRow(selectedTabIndex = sekme) {
        basliklar.forEachIndexed { i, b ->
            Tab(selected = sekme == i, onClick = { sekme = i }, text = { Text(b) })
        }
    }
    when (sekme) {
        0 -> AyarListeSekmesi(onbellek)
        1 -> AyarGecmisSekmesi()
        2 -> AyarFarkSekmesi()
        else -> AyarTestSekmesi()
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

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AyarMotoru.tablolar.forEach { t ->
                if (tablo == t) Button(onClick = { tablo = t }) { Text(t) }
                else OutlinedButton(onClick = { tablo = t }) { Text(t) }
            }
        }
        OutlinedTextField(
            value = arama,
            onValueChange = { arama = it },
            label = { Text("Ara (ad veya deger)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { yukle() }) { Text("Yenile") }
            OutlinedButton(onClick = { yeniEkle = true }) { Text("Yeni ayar ekle") }
        }
        if (yukleniyor) Text("Okunuyor...")
        hata?.let { Text("Hata: $it", color = MaterialTheme.colorScheme.error) }
        mesaj?.let { Text(it) }
        val veri = onbellek[tablo] ?: emptyMap()
        val sonuc = remember(veri, arama) {
            if (arama.isBlank()) veri.entries.toList()
            else veri.entries.filter { it.key.contains(arama, true) || it.value.contains(arama, true) }
        }
        Text("${sonuc.size} / ${veri.size} ayar")
        LazyColumn(Modifier.weight(1f)) {
            items(sonuc, key = { it.key }) { e ->
                Column(
                    Modifier.fillMaxWidth().clickable { duzenle = Pair(e.key, e.value) }.padding(vertical = 6.dp)
                ) {
                    Text(e.key, style = MaterialTheme.typography.bodyMedium)
                    Text(e.value.take(150), style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
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

    AlertDialog(
        onDismissRequest = { if (!calisiyor) bitti(null) },
        title = { Text(if (onay) "Emin misin?" else "$tablo ayari") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!onay) {
                    if (yeniAnahtar) {
                        OutlinedTextField(
                            value = anahtar,
                            onValueChange = { anahtar = it },
                            label = { Text("Ad") },
                            singleLine = true
                        )
                    } else {
                        Text(anahtar)
                        Text("Simdiki deger: $eski0")
                    }
                    OutlinedTextField(
                        value = deger,
                        onValueChange = { deger = it },
                        label = { Text("Yeni deger") }
                    )
                    Text("Uyari: yanlis bir ayar sistemi bozabilir. Emin degilsen sadece qanopy_ ile baslayan deneme ayarlari yaz.")
                } else {
                    Text("Tablo: $tablo\nAd: ${anahtar.trim()}\nEski: ${if (yeniAnahtar) "(yok)" else eski0}\nYeni: $deger")
                    Text("Eski deger gunluge yazilacak. Gecmis sekmesinden geri alabilirsin.")
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
                                if (h == null) bitti("Yazildi: $tablo/$a = $d") else hata = h
                            }
                        }
                    }
                }
            ) { Text(if (onay) "Onayla ve yaz" else "Devam") }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !calisiyor,
                onClick = { if (onay) onay = false else bitti(null) }
            ) { Text(if (onay) "Geri" else "Vazgec") }
        }
    )
}

@Composable
fun AyarGecmisSekmesi() {
    val ctx = LocalContext.current
    var liste by remember { mutableStateOf(AyarMotoru.gecmis(ctx)) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Degisiklik gecmisi (en yeni ustte, en cok 100)")
        mesaj?.let { Text(it) }
        if (liste.isEmpty()) Text("Henuz kayit yok.")
        LazyColumn(Modifier.weight(1f)) {
            items(liste) { k ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${k.zaman}  ${k.tablo}/${k.anahtar}", style = MaterialTheme.typography.bodyMedium)
                    Text("${k.eski ?: "(yok)"}  ->  ${k.yeni ?: "(silindi)"}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        enabled = !calisiyor,
                        onClick = {
                            calisiyor = true
                            thread {
                                val h = AyarMotoru.yaz(ctx, k.tablo, k.anahtar, k.eski)
                                anaIs {
                                    mesaj = h ?: "Geri alindi: ${k.tablo}/${k.anahtar}"
                                    liste = AyarMotoru.gecmis(ctx)
                                    calisiyor = false
                                }
                            }
                        }
                    ) { Text("Geri al") }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun AyarFarkSekmesi() {
    val ctx = LocalContext.current
    var bilgi by remember { mutableStateOf(AyarMotoru.onceBilgi(ctx)) }
    var sonuc by remember { mutableStateOf<List<String>>(emptyList()) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("1) 'Once'e bas.  2) Telefon ayarlarinda TEK bir sey degistir.  3) 'Sonra'ya bas.")
        Text(bilgi)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !calisiyor,
                onClick = {
                    calisiyor = true
                    mesaj = "Okunuyor..."
                    thread {
                        val r = AyarMotoru.goruntu()
                        if (r.second != null) {
                            anaIs { mesaj = "Hata: ${r.second}"; calisiyor = false }
                        } else {
                            AyarMotoru.onceKaydet(ctx, r.first)
                            anaIs {
                                bilgi = AyarMotoru.onceBilgi(ctx)
                                mesaj = "Once kaydedildi: ${r.first.size} ayar"
                                calisiyor = false
                            }
                        }
                    }
                }
            ) { Text("Once") }
            Button(
                enabled = !calisiyor,
                onClick = {
                    val once = AyarMotoru.onceOku(ctx)
                    if (once == null) {
                        mesaj = "Once 'Once'e bas."
                    } else {
                        calisiyor = true
                        mesaj = "Okunuyor..."
                        thread {
                            val r = AyarMotoru.goruntu()
                            if (r.second != null) {
                                anaIs { mesaj = "Hata: ${r.second}"; calisiyor = false }
                            } else {
                                val tum = AyarMotoru.fark(once, r.first)
                                val gosterilen = if (tum.size > 300) tum.take(300) + "... ${tum.size - 300} satir daha (kisaltildi)" else tum
                                anaIs {
                                    sonuc = gosterilen
                                    mesaj = if (tum.isEmpty()) "Fark yok" else "${tum.size} fark bulundu"
                                    calisiyor = false
                                }
                            }
                        }
                    }
                }
            ) { Text("Sonra") }
            if (sonuc.isNotEmpty()) {
                OutlinedButton(onClick = { kopyala(ctx, sonuc.joinToString("\n")) }) { Text("Kopyala") }
            }
        }
        mesaj?.let { Text(it) }
        LazyColumn(Modifier.weight(1f)) {
            items(sonuc) { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

@Composable
fun AyarTestSekmesi() {
    val ctx = LocalContext.current
    var sonuc by remember { mutableStateOf<List<String>>(emptyList()) }
    var calisiyor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Her tabloya sahte 'qanopy_test_x2' ayari yazar, geri okur, siler.")
        Button(
            enabled = !calisiyor,
            onClick = {
                calisiyor = true
                sonuc = listOf("Calisiyor...")
                thread {
                    val r = AyarMotoru.yazmaTesti()
                    anaIs { sonuc = r; calisiyor = false }
                }
            }
        ) { Text("Yazma testini calistir") }
        sonuc.forEach { Text(it) }
        if (sonuc.isNotEmpty() && !calisiyor) {
            OutlinedButton(onClick = { kopyala(ctx, sonuc.joinToString("\n")) }) { Text("Kopyala") }
        }
    }
}
