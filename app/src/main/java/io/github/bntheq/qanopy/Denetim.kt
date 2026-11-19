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

object Denetim {
    const val HEDEF_CIHAZ = "surya"

    private fun varMi(yol: String, bayrak: String = "f"): Boolean {
        return Root.calistir("test -" + bayrak + " " + yol + " && echo evet || echo hayir").cikti.trim() == "evet"
    }

    fun kontrolEt(): Durum {
        val cihaz = Build.DEVICE ?: "?"
        if (cihaz != HEDEF_CIHAZ) {
            return Durum(cihaz, false, null, null, null, null, null, null, null, null, "cihaz kilidi")
        }
        val id = Root.calistir("id")
        val rootVar = id.basarili && id.cikti.contains("uid=0")
        if (!rootVar) {
            return Durum(cihaz, true, false, null, null, null, null, null, null, null, "su: " + id.cikti)
        }
        val mv = Root.calistir("magisk -v")
        val modulKurulu = varMi("/data/adb/modules/qanopy_core", "d")
        var surum: String? = null
        var kapali: Boolean? = null
        if (modulKurulu) {
            val s = Root.calistir("grep '^version=' /data/adb/modules/qanopy_core/module.prop | cut -d= -f2")
            surum = if (s.basarili && s.cikti.isNotEmpty()) s.cikti else null
            kapali = varMi("/data/adb/modules/qanopy_core/disable")
        }
        val guvenli = varMi("/data/adb/qanopy/safe_mode")
        val sayac = Root.calistir("cat /data/adb/qanopy/boot_count 2>/dev/null || echo ?").cikti.trim()
        val log = Root.calistir("tail -n 40 /data/adb/qanopy/boot.log 2>/dev/null").cikti
        return Durum(
            cihaz,
            true,
            true,
            if (mv.basarili && mv.cikti.isNotEmpty()) mv.cikti else null,
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
