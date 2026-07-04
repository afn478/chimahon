package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelEpubContentPolicyTest {
    @Test
    fun extensionPredicatesMatchImporterContentTypesIgnoringCase() {
        assertTrue(NovelEpubContentPolicy.isImageExtension("JPG"))
        assertTrue(NovelEpubContentPolicy.isImageExtension("jpeg"))
        assertTrue(NovelEpubContentPolicy.isImageExtension("png"))
        assertTrue(NovelEpubContentPolicy.isImageExtension("webp"))
        assertTrue(NovelEpubContentPolicy.isImageExtension("gif"))
        assertFalse(NovelEpubContentPolicy.isImageExtension("svg"))

        assertTrue(NovelEpubContentPolicy.isMarkupExtension("HTML"))
        assertTrue(NovelEpubContentPolicy.isMarkupExtension("xhtml"))
        assertTrue(NovelEpubContentPolicy.isMarkupExtension("htm"))
        assertFalse(NovelEpubContentPolicy.isMarkupExtension("css"))

        assertTrue(NovelEpubContentPolicy.isStylesheetExtension("CSS"))
        assertFalse(NovelEpubContentPolicy.isStylesheetExtension("scss"))
    }

    @Test
    fun wrapBodyContentWrapsBodyChildren() {
        val html = "<html><body class=\"chapter\"><p>Hello</p></body></html>"

        val result = NovelEpubContentPolicy.wrapBodyContent(html)

        assertEquals(
            "<html><body class=\"chapter\"><div id=\"hoshi-content-wrapper\"><p>Hello</p></div></body></html>",
            result,
        )
    }

    @Test
    fun wrapBodyContentIsCaseInsensitive() {
        val html = "<HTML><BODY><p>Hello</p></BODY></HTML>"

        val result = NovelEpubContentPolicy.wrapBodyContent(html)

        assertEquals(
            "<HTML><BODY><div id=\"hoshi-content-wrapper\"><p>Hello</p></div></BODY></HTML>",
            result,
        )
    }

    @Test
    fun wrapBodyContentLeavesAlreadyWrappedOrBodylessHtmlUnchanged() {
        val wrapped = "<body><div id=\"hoshi-content-wrapper\"><p>Hello</p></div></body>"
        val bodyless = "<section><p>Hello</p></section>"

        assertEquals(wrapped, NovelEpubContentPolicy.wrapBodyContent(wrapped))
        assertEquals(bodyless, NovelEpubContentPolicy.wrapBodyContent(bodyless))
    }

    @Test
    fun cleanCssRemovesReaderHostileGlobalRules() {
        val css = """
            @page { margin: 0; }
            html, body { margin: 0; padding: 0; }
            p {
              -epub-hyphens: auto;
              -webkit-writing-mode: vertical-rl;
              writing-mode: vertical-rl;
              column-count: 2;
              overflow-x: hidden;
              line-height: 1.4;
              text-indent: 2em;
              text-align: justify;
              color: black;
            }
            .body-text { color: red; }
        """.trimIndent()

        val result = NovelEpubContentPolicy.cleanCss(css)

        assertFalse(result.contains("@page"))
        assertFalse(result.contains("html, body"))
        assertFalse(result.contains("-epub-hyphens"))
        assertFalse(result.contains("writing-mode"))
        assertFalse(result.contains("column-count"))
        assertFalse(result.contains("overflow-x"))
        assertFalse(result.contains("line-height"))
        assertFalse(result.contains("text-indent"))
        assertFalse(result.contains("text-align"))
        assertTrue(result.contains("color: black"))
        assertTrue(result.contains(".body-text { color: red; }"))
    }

    @Test
    fun cleanCssPreservesBlankCss() {
        assertEquals("  \n", NovelEpubContentPolicy.cleanCss("  \n"))
    }
}
