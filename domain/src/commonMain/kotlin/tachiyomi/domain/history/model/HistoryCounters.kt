package tachiyomi.domain.history.model

object HistoryCounters {
    fun unreadCount(totalCount: Long, readCount: Long): Long {
        return totalCount - readCount
    }
}
