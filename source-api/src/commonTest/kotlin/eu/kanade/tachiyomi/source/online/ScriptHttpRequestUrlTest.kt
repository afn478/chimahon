package eu.kanade.tachiyomi.source.online

import tachiyomi.core.extensions.ScriptHttpRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScriptHttpRequestUrlTest {
    @Test
    fun keepsAbsoluteRequestUrls() {
        val request = ScriptHttpRequest(url = "https://cdn.example/image.jpg")

        assertEquals(request, request.resolveAgainst("https://source.example/root"))
        assertEquals(
            "https://cdn.example/image.jpg",
            resolveScriptSourceUrl("https://source.example/root", "https://cdn.example/image.jpg"),
        )
    }

    @Test
    fun keepsBlankRequestUrlsInvalid() {
        val request = ScriptHttpRequest(url = "   ")

        assertEquals("", request.resolveAgainst("https://source.example/root").url)
        assertNull(resolveScriptSourceUrl("https://source.example/root", "   "))
    }

    @Test
    fun resolvesProtocolRelativeRequestUrls() {
        val request = ScriptHttpRequest(url = "//cdn.example/image.jpg")

        assertEquals(
            "https://cdn.example/image.jpg",
            request.resolveAgainst("https://source.example/root").url,
        )
        assertEquals(
            "https://cdn.example/image.jpg",
            resolveScriptSourceUrl("https://source.example/root", "//cdn.example/image.jpg"),
        )
    }

    @Test
    fun resolvesRootRelativeRequestUrlsAgainstOrigin() {
        val request = ScriptHttpRequest(url = "/popular?page=2")

        assertEquals(
            "https://source.example/popular?page=2",
            request.resolveAgainst("https://source.example/catalog").url,
        )
        assertEquals(
            "https://source.example/popular?page=2",
            resolveScriptSourceUrl("https://source.example/catalog", "/popular?page=2"),
        )
    }

    @Test
    fun resolvesRelativeRequestUrlsAgainstSourceBaseUrl() {
        val request = ScriptHttpRequest(url = "chapter/1#page")

        assertEquals(
            "https://source.example/catalog/chapter/1#page",
            request.resolveAgainst("https://source.example/catalog").url,
        )
        assertEquals(
            "https://source.example/catalog/chapter/1#page",
            resolveScriptSourceUrl("https://source.example/catalog", "chapter/1#page"),
        )
    }
}
