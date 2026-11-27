package io.github.bntheq.qanopy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import java.util.concurrent.TimeUnit

object QaPil {
    var satirlar by mutableStateOf<List<String>>(listOf("OKUNUYOR"))

    @Volatile
    private var aktif = false

    @Volatile
    private var nesil = 0

    fun baslat() {
        nesil++
        val benim = nesil
        aktif = true
        satirlar = listOf("OKUNUYOR")
        Thread {
            var tur = 0
            while (aktif && nesil == benim && tur < 600) {
                val s = oku()
                if (aktif && nesil == benim) satirlar = s
                tur++
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                }
            }
        }.start()
    }

    fun durdur() {
        aktif = false
    }

    private fun ondalik(deger: Long, basamak: Int): String {
        val k = if (basamak == 2) 100L else 10L
        val tam = deger / k
        val kalan = Math.abs(deger % k)
        return tam.toString() + "." + kalan.toString().padStart(basamak, '0')
    }

    private fun isi(s: String?): String {
        val d = s?.toLongOrNull() ?: return "---"
        return ondalik(d, 1) + " C"
    }

    private fun oku(): List<String> {
        val v = HashMap<String, String>()
        try {
            val komut = "cd /sys/class/power_supply/battery && for f in status capacity current_now voltage_now temp charger_temp cycle_count charge_full charge_full_design; do echo \"\$f=\$(cat \$f 2>/dev/null)\"; done"
            val p = ProcessBuilder("su", "-c", komut).redirectErrorStream(true).start()
            val cikti = p.inputStream.bufferedReader().readText()
            p.waitFor(5, TimeUnit.SECONDS)
            for (satir in cikti.lines()) {
                val i = satir.indexOf('=')
                if (i > 0) v[satir.substring(0, i).trim()] = satir.substring(i + 1).trim()
            }
        } catch (e: Exception) {
        }
        if (v.isEmpty()) return listOf("OKUNAMADI", "ROOT VAR MI")
        val akim = Math.abs(v["current_now"]?.toLongOrNull() ?: 0L)
        val volt = v["voltage_now"]?.toLongOrNull() ?: 0L
        val mw = (volt / 1000L) * (akim / 1000L) / 1000L
        val cw = mw / 10L
        val durum = when (v["status"]) {
            "Charging" -> "ŞARJ OLUYOR"
            "Discharging" -> "BOŞALIYOR"
            "Full" -> "DOLU"
            "Not charging" -> "ŞARJ YOK"
            else -> (v["status"] ?: "---").uppercase()
        }
        val tam = v["charge_full"]?.toLongOrNull() ?: 0L
        val tasarim = v["charge_full_design"]?.toLongOrNull() ?: 0L
        val saglik = if (tasarim > 0L) ondalik(tam * 1000L / tasarim, 1) + "%" else "---"
        return listOf(
            "DURUM: " + durum,
            "SEVİYE: " + (v["capacity"] ?: "---") + "%",
            "GÜÇ: " + ondalik(cw, 2) + " W",
            "AKIM: " + (akim / 1000L).toString() + " mA",
            "VOLT: " + ondalik(volt / 10000L, 2) + " V",
            "PİL ISISI: " + isi(v["temp"]),
            "ŞARJ ISISI: " + isi(v["charger_temp"]),
            "SAĞLIK: " + saglik,
            "DÖNGÜ: " + (v["cycle_count"] ?: "---")
        )
    }
}

fun DrawScope.qaPilCiz(wx: Int, wy: Int, xk: Int, yk: Int, o: Int, renk: Color) {
    qaYaziOrta("PİL PANELİ", xk + (wx + 120) * o, yk + (wy + 12) * o, o, renk)
    val s = QaPil.satirlar
    for (i in s.indices) {
        qaYazi(s[i], xk + (wx + 12) * o, yk + (wy + 40 + i * 20) * o, o, renk)
    }
}
