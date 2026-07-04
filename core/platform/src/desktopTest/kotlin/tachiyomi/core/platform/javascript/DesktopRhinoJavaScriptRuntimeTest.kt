package tachiyomi.core.platform.javascript

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DesktopRhinoJavaScriptRuntimeTest {
    private val runtime = DesktopRhinoJavaScriptRuntime()

    @Test
    fun evaluateReturnsPrimitiveResult() = runBlocking {
        assertEquals("chimahon", runtime.evaluate<String>("'chima' + 'hon'"))
        assertEquals(42.0, runtime.evaluate<Double>("40 + 2"))
    }

    @Test
    fun evaluateUsesSafeStandardObjects() = runBlocking {
        assertEquals("undefined", runtime.evaluate<String>("typeof Packages"))
    }

    @Test
    fun evaluateWrapsJavaScriptFailures() {
        val error = assertThrows(JavaScriptRuntimeException::class.java) {
            runBlocking {
                runtime.evaluate<String>("throw new Error('boom')")
            }
        }

        assertTrue(error.message.orEmpty().contains("Desktop Rhino JavaScript evaluation failed"))
        assertTrue(error.message.orEmpty().contains("boom"))
    }
}
