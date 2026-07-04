package tachiyomi.core.platform.javascript

import app.cash.quickjs.QuickJs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AndroidJavaScriptRuntimeFactory : JavaScriptRuntimeFactory {
    override fun create(): JavaScriptRuntime = AndroidQuickJsRuntime()
}

class AndroidQuickJsRuntime : JavaScriptRuntime {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> evaluate(script: String): T = withContext(Dispatchers.IO) {
        val result = try {
            QuickJs.create().use {
                it.evaluate(script)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw JavaScriptRuntimeException(RUNTIME_NAME, error.message ?: error.toString(), error)
        }
        result as T
    }

    private companion object {
        const val RUNTIME_NAME = "Android QuickJS"
    }
}
