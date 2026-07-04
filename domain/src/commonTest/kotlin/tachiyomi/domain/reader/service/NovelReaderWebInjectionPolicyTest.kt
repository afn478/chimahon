package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.ReaderSettings

class NovelReaderWebInjectionPolicyTest {
    @Test
    fun readerModePrefersImageOnlyBeforeContinuousMode() {
        assertEquals(
            NovelReaderWebInjectionPolicy.ReaderMode.IMAGE_ONLY,
            NovelReaderWebInjectionPolicy.readerMode(
                isImageOnly = true,
                continuousMode = false,
            ),
        )
        assertEquals(
            NovelReaderWebInjectionPolicy.ReaderMode.IMAGE_ONLY,
            NovelReaderWebInjectionPolicy.readerMode(
                isImageOnly = true,
                continuousMode = true,
            ),
        )
    }

    @Test
    fun readerModeSelectsContinuousOrPagedTextModes() {
        assertEquals(
            NovelReaderWebInjectionPolicy.ReaderMode.CONTINUOUS,
            NovelReaderWebInjectionPolicy.readerMode(
                isImageOnly = false,
                continuousMode = true,
            ),
        )
        assertEquals(
            NovelReaderWebInjectionPolicy.ReaderMode.PAGED,
            NovelReaderWebInjectionPolicy.readerMode(
                isImageOnly = false,
                continuousMode = false,
            ),
        )
    }

    @Test
    fun imageOnlyInjectionScriptInstallsMinimalReaderApi() {
        val script = NovelReaderWebInjectionPolicy.readerInjectionScript(
            mode = NovelReaderWebInjectionPolicy.ReaderMode.IMAGE_ONLY,
            readerJs = "window.fullReaderShouldNotLoad = true;",
            settings = ReaderSettings(theme = "dark"),
            pendingProgress = 0.5,
        )

        assertTrue(script.contains("window.hoshiReader = {"))
        assertTrue(script.contains("handleTap: function(clientX, clientY)"))
        assertTrue(script.contains("paginate: function(direction)"))
        assertTrue(script.contains("return 'limit';"))
        assertTrue(script.contains("var target = document.querySelector('img, svg');"))
        assertTrue(script.contains("background:#1a1a1a!important"))
        assertTrue(!script.contains("window.fullReaderShouldNotLoad"))
    }

    @Test
    fun continuousInjectionScriptBuildsScrollableReader() {
        val script = NovelReaderWebInjectionPolicy.readerInjectionScript(
            mode = NovelReaderWebInjectionPolicy.ReaderMode.CONTINUOUS,
            readerJs = "window.readerJsLoaded = true;",
            settings = ReaderSettings(
                verticalWriting = false,
                horizontalPadding = 12.0,
                verticalPadding = 6.0,
            ),
            pendingProgress = 0.42,
        )

        assertTrue(script.contains("window.webkit.messageHandlers.restoreCompleted"))
        assertTrue(script.contains("reader-cont-img-style"))
        assertTrue(script.contains("window.readerJsLoaded = true;"))
        assertTrue(script.contains("if (!b) { window.hoshiReader.notifyRestoreComplete(); return; }"))
        assertTrue(script.contains("b.style.setProperty('touch-action', 'pan-y', 'important');"))
        assertTrue(script.contains("window.hoshiReader.continuousMode = true;"))
        assertTrue(script.contains("window.hoshiReader.restoreProgress(0.42, false);"))
        assertAppearsBefore(script, "reader-cont-img-style", "window.readerJsLoaded = true;")
        assertAppearsBefore(script, "window.readerJsLoaded = true;", "window.hoshiReader.registerCopyText();")
    }

    @Test
    fun pagedInjectionScriptBuildsColumnReader() {
        val script = NovelReaderWebInjectionPolicy.readerInjectionScript(
            mode = NovelReaderWebInjectionPolicy.ReaderMode.PAGED,
            readerJs = "window.readerJsLoaded = true;",
            settings = ReaderSettings(
                verticalWriting = true,
                avoidPageBreak = true,
                fontSize = 20.0,
            ),
            pendingProgress = 0.75,
        )

        assertTrue(script.contains("reader-block-img-style"))
        assertTrue(script.contains("window.readerJsLoaded = true;"))
        assertTrue(script.contains("break-inside: avoid !important"))
        assertTrue(script.contains("hoshi-override-style"))
        assertTrue(script.contains("b.style.setProperty('column-width', ih + 'px', 'important');"))
        assertTrue(script.contains("window.hoshiReader.restoreProgress(0.75, true);"))
        assertTrue(!script.contains("window.hoshiReader.continuousMode = true;"))
        assertAppearsBefore(script, "window.readerJsLoaded = true;", "hoshi-override-style")
        assertAppearsBefore(script, "hoshi-override-style", "window.hoshiReader.registerCopyText();")
    }

    @Test
    fun liveSettingsScriptUpdatesWrappedReaderDom() {
        val script = NovelReaderWebInjectionPolicy.liveSettingsScript(
            ReaderSettings(
                fontSize = 20.0,
                horizontalPadding = 8.0,
                verticalPadding = 12.0,
                paragraphSpacing = 0.5,
                theme = "custom",
                backgroundColor = 0xFF102030.toInt(),
                textColor = 0xFFE0F0A0.toInt(),
            ),
        )

        assertTrue(script.contains("if (!b) return;"))
        assertTrue(script.contains("var iw = window.innerWidth;"))
        assertTrue(script.contains("var ih = window.innerHeight;"))
        assertTrue(script.contains("var hPad = Math.round(iw * 8.0 / 100);"))
        assertTrue(script.contains("var vPad = Math.round(ih * 12.0 / 100);"))
        assertTrue(script.contains("wrapper.style.setProperty('font-size', '20.0px', 'important');"))
        assertTrue(script.contains("b.style.setProperty('font-size', '20.0px', 'important');"))
        assertTrue(script.contains("margin-block-end: 0.5em !important"))
        assertTrue(script.contains("b.style.setProperty('background-color', '#102030', 'important')"))
        assertTrue(script.contains("wrapper.style.setProperty('color', '#E0F0A0', 'important')"))
        assertAppearsBefore(
            script,
            "if (!b) return;",
            "wrapper.style.setProperty('padding'",
        )
        assertAppearsBefore(
            script,
            "wrapper.style.setProperty('padding'",
            "wrapper.style.setProperty('font-size'",
        )
    }

    private fun assertAppearsBefore(
        text: String,
        first: String,
        second: String,
    ) {
        val firstIndex = text.indexOf(first)
        val secondIndex = text.indexOf(second)
        assertTrue(firstIndex >= 0, "Expected to find `$first`.")
        assertTrue(secondIndex >= 0, "Expected to find `$second`.")
        assertTrue(firstIndex < secondIndex, "Expected `$first` before `$second`.")
    }
}
