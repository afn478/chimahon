package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderWebLoadPolicyTest {
    @Test
    fun urlLoadTargetLoadsSchemedUrlsDirectly() {
        assertEquals(
            NovelReaderWebLoadPolicy.UrlLoadTarget.DirectUrl("https://example.com/chapter.xhtml"),
            NovelReaderWebLoadPolicy.urlLoadTarget("https://example.com/chapter.xhtml"),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.UrlLoadTarget.DirectUrl("content://books/chapter.xhtml"),
            NovelReaderWebLoadPolicy.urlLoadTarget("content://books/chapter.xhtml"),
        )
    }

    @Test
    fun urlLoadTargetValidatesFileUrlsAndRawPaths() {
        assertEquals(
            NovelReaderWebLoadPolicy.UrlLoadTarget.LocalFile(
                url = "file:///tmp/My%20Book/chapter.xhtml?cache=1#part",
                localPath = "/tmp/My Book/chapter.xhtml",
            ),
            NovelReaderWebLoadPolicy.urlLoadTarget("file:///tmp/My%20Book/chapter.xhtml?cache=1#part"),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.UrlLoadTarget.LocalFile(
                url = "C:\\Books\\Chapter.xhtml",
                localPath = "C:/Books/Chapter.xhtml",
            ),
            NovelReaderWebLoadPolicy.urlLoadTarget("C:\\Books\\Chapter.xhtml"),
        )
    }

    @Test
    fun chapterLoadActionDefersUntilViewportHasSize() {
        assertEquals(
            NovelReaderWebLoadPolicy.ChapterLoadAction.Defer(
                delayMillis = NovelReaderWebLoadPolicy.CHAPTER_LOAD_DEFER_DELAY_MS,
            ),
            NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/chapter.xhtml",
                width = 0,
                height = 800,
                verticalWriting = false,
                lastLoadedKey = null,
            ),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.ChapterLoadAction.Defer(
                delayMillis = NovelReaderWebLoadPolicy.CHAPTER_LOAD_DEFER_DELAY_MS,
            ),
            NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/chapter.xhtml",
                width = 480,
                height = -1,
                verticalWriting = false,
                lastLoadedKey = null,
            ),
        )
    }

    @Test
    fun loadFailureMessagesKeepReaderHostCopyStable() {
        assertEquals(
            "File not found: file:///book/missing.xhtml",
            NovelReaderWebLoadPolicy.localFileMissingMessage("file:///book/missing.xhtml"),
        )
        assertEquals(
            "Disk permission denied",
            NovelReaderWebLoadPolicy.loadExceptionFailureMessage("Disk permission denied"),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.DEFAULT_CHAPTER_LOAD_FAILURE_MESSAGE,
            NovelReaderWebLoadPolicy.loadExceptionFailureMessage(null),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.DEFAULT_CHAPTER_LOAD_FAILURE_MESSAGE,
            NovelReaderWebLoadPolicy.loadExceptionFailureMessage(""),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.RendererGoneFailure(
                reason = "WebView crashed",
                message = "Renderer died (WebView crashed). Try disabling hardware acceleration or 'Avoid page breaks'.",
            ),
            NovelReaderWebLoadPolicy.rendererGoneFailure(crashed = true),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.RendererGoneFailure(
                reason = "WebView killed by system (OOM)",
                message = "Renderer died (WebView killed by system (OOM)). Try disabling hardware acceleration or 'Avoid page breaks'.",
            ),
            NovelReaderWebLoadPolicy.rendererGoneFailure(crashed = false),
        )
    }

    @Test
    fun receivedErrorActionReportsOnlyMainFrameFailures() {
        assertEquals(
            NovelReaderWebLoadPolicy.ReceivedErrorAction.ReportFailure("Network unavailable"),
            NovelReaderWebLoadPolicy.receivedErrorAction(
                isMainFrame = true,
                description = "Network unavailable",
            ),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.ReceivedErrorAction.ReportFailure(
                NovelReaderWebLoadPolicy.DEFAULT_CHAPTER_LOAD_FAILURE_MESSAGE,
            ),
            NovelReaderWebLoadPolicy.receivedErrorAction(
                isMainFrame = true,
                description = null,
            ),
        )
        assertEquals(
            NovelReaderWebLoadPolicy.ReceivedErrorAction.Ignore,
            NovelReaderWebLoadPolicy.receivedErrorAction(
                isMainFrame = false,
                description = "Missing image",
            ),
        )
    }

    @Test
    fun chapterLoadActionSkipsExactDuplicateRequest() {
        val key = loadKey(
            url = "file:///book/chapter.xhtml",
            width = 480,
            height = 800,
            verticalWriting = false,
        )

        assertEquals(
            NovelReaderWebLoadPolicy.ChapterLoadAction.SkipDuplicate,
            NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/chapter.xhtml",
                width = 480,
                height = 800,
                verticalWriting = false,
                lastLoadedKey = key,
            ),
        )
    }

    @Test
    fun chapterLoadActionLoadsInitialRequest() {
        val key = loadKey(
            url = "file:///book/chapter.xhtml",
            width = 480,
            height = 800,
            verticalWriting = false,
        )

        assertLoad(
            expectedKey = key,
            action = NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/chapter.xhtml",
                width = 480,
                height = 800,
                verticalWriting = false,
                lastLoadedKey = null,
            ),
        )
    }

    @Test
    fun chapterLoadActionLoadsWhenUrlViewportOrWritingModeChanges() {
        val lastLoadedKey = loadKey(
            url = "file:///book/chapter.xhtml",
            width = 480,
            height = 800,
            verticalWriting = false,
        )

        assertLoad(
            expectedKey = lastLoadedKey.copy(url = "file:///book/next.xhtml"),
            action = NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/next.xhtml",
                width = 480,
                height = 800,
                verticalWriting = false,
                lastLoadedKey = lastLoadedKey,
            ),
        )
        assertLoad(
            expectedKey = lastLoadedKey.copy(width = 540),
            action = NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/chapter.xhtml",
                width = 540,
                height = 800,
                verticalWriting = false,
                lastLoadedKey = lastLoadedKey,
            ),
        )
        assertLoad(
            expectedKey = lastLoadedKey.copy(height = 900),
            action = NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/chapter.xhtml",
                width = 480,
                height = 900,
                verticalWriting = false,
                lastLoadedKey = lastLoadedKey,
            ),
        )
        assertLoad(
            expectedKey = lastLoadedKey.copy(verticalWriting = true),
            action = NovelReaderWebLoadPolicy.chapterLoadAction(
                url = "file:///book/chapter.xhtml",
                width = 480,
                height = 800,
                verticalWriting = true,
                lastLoadedKey = lastLoadedKey,
            ),
        )
    }

    private fun assertLoad(
        expectedKey: NovelReaderWebLoadPolicy.ChapterLoadKey,
        action: NovelReaderWebLoadPolicy.ChapterLoadAction,
    ) {
        assertEquals(NovelReaderWebLoadPolicy.ChapterLoadAction.Load(expectedKey), action)
    }

    private fun loadKey(
        url: String,
        width: Int,
        height: Int,
        verticalWriting: Boolean,
    ): NovelReaderWebLoadPolicy.ChapterLoadKey {
        return NovelReaderWebLoadPolicy.ChapterLoadKey(
            url = url,
            width = width,
            height = height,
            verticalWriting = verticalWriting,
        )
    }
}
