package tachiyomi.domain.manga.service

import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.MangaViewerFlags
import kotlin.test.Test
import kotlin.test.assertEquals

class MangaViewerFlagUpdatePolicyTest {
    @Test
    fun readingModeUpdateOnlyChangesViewerFlags() {
        val update = MangaViewerFlagUpdatePolicy.readingModeUpdate(
            mangaId = MANGA_ID,
            currentFlags = ORIENTATION_FREE,
            flag = READING_MODE_WEBTOON,
        )
        val viewerFlags = update.viewerFlags ?: error("Viewer flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, viewerFlags = viewerFlags), update)
        assertEquals(READING_MODE_WEBTOON, MangaViewerFlags.readingMode(viewerFlags))
        assertEquals(ORIENTATION_FREE, MangaViewerFlags.orientation(viewerFlags))
    }

    @Test
    fun orientationUpdateOnlyChangesOrientation() {
        val update = MangaViewerFlagUpdatePolicy.orientationUpdate(
            mangaId = MANGA_ID,
            currentFlags = READING_MODE_LEFT_TO_RIGHT,
            flag = ORIENTATION_LOCKED_PORTRAIT,
        )
        val viewerFlags = update.viewerFlags ?: error("Viewer flags must be set")

        assertEquals(MangaUpdate(id = MANGA_ID, viewerFlags = viewerFlags), update)
        assertEquals(READING_MODE_LEFT_TO_RIGHT, MangaViewerFlags.readingMode(viewerFlags))
        assertEquals(ORIENTATION_LOCKED_PORTRAIT, MangaViewerFlags.orientation(viewerFlags))
    }

    private companion object {
        const val MANGA_ID = 42L
        const val READING_MODE_LEFT_TO_RIGHT = 0x00000001L
        const val READING_MODE_WEBTOON = 0x00000004L
        const val ORIENTATION_FREE = 0x00000008L
        const val ORIENTATION_LOCKED_PORTRAIT = 0x00000020L
    }
}
