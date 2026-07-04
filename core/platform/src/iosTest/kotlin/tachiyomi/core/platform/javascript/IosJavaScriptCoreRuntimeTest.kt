package tachiyomi.core.platform.javascript

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosJavaScriptCoreRuntimeTest {
    private val runtime = IosJavaScriptCoreRuntime()

    @Test
    fun evaluateReturnsPrimitiveResults() = runBlocking {
        assertEquals("chimahon", runtime.evaluate<String>("'chima' + 'hon'"))
        assertEquals(42.0, runtime.evaluate<Double>("40 + 2"))
        assertEquals(true, runtime.evaluate<Boolean>("40 < 42"))
        assertNull(runtime.evaluate<Any?>("undefined"))
    }

    @Test
    fun evaluateUsesIsolatedStandardObjects() = runBlocking {
        assertEquals("undefined", runtime.evaluate<String>("typeof Packages"))
        assertEquals("undefined", runtime.evaluate<String>("typeof require"))
    }

    @Test
    fun evaluateWrapsJavaScriptFailures() {
        val error = assertFailsWith<JavaScriptRuntimeException> {
            runBlocking {
                runtime.evaluate<String>("throw new Error('ios boom')")
            }
        }

        assertTrue(error.message.orEmpty().contains("iOS JavaScriptCore JavaScript evaluation failed"))
        assertTrue(error.message.orEmpty().contains("ios boom"))
    }
}
