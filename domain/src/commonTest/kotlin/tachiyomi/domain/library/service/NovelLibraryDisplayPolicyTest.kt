package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.NovelLibrarySortMode
import kotlin.test.Test
import kotlin.test.assertEquals

class NovelLibraryDisplayPolicyTest {
    @Test
    fun normalizedCategoryIdsDropsBlankAndUnknownNonDefaultCategories() {
        val result = NovelLibraryDisplayPolicy.normalizedCategoryIds(
            categoryIds = listOf("", "default", "sci-fi", "missing", "sci-fi"),
            knownCategoryIds = setOf("default", "sci-fi"),
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("sci-fi"), result)
    }

    @Test
    fun normalizedCategoryIdsKeepsDefaultWhenNoOtherCategoryExists() {
        val result = NovelLibraryDisplayPolicy.normalizedCategoryIds(
            categoryIds = listOf("default", "default"),
            knownCategoryIds = setOf("default", "sci-fi"),
            uncategorizedCategoryId = "default",
        )

        assertEquals(listOf("default"), result)
    }

    @Test
    fun filterBooksForSystemCategoryIncludesDefaultAndUncategorizedBooks() {
        val result = filterBooks(
            categoryId = "default",
            categoryIsSystem = true,
            books = listOf(
                book(id = "1", categories = listOf("default")),
                book(id = "2", categories = emptyList()),
                book(id = "3", categories = listOf("sci-fi")),
            ),
        )

        assertEquals(listOf("1", "2"), result.map(TestBook::id))
    }

    @Test
    fun filterBooksForUserCategoryMatchesKnownCategoryAfterSearch() {
        val result = filterBooks(
            categoryId = "sci-fi",
            categoryIsSystem = false,
            searchQuery = "Mars",
            books = listOf(
                book(id = "1", title = "Mars Colony", categories = listOf("sci-fi")),
                book(id = "2", title = "Ocean Colony", categories = listOf("sci-fi")),
                book(id = "3", title = "Mars Notes", categories = listOf("missing")),
            ),
        )

        assertEquals(listOf("1"), result.map(TestBook::id))
    }

    @Test
    fun selectBooksForCategorySortsByRequestedModeAndDirection() {
        val result = selectBooks(
            books = listOf(
                book(id = "1", title = "Beta", dateAdded = 10, lastRead = 50, categories = listOf("sci-fi")),
                book(id = "2", title = "Alpha", dateAdded = 30, lastRead = 70, categories = listOf("sci-fi")),
                book(id = "3", title = "Gamma", dateAdded = 20, lastRead = 60, categories = listOf("sci-fi")),
            ),
            sortMode = NovelLibrarySortMode.DATE_ADDED,
            sortDescending = true,
        )

        assertEquals(listOf("2", "3", "1"), result.map(TestBook::id))
    }

    @Test
    fun selectBooksForCategorySortsAlphabeticallyAscending() {
        val result = selectBooks(
            books = listOf(
                book(id = "1", title = "Beta", categories = listOf("sci-fi")),
                book(id = "2", title = "alpha", categories = listOf("sci-fi")),
                book(id = "3", title = null, categories = listOf("sci-fi")),
            ),
            sortMode = NovelLibrarySortMode.ALPHABETICAL,
            sortDescending = false,
        )

        assertEquals(listOf("3", "2", "1"), result.map(TestBook::id))
    }

    @Test
    fun selectBooksForCategorySortsByLastReadDescending() {
        val result = selectBooks(
            books = listOf(
                book(id = "1", lastRead = 20, categories = listOf("sci-fi")),
                book(id = "2", lastRead = 10, categories = listOf("sci-fi")),
                book(id = "3", lastRead = 30, categories = listOf("sci-fi")),
            ),
            sortMode = NovelLibrarySortMode.LAST_READ,
            sortDescending = true,
        )

        assertEquals(listOf("3", "1", "2"), result.map(TestBook::id))
    }

    @Test
    fun matchesSearchAllowsBlankQueriesAndMatchesTitlesIgnoringCase() {
        assertEquals(true, NovelLibraryDisplayPolicy.matchesSearch(title = null, searchQuery = null))
        assertEquals(true, NovelLibraryDisplayPolicy.matchesSearch(title = "Mars Colony", searchQuery = "mars"))
        assertEquals(false, NovelLibraryDisplayPolicy.matchesSearch(title = null, searchQuery = "mars"))
    }

    private fun filterBooks(
        categoryId: String,
        categoryIsSystem: Boolean,
        books: List<TestBook>,
        searchQuery: String? = null,
    ): List<TestBook> {
        return NovelLibraryDisplayPolicy.filterBooksForCategory(
            books = books,
            categoryId = categoryId,
            categoryIsSystem = categoryIsSystem,
            knownCategoryIds = KNOWN_CATEGORY_IDS,
            uncategorizedCategoryId = "default",
            searchQuery = searchQuery,
            bookTitle = TestBook::title,
            bookCategoryIds = TestBook::categories,
        )
    }

    private fun selectBooks(
        books: List<TestBook>,
        sortMode: NovelLibrarySortMode,
        sortDescending: Boolean,
    ): List<TestBook> {
        return NovelLibraryDisplayPolicy.selectBooksForCategory(
            books = books,
            categoryId = "sci-fi",
            categoryIsSystem = false,
            knownCategoryIds = KNOWN_CATEGORY_IDS,
            uncategorizedCategoryId = "default",
            searchQuery = null,
            sortMode = sortMode,
            sortDescending = sortDescending,
            bookId = TestBook::id,
            bookTitle = TestBook::title,
            bookDateAdded = TestBook::dateAdded,
            bookLastRead = TestBook::lastRead,
            bookCategoryIds = TestBook::categories,
        )
    }

    private fun book(
        id: String,
        title: String? = "Book $id",
        dateAdded: Long = 0,
        lastRead: Long = 0,
        categories: List<String>,
    ): TestBook {
        return TestBook(
            id = id,
            title = title,
            dateAdded = dateAdded,
            lastRead = lastRead,
            categories = categories,
        )
    }

    private data class TestBook(
        val id: String,
        val title: String?,
        val dateAdded: Long,
        val lastRead: Long,
        val categories: List<String>,
    )

    private companion object {
        val KNOWN_CATEGORY_IDS = setOf("default", "sci-fi")
    }
}
