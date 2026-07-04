package tachiyomi.domain.library.service

object LibraryUpdateCategoryPolicy {
    fun shouldInclude(
        categoryIds: Collection<Long>,
        includedCategoryIds: Collection<Long>,
        excludedCategoryIds: Collection<Long>,
    ): Boolean {
        val included = includedCategoryIds.isEmpty() || categoryIds.any { it in includedCategoryIds }
        val excluded = categoryIds.any { it in excludedCategoryIds }
        return included && !excluded
    }
}
