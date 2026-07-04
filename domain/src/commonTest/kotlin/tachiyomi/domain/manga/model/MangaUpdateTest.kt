package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MangaUpdateTest {
    @Test
    fun defaultUpdateOnlyRequiresId() {
        val update = MangaUpdate(id = 1)

        assertEquals(1, update.id)
        assertNull(update.source)
        assertNull(update.title)
        assertNull(update.filteredScanlators)
    }

    @Test
    fun updateCarriesCommonSourceMetadata() {
        val update = MangaUpdate(
            id = 10,
            title = "Title",
            genre = listOf("Action", "Drama"),
            updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
            initialized = true,
            filteredScanlators = listOf("Group"),
        )

        assertEquals("Title", update.title)
        assertEquals(listOf("Action", "Drama"), update.genre)
        assertEquals(UpdateStrategy.ONLY_FETCH_ONCE, update.updateStrategy)
        assertEquals(true, update.initialized)
        assertEquals(listOf("Group"), update.filteredScanlators)
    }
}
