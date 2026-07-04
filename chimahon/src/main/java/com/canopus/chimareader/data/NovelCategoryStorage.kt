package com.canopus.chimareader.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.domain.library.service.NovelCategoryPolicy
import java.io.File

class NovelCategoryStorage(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val categoriesFile: File
        get() = File(context.filesDir, "novel_categories.json")

    fun loadAllCategories(): List<NovelCategory> {
        val categories = if (!categoriesFile.exists()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<NovelCategory>>(categoriesFile.readText())
            } catch (e: Exception) {
                emptyList()
            }
        }

        val categoriesWithDefault = NovelCategoryPolicy.ensureDefaultCategory(
            categories = categories,
            defaultCategory = createDefaultCategory(),
            categoryId = NovelCategory::id,
            uncategorizedCategoryId = NovelCategory.UNCATEGORIZED_ID,
        )

        return sortCategories(categoriesWithDefault)
    }

    private fun createDefaultCategory() = NovelCategory(
        id = NovelCategory.UNCATEGORIZED_ID,
        name = "Default",
        order = NovelCategoryPolicy.SYSTEM_CATEGORY_ORDER,
    )

    fun saveCategories(categories: List<NovelCategory>) {
        categoriesFile.writeText(json.encodeToString(categories))
    }

    fun createCategory(name: String): NovelCategory {
        val categories = loadAllCategories().toMutableList()
        val nextOrder = NovelCategoryPolicy.nextUserCategoryOrder(
            categories = categories,
            isSystemCategory = NovelCategory::isSystemCategory,
            categoryOrder = NovelCategory::order,
        )
        val newCategory = NovelCategory(name = name, order = nextOrder)
        categories.add(newCategory)
        saveCategories(categories)
        return newCategory
    }

    fun deleteCategory(categoryId: String) {
        val categories = loadAllCategories().toMutableList()
        categories.removeAll { it.id == categoryId }
        saveCategories(categories)
    }

    fun updateCategory(category: NovelCategory) {
        val categories = loadAllCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            saveCategories(categories)
        }
    }

    private fun sortCategories(categories: List<NovelCategory>): List<NovelCategory> {
        return NovelCategoryPolicy.sortCategories(
            categories = categories,
            isSystemCategory = NovelCategory::isSystemCategory,
            categoryOrder = NovelCategory::order,
            categoryName = NovelCategory::name,
        )
    }
}
