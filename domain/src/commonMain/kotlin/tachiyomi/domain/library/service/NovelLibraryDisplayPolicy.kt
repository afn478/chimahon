package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.NovelLibrarySortMode

object NovelLibraryDisplayPolicy {
    fun <T> selectBooksForCategory(
        books: List<T>,
        categoryId: String,
        categoryIsSystem: Boolean,
        knownCategoryIds: Set<String>,
        uncategorizedCategoryId: String,
        searchQuery: String?,
        sortMode: NovelLibrarySortMode,
        sortDescending: Boolean,
        bookId: (T) -> String,
        bookTitle: (T) -> String?,
        bookDateAdded: (T) -> Long,
        bookLastRead: (T) -> Long,
        bookCategoryIds: (T) -> Collection<String>,
    ): List<T> {
        val filteredBooks = filterBooksForCategory(
            books = books,
            categoryId = categoryId,
            categoryIsSystem = categoryIsSystem,
            knownCategoryIds = knownCategoryIds,
            uncategorizedCategoryId = uncategorizedCategoryId,
            searchQuery = searchQuery,
            bookTitle = bookTitle,
            bookCategoryIds = bookCategoryIds,
        )
        val comparator = bookComparator(
            sortMode = sortMode,
            bookId = bookId,
            bookTitle = bookTitle,
            bookDateAdded = bookDateAdded,
            bookLastRead = bookLastRead,
        )

        return filteredBooks.sortedWith(
            if (sortDescending) {
                comparator.reversed()
            } else {
                comparator
            },
        )
    }

    fun <T> filterBooksForCategory(
        books: List<T>,
        categoryId: String,
        categoryIsSystem: Boolean,
        knownCategoryIds: Set<String>,
        uncategorizedCategoryId: String,
        searchQuery: String?,
        bookTitle: (T) -> String?,
        bookCategoryIds: (T) -> Collection<String>,
    ): List<T> {
        return books
            .filter { matchesSearch(bookTitle(it), searchQuery) }
            .filter { book ->
                val categoryIds = normalizedCategoryIds(
                    categoryIds = bookCategoryIds(book),
                    knownCategoryIds = knownCategoryIds,
                    uncategorizedCategoryId = uncategorizedCategoryId,
                )

                if (categoryIsSystem) {
                    categoryIds.isEmpty() || uncategorizedCategoryId in categoryIds
                } else {
                    categoryId in categoryIds
                }
            }
    }

    fun normalizedCategoryIds(
        categoryIds: Collection<String>,
        knownCategoryIds: Set<String>,
        uncategorizedCategoryId: String,
    ): List<String> {
        val normalizedIds = NovelCategoryPolicy.normalizeCategoryIds(
            categoryIds = categoryIds,
            uncategorizedCategoryId = uncategorizedCategoryId,
        )

        return if (normalizedIds.any { it != uncategorizedCategoryId }) {
            normalizedIds.filter { it in knownCategoryIds }
        } else {
            normalizedIds
        }
    }

    fun matchesSearch(
        title: String?,
        searchQuery: String?,
    ): Boolean {
        return searchQuery.isNullOrBlank() || title?.contains(searchQuery, ignoreCase = true) == true
    }

    private fun <T> bookComparator(
        sortMode: NovelLibrarySortMode,
        bookId: (T) -> String,
        bookTitle: (T) -> String?,
        bookDateAdded: (T) -> Long,
        bookLastRead: (T) -> Long,
    ): Comparator<T> {
        return when (sortMode) {
            NovelLibrarySortMode.ALPHABETICAL -> compareBy(
                { bookTitle(it)?.lowercase().orEmpty() },
                bookId,
            )
            NovelLibrarySortMode.DATE_ADDED -> compareBy(
                bookDateAdded,
                { bookTitle(it)?.lowercase().orEmpty() },
                bookId,
            )
            NovelLibrarySortMode.LAST_READ -> compareBy(
                bookLastRead,
                { bookTitle(it)?.lowercase().orEmpty() },
                bookId,
            )
        }
    }
}
