package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelReaderFileUrlPolicyTest {
    @Test
    fun fileUrlForAbsolutePathKeepsUnixPathsStable() {
        assertEquals(
            "file:///tmp/books/chapter.xhtml",
            NovelReaderFileUrlPolicy.fileUrlForAbsolutePath("/tmp/books/chapter.xhtml"),
        )
    }

    @Test
    fun fileUrlForAbsolutePathNormalizesWindowsDrivePaths() {
        assertEquals(
            "file:///C:/Books/Chapter.xhtml",
            NovelReaderFileUrlPolicy.fileUrlForAbsolutePath("C:\\Books\\Chapter.xhtml"),
        )
    }

    @Test
    fun fileUrlForAbsolutePathNormalizesUncPaths() {
        assertEquals(
            "file://server/share/Chapter.xhtml",
            NovelReaderFileUrlPolicy.fileUrlForAbsolutePath("\\\\server\\share\\Chapter.xhtml"),
        )
    }

    @Test
    fun localPathForFileUrlOrPathStripsQueryFragmentSchemeAndDecodes() {
        assertEquals(
            "/tmp/My Book/chapter.xhtml",
            NovelReaderFileUrlPolicy.localPathForFileUrlOrPath(
                "file:///tmp/My%20Book/chapter.xhtml?cache=1#part",
            ),
        )
    }

    @Test
    fun localPathForFileUrlOrPathDropsWindowsDriveLeadingSlash() {
        assertEquals(
            "C:/Books/Chapter.xhtml",
            NovelReaderFileUrlPolicy.localPathForFileUrlOrPath("file:///C:/Books/Chapter.xhtml#part"),
        )
    }

    @Test
    fun localPathForFileUrlOrPathPreservesUncFileUrls() {
        assertEquals(
            "//server/share/Chapter.xhtml",
            NovelReaderFileUrlPolicy.localPathForFileUrlOrPath("file://server/share/Chapter.xhtml"),
        )
    }

    @Test
    fun localPathForFileUrlOrPathNormalizesRawPaths() {
        assertEquals(
            "C:/Books/Chapter.xhtml",
            NovelReaderFileUrlPolicy.localPathForFileUrlOrPath("C:\\Books\\Chapter.xhtml"),
        )
    }

    @Test
    fun isLocalFileUrlOrPathMatchesFileUrlsAndRawPaths() {
        assertTrue(
            NovelReaderFileUrlPolicy.isLocalFileUrlOrPath("file:///tmp/book/chapter.xhtml"),
        )
        assertTrue(
            NovelReaderFileUrlPolicy.isLocalFileUrlOrPath("C:\\Books\\Chapter.xhtml"),
        )
        assertFalse(
            NovelReaderFileUrlPolicy.isLocalFileUrlOrPath("https://example.com/chapter.xhtml"),
        )
        assertFalse(
            NovelReaderFileUrlPolicy.isLocalFileUrlOrPath("content://books/chapter.xhtml"),
        )
    }

    @Test
    fun hrefPathForComparisonNormalizesAndDecodesHref() {
        assertEquals(
            "OPS/Text/My Chapter.xhtml",
            NovelReaderFileUrlPolicy.hrefPathForComparison("OPS\\Text\\My%20Chapter.xhtml#frag"),
        )
    }

    @Test
    fun fragmentForUrlReturnsFragmentOnlyWhenPresent() {
        assertEquals(
            "section-2",
            NovelReaderFileUrlPolicy.fragmentForUrl("file:///tmp/chapter.xhtml#section-2"),
        )
        assertEquals(null, NovelReaderFileUrlPolicy.fragmentForUrl("file:///tmp/chapter.xhtml"))
    }

    @Test
    fun percentDecodeLeavesInvalidEscapesLiteral() {
        assertEquals(
            "chapter%ZZ.xhtml",
            NovelReaderFileUrlPolicy.percentDecode("chapter%ZZ.xhtml"),
        )
    }
}
