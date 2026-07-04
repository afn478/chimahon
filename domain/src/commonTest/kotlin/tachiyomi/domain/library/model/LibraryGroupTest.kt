package tachiyomi.domain.library.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryGroupTest {
    @Test
    fun libraryGroupValuesRemainStableForPersistedPreferences() {
        assertEquals(0, LibraryGroup.BY_DEFAULT)
        assertEquals(1, LibraryGroup.BY_SOURCE)
        assertEquals(2, LibraryGroup.BY_STATUS)
        assertEquals(3, LibraryGroup.BY_TRACK_STATUS)
        assertEquals(4, LibraryGroup.UNGROUPED)
    }
}
