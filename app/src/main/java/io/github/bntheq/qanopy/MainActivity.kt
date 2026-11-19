package io.github.bntheq.qanopy

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
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
