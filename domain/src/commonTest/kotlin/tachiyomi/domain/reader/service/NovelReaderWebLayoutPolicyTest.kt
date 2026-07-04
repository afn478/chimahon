package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.ReaderSettings

class NovelReaderWebLayoutPolicyTest {
    @Test
    fun pagedBottomOverlapUsesFontSizeOnlyForVerticalWriting() {
        assertEquals(
            18.5,
            NovelReaderWebLayoutPolicy.pagedBottomOverlapPx(
                ReaderSettings(verticalWriting = true, fontSize = 18.5),
            ),
        )
        assertEquals(
            0.0,
            NovelReaderWebLayoutPolicy.pagedBottomOverlapPx(
                ReaderSettings(verticalWriting = false, fontSize = 18.5),
            ),
        )
    }

    @Test
    fun viewportMetricsScriptBuildsFullViewportImageBounds() {
        val script = NovelReaderWebLayoutPolicy.viewportMetricsScript(
            settings = ReaderSettings(
                horizontalPadding = 12.5,
                verticalPadding = 8.0,
            ),
            imageHeightMode = NovelReaderWebLayoutPolicy.ImageHeightMode.FULL_VIEWPORT,
        )

        assertTrue(script.contains("var ih = window.innerHeight;"))
        assertTrue(script.contains("var iw = window.innerWidth;"))
        assertTrue(script.contains("var hPad = Math.round(iw * 12.5 / 100);"))
        assertTrue(script.contains("var vPad = Math.round(ih * 8.0 / 100);"))
        assertTrue(script.contains("var imgMaxW = Math.max(1, Math.floor(iw * (100 - 12.5) / 100));"))
        assertTrue(script.contains("var imgMaxH = Math.max(1, ih);"))
        assertTrue(script.contains("document.documentElement.style.setProperty('--reader-image-max-width', imgMaxW + 'px');"))
        assertTrue(script.contains("document.documentElement.style.setProperty('--reader-image-max-height', imgMaxH + 'px');"))
    }

    @Test
    fun viewportMetricsScriptBuildsPagedImageBoundsWithVerticalOverlap() {
        val script = NovelReaderWebLayoutPolicy.viewportMetricsScript(
            settings = ReaderSettings(
                verticalWriting = true,
                fontSize = 20.0,
                horizontalPadding = 5.0,
                verticalPadding = 4.0,
            ),
            imageHeightMode = NovelReaderWebLayoutPolicy.ImageHeightMode.PAGED_WITH_VERTICAL_OVERLAP,
        )

        assertTrue(script.contains("var hPad = Math.round(iw * 5.0 / 100);"))
        assertTrue(script.contains("var vPad = Math.round(ih * 4.0 / 100);"))
        assertTrue(script.contains("var imgMaxH = Math.max(1, ih - 20.0);"))
    }

    @Test
    fun viewportMetricsScriptBuildsPagedImageBoundsWithoutHorizontalOverlap() {
        val script = NovelReaderWebLayoutPolicy.viewportMetricsScript(
            settings = ReaderSettings(
                verticalWriting = false,
                fontSize = 20.0,
            ),
            imageHeightMode = NovelReaderWebLayoutPolicy.ImageHeightMode.PAGED_WITH_VERTICAL_OVERLAP,
        )

        assertTrue(script.contains("var imgMaxH = Math.max(1, ih - 0);"))
    }

    @Test
    fun contentPaddingScriptCanReuseExistingViewportMetrics() {
        val script = NovelReaderWebLayoutPolicy.contentPaddingScript(
            settings = ReaderSettings(horizontalPadding = 15.0, verticalPadding = 6.0),
            declareViewportMetrics = false,
        )

        assertTrue(!script.contains("window.innerWidth"))
        assertTrue(!script.contains("window.innerHeight"))
        assertTrue(script.contains("wrapper.style.setProperty('padding', vPad + 'px 0', 'important');"))
        assertTrue(script.contains("pPadStyle.id = 'hoshi-p-padding-style';"))
        assertTrue(script.contains("padding-left: ' + hPad + 'px !important; padding-right: ' + hPad + 'px !important;"))
    }

    @Test
    fun contentPaddingScriptCanDeclareViewportMetricsForSettingsUpdates() {
        val script = NovelReaderWebLayoutPolicy.contentPaddingScript(
            settings = ReaderSettings(horizontalPadding = 15.0, verticalPadding = 6.0),
            declareViewportMetrics = true,
        )

        assertTrue(script.contains("var iw = window.innerWidth;"))
        assertTrue(script.contains("var ih = window.innerHeight;"))
        assertTrue(script.contains("var hPad = Math.round(iw * 15.0 / 100);"))
        assertTrue(script.contains("var vPad = Math.round(ih * 6.0 / 100);"))
    }

    @Test
    fun imageOnlyDocumentLayoutScriptFitsSingleImageToViewport() {
        val script = NovelReaderWebLayoutPolicy.imageOnlyDocumentLayoutScript(
            backgroundHex = "#112233",
            restoreCompletedScript = "window.done();",
        )

        assertTrue(script.contains("var w = window.innerWidth;"))
        assertTrue(script.contains("var h = window.innerHeight;"))
        assertTrue(script.contains("'overflow:hidden!important;background:#112233!important;'"))
        assertTrue(script.contains("var target = document.querySelector('img, svg');"))
        assertTrue(script.contains("Array.from(document.body.children).forEach(function(child)"))
        assertTrue(script.contains("if (!child.contains(target)) child.style.display = 'none';"))
        assertTrue(script.contains("'display:flex!important;align-items:center!important;justify-content:center!important;'"))
        assertTrue(script.contains("while (curr && curr !== document.body)"))
        assertTrue(script.contains("'object-fit:contain!important;display:block!important;margin:auto!important;padding:0!important;'"))
    }

