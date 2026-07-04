package tachiyomi.domain.library.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelBookMergePolicyTest {
    @Test
    fun selectLatestKeepsCurrentWhenTimestampsTie() {
        val result = NovelBookMergePolicy.selectLatest(
            current = item(id = "current", modified = 10),
            incoming = item(id = "incoming", modified = 10),
            lastModified = TestItem::modified,
        )

        assertEquals("current", result.id)
    }

    @Test
    fun selectLatestUsesIncomingWhenNewer() {
        val result = NovelBookMergePolicy.selectLatest(
            current = item(id = "current", modified = 10),
            incoming = item(id = "incoming", modified = 11),
            lastModified = TestItem::modified,
        )

        assertEquals("incoming", result.id)
    }

    @Test
    fun selectLatestOrNullHandlesMissingSides() {
        val incoming = item(id = "incoming", modified = 11)
        val current = item(id = "current", modified = 10)

        assertEquals(incoming, NovelBookMergePolicy.selectLatestOrNull(null, incoming, TestItem::modified))
        assertEquals(current, NovelBookMergePolicy.selectLatestOrNull(current, null, TestItem::modified))
        assertNull(NovelBookMergePolicy.selectLatestOrNull<TestItem>(null, null, TestItem::modified))
    }

    @Test
    fun mergeLatestByKeyKeepsLatestItemPerKey() {
        val result = NovelBookMergePolicy.mergeLatestByKey(
            items = listOf(
                item(id = "old-a", key = "a", modified = 1),
                item(id = "b", key = "b", modified = 5),
                item(id = "new-a", key = "a", modified = 3),
            ),
            itemKey = TestItem::key,
            lastModified = TestItem::modified,
        )

        assertEquals(listOf("new-a", "b"), result.map(TestItem::id))
    }

    @Test
    fun mergeLatestByKeyKeepsFirstItemWhenTimestampsTie() {
        val result = NovelBookMergePolicy.mergeLatestByKey(
            items = listOf(
                item(id = "first", key = "a", modified = 1),
                item(id = "second", key = "a", modified = 1),
            ),
            itemKey = TestItem::key,
            lastModified = TestItem::modified,
        )

        assertEquals(listOf("first"), result.map(TestItem::id))
    }

    @Test
    fun mergeLatestByKeyPreservesFirstSeenKeyOrder() {
        val result = NovelBookMergePolicy.mergeLatestByKey(
            items = listOf(
                item(id = "b-old", key = "b", modified = 1),
                item(id = "a", key = "a", modified = 5),
                item(id = "b-new", key = "b", modified = 2),
            ),
            itemKey = TestItem::key,
            lastModified = TestItem::modified,
        )

        assertEquals(listOf("b-new", "a"), result.map(TestItem::id))
    }

    @Test
    fun mergeCategoryIdsNormalizesCombinedIds() {
        val result = NovelBookMergePolicy.mergeCategoryIds(
            currentCategoryIds = listOf("default", "sci-fi"),
            incomingCategoryIds = listOf("sci-fi", "classics"),
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("sci-fi", "classics"), result)
    }

    private fun item(
        id: String,
        key: String = id,
        modified: Long,
    ): TestItem {
        return TestItem(
            id = id,
            key = key,
            modified = modified,
        )
    }

    private data class TestItem(
        val id: String,
        val key: String,
        val modified: Long,
    )
}
