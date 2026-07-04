package tachiyomi.core.platform.javascript

interface JavaScriptRuntime {
    suspend fun <T> evaluate(script: String): T
}

class JavaScriptRuntimeException(
    runtimeName: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException("$runtimeName JavaScript evaluation failed: $message", cause)

fun interface JavaScriptRuntimeFactory {
    fun create(): JavaScriptRuntime
}

class UnsupportedJavaScriptRuntime(
    private val platformName: String,
) : JavaScriptRuntime {
    override suspend fun <T> evaluate(script: String): T {
        throw JavaScriptRuntimeException(platformName, "JavaScript execution is not implemented yet")
    }
}
