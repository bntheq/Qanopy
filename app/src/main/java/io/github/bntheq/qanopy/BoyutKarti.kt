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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// DPI degisince Android ekrani bastan kurar; bu yuzden geri sayim ve durum
// ekranin disinda, bu nesnede tutulur.
object BoyutDurum {
    var bekliyor by mutableStateOf(false)
    var kalan by mutableIntStateOf(0)
    var calisiyor by mutableStateOf(false)
    var mesaj by mutableStateOf<String?>(null)
    var surum by mutableIntStateOf(0)

    @Volatile var jeton = 0
    @Volatile var eskiDpi: String = "reset"
    @Volatile var eskiFont: String? = null
    @Volatile var dpiDegisti = false
    @Volatile var fontDegisti = false
}

object BoyutMotoru {
    const val SURE = 15
    private const val KLASOR = "/data/adb/qanopy"
    private const val BEKLEYEN = "$KLASOR/ekran_bekleyen"
    private const val JURNAL = "$KLASOR/journal.txt"

    class Yogunluk(val fiziksel: Int?, val ozel: Int?, val hata: String?) {
        val gecerli: Int? get() = ozel ?: fiziksel
    }

    fun dpiOku(): Yogunluk {
        val s = AyarMotoru.kabuk("cmd window density < /dev/null")
        val f = Regex("Physical density: (\\d+)").find(s.cikti)?.groupValues?.get(1)?.toIntOrNull()
        val o = Regex("Override density: (\\d+)").find(s.cikti)?.groupValues?.get(1)?.toIntOrNull()
        if (f == null) return Yogunluk(null, null, "Yoğunluk okunamadı: " + s.cikti.take(200))
        return Yogunluk(f, o, null)
    }

    fun fontAyni(a: String?, b: String): Boolean {
        val x = (a ?: "1.0").toFloatOrNull() ?: return false
        val y = b.toFloatOrNull() ?: return false
        return Math.abs(x - y) < 0.005f
    }

    private fun dpiAyni(d: Yogunluk, hedef: String): Boolean {
        if (hedef == "reset") return d.ozel == null
        return d.gecerli == hedef.toIntOrNull()
    }

    // hedef: "reset" ya da sayi. Ilk eski deger gunluge yazilir (zaten varsa dokunulmaz).
    private fun dpiYaz(hedef: String): String? {
        var sayi = 0
        if (hedef != "reset") {
            sayi = hedef.toIntOrNull() ?: return "Geçersiz yoğunluk"
            if (sayi < 280 || sayi > 520) return "Yoğunluk güvenli aralığın dışında"
        }
        val e = dpiOku()
        if (e.hata != null) return e.hata
        val eski = e.ozel?.toString() ?: "reset"
        val j = AyarMotoru.kabuk("mkdir -p $KLASOR && { grep -q '^ekran|density|' $JURNAL 2>/dev/null || echo 'ekran|density|$eski' >> $JURNAL; }")
        if (j.kod != 0) return "Günlüğe yazılamadı, değişiklik YAPILMADI: " + j.cikti.take(200)
        val w = AyarMotoru.kabuk("cmd window density $hedef < /dev/null")
        Thread.sleep(1200)
        val d = dpiOku()
        if (d.hata != null) return d.hata
        val tamam = if (hedef == "reset") d.ozel == null else (d.ozel == sayi || (d.ozel == null && d.fiziksel == sayi))
        if (!tamam) return "Yoğunluk doğrulanamadı. İstenen: $hedef, okunan: ${d.gecerli}. Çıktı: " + w.cikti.take(150)
        return null
    }

    // Uygulama kapansa/cokse bile eski degere donecek bagimsiz is (guvence)
    private fun kur(): String? {
        val token = System.currentTimeMillis().toString()
        val yol = "$KLASOR/ekran_geri_$token.sh"
        val fontDeger = BoyutDurum.eskiFont?.takeIf { Regex("[0-9]+(\\.[0-9]+)?").matches(it) } ?: "1.0"
        val dpiDeger = BoyutDurum.eskiDpi.takeIf { it == "reset" || Regex("[0-9]{3}").matches(it) } ?: "reset"
        val satirlar = listOf(
            "rm -f $yol",
            "sleep ${SURE + 10}",
            "grep -qx $token $BEKLEYEN 2>/dev/null || exit 0",
            "cmd window density $dpiDeger < /dev/null",
            "content insert --uri content://settings/system --bind name:s:font_scale --bind value:s:$fontDeger < /dev/null",
            "rm -f $BEKLEYEN"
        )
        val metin = satirlar.joinToString(" ") { "'" + it + "'" }
        val komut = "mkdir -p $KLASOR && rm -f $KLASOR/ekran_geri_*.sh && printf '%s\\n' $metin > $yol && echo $token > $BEKLEYEN && { setsid sh $yol </dev/null >/dev/null 2>&1 & }"
        val r = AyarMotoru.kabuk(komut)
        if (r.kod != 0) return "Geri alma güvencesi kurulamadı, değişiklik YAPILMADI: " + r.cikti.take(200)
        return null
    }

