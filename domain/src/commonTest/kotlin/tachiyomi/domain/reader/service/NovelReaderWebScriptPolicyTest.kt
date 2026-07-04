package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelReaderWebScriptPolicyTest {
    @Test
    fun jsStringLiteralEscapesSingleQuotedJavascriptContent() {
        assertEquals(
            "'a\\\\b\\'c\\n\\r\\t'",
            NovelReaderWebScriptPolicy.jsStringLiteral("a\\b'c\n\r\t"),
        )
        assertEquals(
            "'\\u2028\\u2029'",
            NovelReaderWebScriptPolicy.jsStringLiteral("\u2028\u2029"),
        )
    }

    @Test
    fun jsSingleQuotedContentEscapesWithoutAddingQuotes() {
        assertEquals(
            "font\\\\family\\'name\\n",
            NovelReaderWebScriptPolicy.jsSingleQuotedContent("font\\family'name\n"),
        )
    }

    @Test
    fun scrollToFragmentScriptUsesSafeLiteralAndNameFallback() {
        val script = NovelReaderWebScriptPolicy.scrollToFragmentScript("chapter'1")

        assertTrue(script.contains("var id = 'chapter\\'1';"))
        assertTrue(script.contains("document.getElementById(id)"))
        assertTrue(script.contains("document.getElementsByName(id)"))
        assertTrue(script.contains("scrollIntoView({ behavior: 'instant', block: 'start' })"))
    }

    @Test
    fun selectionCommandScriptsTargetHoshiReaderApi() {
        assertEquals(
            "if(window.hoshiReader && window.hoshiReader.clearSelection) { window.hoshiReader.clearSelection(); }",
            NovelReaderWebScriptPolicy.clearSelectionScript(),
        )
        assertEquals(
            "if(window.hoshiReader && window.hoshiReader.highlightSelection) { window.hoshiReader.highlightSelection(42); }",
            NovelReaderWebScriptPolicy.highlightSelectionScript(42),
        )
        assertEquals(
            "(function() { try { return window.hoshiReader.getSelectionRects(42, 7); } catch(e) { return []; } })()",
            NovelReaderWebScriptPolicy.selectionRectsScript(charCount = 42, startOffset = 7),
        )
    }

    @Test
    fun tapAndProgressScriptsTargetHoshiReaderApi() {
        assertEquals(
            "if (window.hoshiReader && window.hoshiReader.handleTap) { window.hoshiReader.handleTap(12.5, 4.0); }",
            NovelReaderWebScriptPolicy.handleTapScript(cssX = 12.5f, cssY = 4f),
        )
        assertEquals(
            "(function() { return window.hoshiReader.calculateProgress(); })()",
            NovelReaderWebScriptPolicy.calculateProgressScript(),
        )
    }

    @Test
    fun imageOnlyReaderApiScriptProvidesMinimalReaderApi() {
        val script = NovelReaderWebScriptPolicy.imageOnlyReaderApiScript(
            backgroundTapCallScript = "window.bridge.tap(clientX, clientY);",
        )

        assertTrue(script.contains("window.hoshiReader = {"))
        assertTrue(script.contains("handleTap: function(clientX, clientY)"))
        assertTrue(script.contains("window.bridge.tap(clientX, clientY);"))
        assertTrue(script.contains("return false;"))
        assertTrue(script.contains("paginate: function(direction)"))
        assertTrue(script.contains("return 'limit';"))
        assertTrue(script.contains("calculateProgress: function()"))
        assertTrue(script.contains("return 0;"))
    }

    @Test
    fun continuousBoundaryScriptIncludesForwardDirection() {
        val script = NovelReaderWebScriptPolicy.continuousBoundaryScript(forward = true)

        assertTrue(script.contains("var forward = true;"))
        assertTrue(script.contains("if (forward) return y >= maxY - 2 ? 'limit' : 'scrolling';"))
        assertTrue(script.contains("if (forward) return absX >= maxX - 2 ? 'limit' : 'scrolling';"))
    }

    @Test
    fun continuousBoundaryScriptIncludesBackwardDirection() {
        val script = NovelReaderWebScriptPolicy.continuousBoundaryScript(forward = false)

        assertTrue(script.contains("var forward = false;"))
        assertTrue(script.contains("return y <= 2 ? 'limit' : 'scrolling';"))
        assertTrue(script.contains("return absX <= 2 ? 'limit' : 'scrolling';"))
    }

    @Test
    fun paginateScriptUsesSupportedDirections() {
        assertTrue(
            NovelReaderWebScriptPolicy.paginateScript(NovelReaderWebScriptPolicy.PageDirection.FORWARD)
                .contains("window.hoshiReader.paginate('forward')"),
        )
        assertTrue(
            NovelReaderWebScriptPolicy.paginateScript(NovelReaderWebScriptPolicy.PageDirection.BACKWARD)
                .contains("window.hoshiReader.paginate('backward')"),
        )
    }
}
