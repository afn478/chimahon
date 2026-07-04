package tachiyomi.domain.manga.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MangaViewerFlagsTest {
    @Test
    fun readingModeReplacesOnlyReadingModeBits() {
        val flags = MangaViewerFlags.withReadingMode(
            flags = ORIENTATION_FREE,
            flag = READING_MODE_WEBTOON,
        )

        assertEquals(READING_MODE_WEBTOON, MangaViewerFlags.readingMode(flags))
        assertEquals(ORIENTATION_FREE, MangaViewerFlags.orientation(flags))
    }

    @Test
    fun orientationReplacesOnlyOrientationBits() {
        val flags = MangaViewerFlags.withOrientation(
            flags = READING_MODE_LEFT_TO_RIGHT,
            flag = ORIENTATION_LOCKED_PORTRAIT,
        )

        assertEquals(READING_MODE_LEFT_TO_RIGHT, MangaViewerFlags.readingMode(flags))
        assertEquals(ORIENTATION_LOCKED_PORTRAIT, MangaViewerFlags.orientation(flags))
    }

    @Test
    fun masksIgnoreBitsOutsideTheirGroup() {
        val flags = OUTSIDE_VIEWER_BITS or READING_MODE_WEBTOON or ORIENTATION_LOCKED_PORTRAIT

        assertEquals(READING_MODE_WEBTOON, MangaViewerFlags.readingMode(flags))
        assertEquals(ORIENTATION_LOCKED_PORTRAIT, MangaViewerFlags.orientation(flags))
    }

    private companion object {
        const val READING_MODE_LEFT_TO_RIGHT = 0x00000001L
        const val READING_MODE_WEBTOON = 0x00000004L
        const val ORIENTATION_FREE = 0x00000008L
        const val ORIENTATION_LOCKED_PORTRAIT = 0x00000020L
        const val OUTSIDE_VIEWER_BITS = 0x00000100L
    }
}
