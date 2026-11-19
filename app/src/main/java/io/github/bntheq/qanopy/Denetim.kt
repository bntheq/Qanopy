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
    val ayrinti: String
)

object Denetim {
    const val HEDEF_CIHAZ = "surya"

    fun kontrolEt(): Durum {
        val cihaz = Build.DEVICE ?: "?"
        if (cihaz != HEDEF_CIHAZ) {
            return Durum(cihaz, false, null, null, null, "cihaz kilidi")
        }
        val id = Root.calistir("id")
        val rootVar = id.basarili && id.cikti.contains("uid=0")
        if (!rootVar) {
            return Durum(cihaz, true, false, null, null, "su: " + id.cikti)
        }
        val mv = Root.calistir("magisk -v")
        val modul = Root.calistir("test -d /data/adb/modules/qanopy_core && echo evet || echo hayir")
        return Durum(
            cihaz,
            true,
            true,
            if (mv.basarili && mv.cikti.isNotEmpty()) mv.cikti else null,
            modul.cikti.trim() == "evet",
            "ok"
        )
    }
}
