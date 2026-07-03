package app.chimahon.shared

import android.content.Context

object ChimahonAndroidHost {
    @Volatile
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun requireContext(): Context {
        return applicationContext
            ?: error("Call ChimahonAndroidHost.initialize(context) before creating Chimahon shared UI services on Android.")
    }
}
