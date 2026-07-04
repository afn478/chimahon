package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.EXHSavedSearch
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceSavedSearchPolicyTest {
    @Test
    fun sortSavedSearchesUsesCaseInsensitiveNameOrder() {
        val result = SourceSavedSearchPolicy.sortSavedSearches(
            listOf(
                savedSearch(id = 1, name = "zeta"),
                savedSearch(id = 2, name = "Alpha"),
                savedSearch(id = 3, name = "beta"),
            ),
        )

        assertEquals(listOf(2L, 3L, 1L), result.map(EXHSavedSearch::id))
    }

    private fun savedSearch(
        id: Long,
        name: String,
    ): EXHSavedSearch {
        return EXHSavedSearch(
            id = id,
            name = name,
            query = null,
            filterList = null,
        )
    }
}
