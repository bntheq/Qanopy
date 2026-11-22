package io.github.bntheq.qanopy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AyarMotoru.baglam = applicationContext
        Tercih.yukle(applicationContext)
        setContent {
            QanopyTema {
                AyarEkran()
            }
        }
    }
}
