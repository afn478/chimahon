package tachiyomi.domain.source.service

data class SourceCategoryPreference(
    val sourceId: Long,
    val category: String,
)

object SourceCategoryPolicy {
    fun canCreateCategory(category: String): Boolean {
        return SOURCE_CATEGORY_SEPARATOR !in category
    }

    fun addCategory(
        categories: Set<String>,
        category: String,
    ): Set<String> {
        return categories + category
    }

    fun deleteCategory(
        categories: Set<String>,
        category: String,
    ): Set<String> {
        return categories - category
    }

    fun deleteSourceCategoryPreferences(
        sourceCategoryPreferences: Set<String>,
        category: String,
    ): Set<String> {
        return sourceCategoryPreferences
            .filterNot { encodedPreference -> encodedPreference.substringAfter(SOURCE_CATEGORY_SEPARATOR) == category }
            .toSet()
    }

    fun renameCategory(
        categories: Set<String>,
        oldCategory: String,
        newCategory: String,
    ): Set<String> {
        return categories - oldCategory + newCategory
    }

    fun renameSourceCategoryPreferences(
        sourceCategoryPreferences: Set<String>,
        oldCategory: String,
        newCategory: String,
    ): Set<String> {
        return sourceCategoryPreferences
            .map { encodedPreference ->
                val separatorIndex = encodedPreference.indexOf(SOURCE_CATEGORY_SEPARATOR)
                if (separatorIndex != -1 && encodedPreference.substring(separatorIndex + 1) == oldCategory) {
                    encodedPreference.substring(0, separatorIndex + 1) + newCategory
                } else {
                    encodedPreference
                }
            }
            .toSet()
    }

    fun setCategoriesForSource(
        sourceCategoryPreferences: Set<String>,
        sourceId: Long,
        categories: List<String>,
    ): Set<String> {
        val sourceIdString = sourceId.toString()
        val currentSourceCategories = sourceCategoryPreferences.filterNot {
            it.substringBefore(SOURCE_CATEGORY_SEPARATOR) == sourceIdString
        }
        val newSourceCategories = currentSourceCategories + categories.map {
            encodeSourceCategoryPreference(sourceId, it)
        }

        return newSourceCategories.toSet()
    }

    fun parseSourceCategoryPreferences(sourceCategoryPreferences: Set<String>): List<SourceCategoryPreference> {
        return sourceCategoryPreferences.map {
            val (sourceId, category) = it.split(SOURCE_CATEGORY_SEPARATOR)
            SourceCategoryPreference(sourceId.toLong(), category)
        }
    }

    fun sortCategories(
        categories: Set<String>,
        compareNames: (String, String) -> Int = { left, right -> left.compareTo(right, ignoreCase = true) },
    ): List<String> {
        return categories.sortedWith { left, right -> compareNames(left, right) }
    }

    private fun encodeSourceCategoryPreference(
        sourceId: Long,
        category: String,
    ): String {
        return "$sourceId$SOURCE_CATEGORY_SEPARATOR$category"
    }

    private const val SOURCE_CATEGORY_SEPARATOR = "|"
}
