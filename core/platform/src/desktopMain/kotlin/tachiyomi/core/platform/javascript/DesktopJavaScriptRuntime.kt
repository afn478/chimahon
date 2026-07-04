package tachiyomi.core.platform.javascript

import kotlinx.coroutines.CancellationException
import org.mozilla.javascript.Context
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

object DesktopJavaScriptRuntimeFactory : JavaScriptRuntimeFactory {
    override fun create(): JavaScriptRuntime = DesktopRhinoJavaScriptRuntime()
}

class DesktopRhinoJavaScriptRuntime : JavaScriptRuntime {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> evaluate(script: String): T {
        val context = Context.enter()
        return try {
            val scope = context.initSafeStandardObjects()
            val result = context.evaluateString(scope, script, SOURCE_NAME, 1, null)
            when (result) {
                Undefined.instance -> Unit
                is ScriptableObject -> Context.jsToJava(result, Any::class.java)
                else -> result
            } as T
        } catch (error: CancellationException) {
            throw error
        } catch (error: RhinoException) {
            throw JavaScriptRuntimeException(RUNTIME_NAME, error.details(), error)
        } finally {
            Context.exit()
        }
    }

    private companion object {
        const val RUNTIME_NAME = "Desktop Rhino"
        const val SOURCE_NAME = "chimahon-desktop"
    }
}
