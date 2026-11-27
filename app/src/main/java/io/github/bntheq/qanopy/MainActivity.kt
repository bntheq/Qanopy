package io.github.bntheq.qanopy
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AyarMotoru.baglam = applicationContext
        Tercih.yukle(applicationContext)
        QaVarlik.baglam = applicationContext
        QaAyar.yukle(applicationContext)
        yuzenBaslat(applicationContext)
        setContent {
            QanopyTema {
                var eski by remember { mutableStateOf(false) }
                if (eski) {
                    BackHandler { eski = false }
                    AyarEkran()
                } else {
                    QaAnaEkran(onEski = { eski = true })
                }
            }
        }
    }
}
