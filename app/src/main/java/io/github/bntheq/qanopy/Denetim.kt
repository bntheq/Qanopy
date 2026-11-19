package io.github.bntheq.qanopy

import android.os.Build
import java.util.concurrent.TimeUnit

object Root {
    class Sonuc(val basarili: Boolean, val cikti: String)

    fun calistir(komut: String, saniye: Long = 15): Sonuc {
        return try {
            val p = ProcessBuilder("su", "-c", komut).redirectErrorStream(true).start()
            if (!p.waitFor(saniye, TimeUnit.SECONDS)) {
                p.destroy()
                Sonuc(false, "zaman asimi")
            } else {
                val cikti = p.inputStream.bufferedReader().readText().trim()
                Sonuc(p.exitValue() == 0, cikti)
            }
        } catch (e: Exception) {
            Sonuc(false, e.javaClass.simpleName + ": " + (e.message ?: ""))
        }
    }
}

data class Durum(
    val cihaz: String,
    val cihazUygun: Boolean,
    val rootVar: Boolean?,
    val magisk: String?,
    val modulKurulu: Boolean?,
    val modulSurum: String?,
    val modulKapali: Boolean?,
    val guvenliMod: Boolean?,
    val sayac: String?,
    val log: String?,
    val ayrinti: String
)

object DurumOnbellek {
    @Volatile
    var son: Durum? = null
}

object Denetim {
    const val HEDEF_CIHAZ = "surya"

    private const val BETIK = """id
echo '@@MAGISK'
magisk -v 2>/dev/null
echo '@@MODUL'
test -d /data/adb/modules/qanopy_core && echo evet || echo hayir
echo '@@SURUM'
grep '^version=' /data/adb/modules/qanopy_core/module.prop 2>/dev/null | cut -d= -f2
echo '@@KAPALI'
test -f /data/adb/modules/qanopy_core/disable && echo evet || echo hayir
echo '@@GUVENLI'
test -f /data/adb/qanopy/safe_mode && echo evet || echo hayir
echo '@@SAYAC'
cat /data/adb/qanopy/boot_count 2>/dev/null || echo ?
echo '@@LOG'
tail -n 40 /data/adb/qanopy/boot.log 2>/dev/null
echo '@@SON'
"""

    private fun bol(cikti: String): Map<String, String> {
        val b = HashMap<String, String>()
        var anahtar = "id"
        val sb = StringBuilder()
        for (satir in cikti.lines()) {
            if (satir.startsWith("@@")) {
                b[anahtar] = sb.toString().trim()
                anahtar = satir.substring(2).trim()
                sb.setLength(0)
            } else {
                sb.append(satir).append('\n')
            }
        }
        b[anahtar] = sb.toString().trim()
        return b
    }

    fun kontrolEt(): Durum {
        val cihaz = Build.DEVICE ?: "?"
        if (cihaz != HEDEF_CIHAZ) {
            return Durum(cihaz, false, null, null, null, null, null, null, null, null, "cihaz kilidi")
        }
        val r = Root.calistir(BETIK)
        val b = bol(r.cikti)
        val idc = b["id"] ?: ""
        if (!idc.contains("uid=0")) {
            return Durum(cihaz, true, false, null, null, null, null, null, null, null, "su: " + r.cikti.take(200))
        }
        val mv = b["MAGISK"].orEmpty()
        val modulKurulu = b["MODUL"] == "evet"
        val surumHam = b["SURUM"].orEmpty()
        val surum = if (modulKurulu && surumHam.isNotEmpty()) surumHam else null
        val kapali: Boolean? = if (modulKurulu) b["KAPALI"] == "evet" else null
        val guvenli = b["GUVENLI"] == "evet"
        val sayac = b["SAYAC"].orEmpty().ifEmpty { "?" }
        val log = b["LOG"].orEmpty()
        return Durum(
            cihaz,
            true,
            true,
            if (mv.isNotEmpty()) mv else null,
            modulKurulu,
            surum,
            kapali,
            guvenli,
            sayac,
            log,
            "ok"
        )
    }

    fun guvenliModdanCik(): Boolean {
        return Root.calistir("rm -f /data/adb/qanopy/safe_mode").basarili
    }
}
