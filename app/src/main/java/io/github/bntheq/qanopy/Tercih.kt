package io.github.bntheq.qanopy

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Tercih {
    private const val DOSYA = "qanopy_tercih"
    var tema by mutableIntStateOf(0)
        private set
    var dinamikRenk by mutableStateOf(true)
        private set
    var pikselYazi by mutableStateOf(true)
        private set
    private var yuklendi = false

    fun yukle(ctx: Context) {
        if (yuklendi) return
        val p = ctx.getSharedPreferences(DOSYA, Context.MODE_PRIVATE)
        tema = p.getInt("tema", 0)
        dinamikRenk = p.getBoolean("dinamik", true)
        pikselYazi = p.getBoolean("piksel_yazi", true)
        yuklendi = true
    }

    fun temaSec(ctx: Context, v: Int) {
        tema = v
        ctx.getSharedPreferences(DOSYA, Context.MODE_PRIVATE).edit().putInt("tema", v).apply()
    }

    fun renkSec(ctx: Context, v: Boolean) {
        dinamikRenk = v
        ctx.getSharedPreferences(DOSYA, Context.MODE_PRIVATE).edit().putBoolean("dinamik", v).apply()
    }

    fun yaziSec(ctx: Context, piksel: Boolean) {
        pikselYazi = piksel
        ctx.getSharedPreferences(DOSYA, Context.MODE_PRIVATE).edit().putBoolean("piksel_yazi", piksel).apply()
    }
}
