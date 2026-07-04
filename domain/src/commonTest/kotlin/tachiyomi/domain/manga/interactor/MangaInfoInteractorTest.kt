package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MangaInfoInteractorTest {
    @Test
    fun customMangaInfoInteractorsReadAndWriteRepositoryValues() {
        val repository = FakeCustomMangaRepository()
        val getCustomMangaInfo = GetCustomMangaInfo(repository)
        val setCustomMangaInfo = SetCustomMangaInfo(repository)

        assertNull(getCustomMangaInfo.get(1))

        val info = CustomMangaInfo(
            id = 1,
            title = "Custom title",
            author = "Custom author",
            artist = "Custom artist",
            thumbnailUrl = "https://example.invalid/cover.jpg",
            description = "Custom description",
            genre = listOf("Action", "Drama"),
            status = 2,
        )
        setCustomMangaInfo.set(info)

        assertEquals(info, getCustomMangaInfo.get(1))
    }
}

private class FakeCustomMangaRepository : CustomMangaRepository {
    private val entries = mutableMapOf<Long, CustomMangaInfo>()

    override fun get(mangaId: Long): CustomMangaInfo? {
        return entries[mangaId]
    }

    override fun set(mangaInfo: CustomMangaInfo) {
        entries[mangaInfo.id] = mangaInfo
    }
}
