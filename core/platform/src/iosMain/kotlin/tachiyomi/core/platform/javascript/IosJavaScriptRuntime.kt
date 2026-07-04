package tachiyomi.core.platform.javascript

import platform.JavaScriptCore.JSContext
import platform.JavaScriptCore.JSValue

object IosJavaScriptRuntimeFactory : JavaScriptRuntimeFactory {
    override fun create(): JavaScriptRuntime = IosJavaScriptCoreRuntime()
}

class IosJavaScriptCoreRuntime : JavaScriptRuntime {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> evaluate(script: String): T {
        val context = JSContext()
        val value = context.evaluateScript(script)
        val exception = context.exception
        if (exception != null && !exception.isUndefined()) {
            throw JavaScriptRuntimeException(RUNTIME_NAME, exception.toString())
        }
        return value.toKotlinValue() as T
    }

    private fun JSValue?.toKotlinValue(): Any? {
        if (this == null || isUndefined() || isNull()) return null
        return when {
            isBoolean() -> toBool()
            isNumber() -> toDouble()
            isString() -> toString()
            else -> toObject()
        }
    }

    private companion object {
        const val RUNTIME_NAME = "iOS JavaScriptCore"
    }
}