    private fun kaldir() {
        AyarMotoru.kabuk("rm -f $BEKLEYEN")
    }

    private fun geriAlIc(c: Context): String? {
        var hata: String? = null
        if (BoyutDurum.fontDegisti) {
            val h = AyarMotoru.yaz(c, "system", "font_scale", BoyutDurum.eskiFont)
            if (h != null) hata = "Yazı boyutu: $h" else BoyutDurum.fontDegisti = false
        }
        if (BoyutDurum.dpiDegisti) {
            val h = dpiYaz(BoyutDurum.eskiDpi)
            if (h != null) {
                hata = (hata?.let { "$it | " } ?: "") + "Ekran boyutu: $h"
            } else {
                BoyutDurum.dpiDegisti = false
            }
        }
        if (hata == null) kaldir()
        return hata
    }

    private fun isle(c: Context, dpi: String?, font: String?): String? {
        val d0 = dpiOku()
        if (d0.hata != null) return d0.hata
        val f0 = AyarMotoru.hizliOku("system", "font_scale")
        if (f0.hata != null) return f0.hata
        val dDeg = dpi != null && !dpiAyni(d0, dpi)
        val fDeg = font != null && !fontAyni(f0.deger, font)
        if (!dDeg && !fDeg) return c.getString(R.string.boyut33_same)

        val ilk = !BoyutDurum.bekliyor
        if (ilk) {
            BoyutDurum.eskiDpi = d0.ozel?.toString() ?: "reset"
            BoyutDurum.eskiFont = f0.deger
            BoyutDurum.dpiDegisti = false
            BoyutDurum.fontDegisti = false
        }
        val k = kur()
        if (k != null) return k

        if (fDeg && font != null) {
            BoyutDurum.fontDegisti = true
            val h = AyarMotoru.yaz(c, "system", "font_scale", font)
            if (h != null) {
                if (ilk) geriAlIc(c)
                return "Yazı boyutu: $h"
            }
        }
        if (dDeg && dpi != null) {
            BoyutDurum.dpiDegisti = true
            val h = dpiYaz(dpi)
            if (h != null) {
                if (ilk) geriAlIc(c)
                return "Ekran boyutu: $h"
            }
        }
        BoyutDurum.kalan = SURE
        BoyutDurum.bekliyor = true
        BoyutDurum.mesaj = c.getString(R.string.boyut33_applied)
        sayacBaslat(c)
        return null
    }

    fun uygula(ctx: Context, dpi: String?, font: String?) {
        val c = ctx.applicationContext
        if (BoyutDurum.calisiyor) return
        BoyutDurum.calisiyor = true
        BoyutDurum.mesaj = null
        Thread {
            val hata: String? = try {
                isle(c, dpi, font)
            } catch (e: Exception) {
                e.message ?: "bilinmeyen hata"
            }
            if (hata != null) BoyutDurum.mesaj = hata
            BoyutDurum.surum = BoyutDurum.surum + 1
            BoyutDurum.calisiyor = false
        }.start()
    }

    private fun sayacBaslat(c: Context) {
        BoyutDurum.jeton = BoyutDurum.jeton + 1
        val benim = BoyutDurum.jeton
        Thread {
            while (BoyutDurum.bekliyor && BoyutDurum.jeton == benim && BoyutDurum.kalan > 0) {
                Thread.sleep(1000)
                if (BoyutDurum.bekliyor && BoyutDurum.jeton == benim) {
                    BoyutDurum.kalan = BoyutDurum.kalan - 1
                }
            }
            if (BoyutDurum.bekliyor && BoyutDurum.jeton == benim) {
                geriDon(c, c.getString(R.string.boyut33_timeout))
            }
        }.start()
    }

    fun geriDon(ctx: Context, basari: String) {
        val c = ctx.applicationContext
        BoyutDurum.jeton = BoyutDurum.jeton + 1
        BoyutDurum.calisiyor = true
        Thread {
            val h: String? = try {
                geriAlIc(c)
            } catch (e: Exception) {
                e.message ?: "bilinmeyen hata"
            }
            BoyutDurum.bekliyor = false
            BoyutDurum.mesaj = h ?: basari
            BoyutDurum.surum = BoyutDurum.surum + 1
            BoyutDurum.calisiyor = false
        }.start()
    }

