package tachiyomi.domain.history.model

import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryCountersTest {
    @Test
    fun unreadCountSubtractsReadCountFromTotalCount() {
        assertEquals(3, HistoryCounters.unreadCount(totalCount = 5, readCount = 2))
    }

    @Test
    fun unreadCountKeepsExistingBehaviorWhenReadCountExceedsTotalCount() {
        assertEquals(-1, HistoryCounters.unreadCount(totalCount = 2, readCount = 3))
    }
}
