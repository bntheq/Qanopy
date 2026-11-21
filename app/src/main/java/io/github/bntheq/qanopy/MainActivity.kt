package io.github.bntheq.qanopy

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AyarMotoru.baglam = applicationContext
        setContent {
            val ctx = LocalContext.current
            val koyu = isSystemInDarkTheme()
            val renkler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (koyu) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            } else {
                if (koyu) darkColorScheme() else lightColorScheme()
            }
            MaterialTheme(colorScheme = renkler, shapes = PikselSekiller, typography = PikselYazi) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                    AyarEkran()
                }
            }
        }
    }
}