    @Test
    fun imageOnlyDocumentLayoutScriptRestoresWhenBodyOrTargetIsMissing() {
        val script = NovelReaderWebLayoutPolicy.imageOnlyDocumentLayoutScript(
            backgroundHex = "#000000",
            restoreCompletedScript = "restoreBridge.done();",
        )

        assertTrue(script.contains("if (!document.body) {"))
        assertTrue(script.contains("if (!target) {"))
        assertTrue(script.contains("restoreBridge.done();\n    return;"))
    }

    @Test
    fun imageOnlyDocumentLayoutScriptRestoresForSvgAndImageLoadEvents() {
        val script = NovelReaderWebLayoutPolicy.imageOnlyDocumentLayoutScript(
            backgroundHex = "#000000",
            restoreCompletedScript = "restoreBridge.done();",
        )

        assertTrue(script.contains("if (target.tagName.toLowerCase() === 'svg')"))
        assertTrue(script.contains("target.setAttribute('preserveAspectRatio', 'xMidYMid meet');"))
        assertTrue(script.contains("if (target.complete && target.naturalWidth > 0)"))
        assertTrue(script.contains("target.onload = function() { restoreBridge.done(); };"))
        assertTrue(script.contains("target.onerror = function() { restoreBridge.done(); };"))
    }

    @Test
    fun continuousBodyLayoutScriptSetsVerticalAndHorizontalScrollPolicies() {
        val verticalScript = NovelReaderWebLayoutPolicy.continuousBodyLayoutScript(
            verticalWriting = true,
        )
        val horizontalScript = NovelReaderWebLayoutPolicy.continuousBodyLayoutScript(
            verticalWriting = false,
            bodyVar = "bodyEl",
        )

        assertTrue(verticalScript.contains("var vw = true;"))
        assertTrue(verticalScript.contains("b.style.setProperty('touch-action', 'pan-x', 'important');"))
        assertTrue(verticalScript.contains("document.documentElement.style.setProperty('overflow-x', 'auto', 'important');"))
        assertTrue(verticalScript.contains("document.documentElement.style.setProperty('overflow-y', 'hidden', 'important');"))
        assertTrue(verticalScript.contains("b.style.setProperty('min-height', ih + 'px', 'important');"))
        assertTrue(verticalScript.contains("document.documentElement.style.setProperty('height', 'auto', 'important');"))

        assertTrue(horizontalScript.contains("var vw = false;"))
        assertTrue(horizontalScript.contains("bodyEl.style.setProperty('touch-action', 'pan-y', 'important');"))
        assertTrue(horizontalScript.contains("bodyEl.style.setProperty('width', iw + 'px', 'important');"))
        assertTrue(horizontalScript.contains("bodyEl.style.setProperty('height', 'auto', 'important');"))
    }

    @Test
    fun pagedBodyLayoutScriptSetsColumnPagingPolicies() {
        val verticalScript = NovelReaderWebLayoutPolicy.pagedBodyLayoutScript(
            verticalWriting = true,
        )
        val horizontalScript = NovelReaderWebLayoutPolicy.pagedBodyLayoutScript(
            verticalWriting = false,
            bodyVar = "bodyEl",
        )

        assertTrue(verticalScript.contains("document.documentElement.style.setProperty('height', ih + 'px', 'important');"))
        assertTrue(verticalScript.contains("var vw = true;"))
        assertTrue(verticalScript.contains("b.style.setProperty('column-width', ih + 'px', 'important');"))
        assertTrue(verticalScript.contains("b.style.setProperty('min-height', ih + 'px', 'important');"))
        assertTrue(verticalScript.contains("b.style.setProperty('column-fill', 'auto', 'important');"))
        assertTrue(verticalScript.contains("b.style.setProperty('column-gap', '0px', 'important');"))
        assertTrue(verticalScript.contains("document.documentElement.style.setProperty('overflow', 'hidden', 'important');"))

        assertTrue(horizontalScript.contains("var vw = false;"))
        assertTrue(horizontalScript.contains("bodyEl.style.setProperty('column-width', iw + 'px', 'important');"))
        assertTrue(horizontalScript.contains("bodyEl.style.setProperty('height', ih + 'px', 'important');"))
        assertTrue(horizontalScript.contains("bodyEl.style.setProperty('touch-action', 'none', 'important');"))
    }

    @Test
    fun pagedOverrideStyleScriptUsesStableStyleElement() {
        val script = NovelReaderWebLayoutPolicy.pagedOverrideStyleScript(
            styleElementId = "reader'override",
        )

        assertTrue(script.contains("var overrideStyle = document.getElementById('reader\\'override');"))
        assertTrue(script.contains("overrideStyle = document.createElement('style');"))
        assertTrue(script.contains("overrideStyle.id = 'reader\\'override';"))
        assertTrue(script.contains("[class*=\"pt\"] { margin-top: 0 !important; }"))
        assertTrue(script.contains("[class*=\"pb\"] { margin-bottom: 0 !important; }"))
        assertTrue(script.contains("span.img.fpage, span.img.fblk { padding-bottom: 0 !important; }"))
        assertTrue(script.contains("@page { margin: 0 !important; }"))
    }
}
