package io.github.bntheq.qanopy

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AyarMotoru.baglam = applicationContext
        Tercih.yukle(applicationContext)
        setContent {
            val ctx = LocalContext.current
            val sistemKoyu = isSystemInDarkTheme()
            val koyu = when (Tercih.tema) {
                1 -> false
                2 -> true
                else -> sistemKoyu
            }
            val renkler = if (Tercih.dinamikRenk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (koyu) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            } else {
                if (koyu) darkColorScheme() else lightColorScheme()
            }
            MaterialExpressiveTheme(
                colorScheme = renkler,
                motionScheme = MotionScheme.expressive(),
                shapes = PikselSekiller,
                typography = if (Tercih.pikselYazi) PikselYazi else Typography()
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    AyarEkran()
                }
            }
        }
    }
}
