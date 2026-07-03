package eu.kanade.tachiyomi.ui.shared

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import app.chimahon.shared.ChimahonAndroidHost
import app.chimahon.shared.ChimahonServiceApp
import app.chimahon.shared.ChimahonSharedAppServices
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity

class ChimahonSharedActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChimahonAndroidHost.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val services = remember { ChimahonSharedAppServices() }
            DisposableEffect(services) {
                onDispose {
                    services.close()
                }
            }
            ChimahonServiceApp(services)
        }
    }
}