    fun onayla(ctx: Context) {
        val c = ctx.applicationContext
        BoyutDurum.jeton = BoyutDurum.jeton + 1
        BoyutDurum.calisiyor = true
        Thread {
            val r = AyarMotoru.kabuk("rm -f $BEKLEYEN")
            BoyutDurum.bekliyor = false
            BoyutDurum.mesaj = if (r.kod == 0) c.getString(R.string.boyut33_confirmed) else "Onay kaydedilemedi: " + r.cikti.take(200)
            BoyutDurum.surum = BoyutDurum.surum + 1
            BoyutDurum.calisiyor = false
        }.start()
    }
}

@Composable
fun BoyutKarti() {
    val ctx = LocalContext.current
    var dpi by remember { mutableStateOf<BoyutMotoru.Yogunluk?>(null) }
    var font by remember { mutableStateOf<AyarMotoru.Okuma?>(null) }
    val surum = BoyutDurum.surum
    val bekliyor = BoyutDurum.bekliyor
    val kalan = BoyutDurum.kalan
    val calisiyor = BoyutDurum.calisiyor
    val mesaj = BoyutDurum.mesaj

    val dpiSecenek = listOf(
        Triple(stringResource(R.string.boyut33_d440), "440", 440),
        Triple(stringResource(R.string.boyut33_d400), "reset", 400),
        Triple(stringResource(R.string.boyut33_d360), "360", 360),
        Triple(stringResource(R.string.boyut33_d320), "320", 320)
    )
    val fontSecenek = listOf(
        Triple(stringResource(R.string.boyut33_f085), "0.85", 0.85f),
        Triple(stringResource(R.string.boyut33_f100), "1.0", 1.0f),
        Triple(stringResource(R.string.boyut33_f115), "1.15", 1.15f),
        Triple(stringResource(R.string.boyut33_f130), "1.3", 1.3f)
    )

    LaunchedEffect(surum) {
        dpi = withContext(Dispatchers.IO) { BoyutMotoru.dpiOku() }
        font = withContext(Dispatchers.IO) { AyarMotoru.hizliOku("system", "font_scale") }
    }

    val d = dpi
    val f = font
    val hazir = d != null && d.hata == null && f != null && f.hata == null
    val dpiEsles = if (d == null || d.hata != null) null else dpiSecenek.firstOrNull { it.third == d.gecerli }
    val fontEsles = if (f == null || f.hata != null) null else fontSecenek.firstOrNull { BoyutMotoru.fontAyni(f.deger, it.second) }

    RenkKart(0) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.boyut33_title), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
            Text(stringResource(R.string.boyut33_help), style = MaterialTheme.typography.bodyMedium)
            if (bekliyor) {
                Text(
                    stringResource(R.string.boyut33_confirm, kalan),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                LinearProgressIndicator(
                    progress = { kalan.toFloat() / BoyutMotoru.SURE },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { BoyutMotoru.onayla(ctx) },
                    enabled = !calisiyor,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.boyut33_confirm_btn)) }
                OutlinedButton(
                    onClick = { BoyutMotoru.geriDon(ctx, ctx.getString(R.string.boyut33_reverted)) },
                    enabled = !calisiyor,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.boyut33_revert_btn)) }
            } else if (calisiyor) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (d != null && d.hata != null) {
                Text(stringResource(R.string.ayar_error, d.hata))
            } else if (f != null && f.hata != null) {
                Text(stringResource(R.string.ayar_error, f.hata))
            } else if (d != null && f != null) {
                Text(
                    stringResource(R.string.boyut33_current, d.gecerli ?: 0, f.deger ?: "1.0"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.boyut33_physical, d.fiziksel ?: 0),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            mesaj?.let { Text(it) }

            Text(stringResource(R.string.boyut33_dpi_head), style = MaterialTheme.typography.labelLarge)
            SecenekSatirlari(
                secenekler = dpiSecenek,
                sutun = 2,
                secili = { dpiEsles != null && dpiEsles.first == it.first },
                etiket = { it.first },
                aktif = !calisiyor && hazir,
                sec = { BoyutMotoru.uygula(ctx, it.second, null) }
            )

            Text(stringResource(R.string.boyut33_font_head), style = MaterialTheme.typography.labelLarge)
            SecenekSatirlari(
                secenekler = fontSecenek,
                sutun = 2,
                secili = { fontEsles != null && fontEsles.first == it.first },
                etiket = { it.first },
                aktif = !calisiyor && hazir,
                sec = { BoyutMotoru.uygula(ctx, null, it.second) }
            )
        }
    }
}
