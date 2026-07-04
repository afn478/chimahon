package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelReaderWebMediaPolicyTest {
    @Test
    fun blockMediaStyleScriptUsesContinuousStyleWithoutColumnBreakRules() {
        val script = NovelReaderWebMediaPolicy.blockMediaStyleScript(
            NovelReaderWebMediaPolicy.MediaMode.CONTINUOUS,
        )

        assertTrue(script.contains("reader-cont-img-style"))
        assertTrue(script.contains("img.block-img, svg.block-img"))
        assertTrue(script.contains("max-width: var(--reader-image-max-width, 95vw) !important"))
        assertTrue(script.contains("max-height: var(--reader-image-max-height, 95vh) !important"))
        assertTrue(script.contains("object-fit: contain !important"))
        assertTrue(script.contains("img:not(.block-img), svg:not(.block-img)"))
        assertTrue(!script.contains("break-inside: avoid !important"))
        assertTrue(!script.contains("-webkit-column-break-inside: avoid !important"))
    }

    @Test
    fun blockMediaStyleScriptUsesPagedStyleWithColumnBreakRules() {
        val script = NovelReaderWebMediaPolicy.blockMediaStyleScript(
            NovelReaderWebMediaPolicy.MediaMode.PAGED,
        )

        assertTrue(script.contains("reader-block-img-style"))
        assertTrue(script.contains("break-inside: avoid !important"))
        assertTrue(script.contains("-webkit-column-break-inside: avoid !important"))
        assertTrue(script.contains("object-fit: contain !important"))
    }

    @Test
    fun avoidPageBreakStyleScriptCanBeDisabled() {
        assertEquals("", NovelReaderWebMediaPolicy.avoidPageBreakStyleScript(enabled = false))
    }

    @Test
    fun avoidPageBreakStyleScriptTargetsBlockMediaAndTables() {
        val script = NovelReaderWebMediaPolicy.avoidPageBreakStyleScript(enabled = true)

        assertTrue(script.contains("var abStyle = document.createElement('style');"))
        assertTrue(script.contains("img, svg, figure, table, tr, td, th"))
        assertTrue(script.contains("p:has(> img:only-child), div:has(> img:only-child), span.img, div.img, p.img"))
        assertTrue(script.contains("break-inside: avoid !important"))
        assertTrue(script.contains("page-break-inside: avoid !important"))
        assertTrue(script.contains("document.head.appendChild(abStyle);"))
    }

    @Test
    fun classifyMediaAndRestoreProgressScriptClassifiesLargeNonGaijiMedia() {
        val script = NovelReaderWebMediaPolicy.classifyMediaAndRestoreProgressScript(
            pendingProgress = 0.42,
            verticalWriting = true,
        )

        assertTrue(script.contains("Array.from(document.querySelectorAll('img, svg'))"))
        assertTrue(script.contains("el.classList.contains('gaiji') || el.classList.contains('gaiji-line')"))
        assertTrue(script.contains("el.naturalWidth > ${NovelReaderWebMediaPolicy.LARGE_MEDIA_THRESHOLD_PX}"))
        assertTrue(script.contains("el.naturalHeight > ${NovelReaderWebMediaPolicy.LARGE_MEDIA_THRESHOLD_PX}"))
        assertTrue(script.contains("var vb = el.viewBox && el.viewBox.baseVal;"))
        assertTrue(script.contains("isLarge = w > ${NovelReaderWebMediaPolicy.LARGE_MEDIA_THRESHOLD_PX} || h > ${NovelReaderWebMediaPolicy.LARGE_MEDIA_THRESHOLD_PX};"))
        assertTrue(script.contains("el.classList.add('block-img');"))
    }

    @Test
    fun classifyMediaAndRestoreProgressScriptWaitsBeforeRestore() {
        val verticalScript = NovelReaderWebMediaPolicy.classifyMediaAndRestoreProgressScript(
            pendingProgress = 0.42,
            verticalWriting = true,
        )
        val horizontalScript = NovelReaderWebMediaPolicy.classifyMediaAndRestoreProgressScript(
            pendingProgress = 0.75,
            verticalWriting = false,
        )

        assertTrue(verticalScript.contains("Promise.all(imagePromises)"))
        assertTrue(verticalScript.contains("setTimeout(r, 50)"))
        assertTrue(verticalScript.contains("window.hoshiReader.restoreProgress(0.42, true);"))
        assertTrue(horizontalScript.contains("window.hoshiReader.restoreProgress(0.75, false);"))
    }
}
