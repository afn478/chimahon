package tachiyomi.domain.category.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryInteractorTest {
    @Test
    fun reorderCategoryIgnoresSystemCategoryWhenAssigningOrder() = runTest {
        val repository = FakeCategoryRepository(
            category(id = Category.UNCATEGORIZED_ID, name = "All", order = -1),
            category(id = 1, name = "Reading", order = 0),
            category(id = 2, name = "Backlog", order = 1),
            category(id = 3, name = "Done", order = 2),
        )
        val reorder = ReorderCategory(repository)

        val result = reorder.await(category(id = 3, name = "Done", order = 2), newIndex = 0)

        assertEquals(ReorderCategory.Result.Success, result)
        assertEquals(listOf(3L, 1L, 2L), repository.userCategories().map(Category::id))
        assertEquals(listOf(0L, 1L, 2L), repository.userCategories().map(Category::order))
        assertEquals(-1L, repository.get(Category.UNCATEGORIZED_ID)?.order)
    }

    @Test
    fun reorderCategoryReturnsUnchangedWhenCategoryIsMissing() = runTest {
        val repository = FakeCategoryRepository(category(id = 1, name = "Reading", order = 0))
        val reorder = ReorderCategory(repository)

        val result = reorder.await(category(id = 99, name = "Missing", order = 0), newIndex = 0)

        assertEquals(ReorderCategory.Result.Unchanged, result)
        assertEquals(emptyList(), repository.appliedUpdates)
    }

    @Test
    fun renameHideAndUpdateCategoryApplyPartialUpdates() = runTest {
        val repository = FakeCategoryRepository(category(id = 1, name = "Reading", order = 0))

        assertEquals(RenameCategory.Result.Success, RenameCategory(repository).await(1, "Favorites"))
        assertEquals(HideCategory.Result.Success, HideCategory(repository).await(repository.get(1)!!))
        assertEquals(
            UpdateCategory.Result.Success,
            UpdateCategory(repository).await(CategoryUpdate(id = 1, flags = 42L)),
        )

        assertEquals(
            category(id = 1, name = "Favorites", order = 0, flags = 42L, hidden = true),
            repository.get(1),
        )
    }
}

private fun category(
    id: Long,
    name: String,
    order: Long,
    flags: Long = 0,
    hidden: Boolean = false,
): Category {
    return Category(
        id = id,
        name = name,
        order = order,
        flags = flags,
        hidden = hidden,
    )
}

private class FakeCategoryRepository(
    vararg categories: Category,
) : CategoryRepository {
    private val categories = MutableStateFlow(categories.associateBy(Category::id))
    val appliedUpdates = mutableListOf<CategoryUpdate>()

    fun userCategories(): List<Category> {
        return categories.value.values
            .filterNot(Category::isSystemCategory)
            .sortedBy(Category::order)
    }

    override suspend fun get(id: Long): Category? {
        return categories.value[id]
    }

    override suspend fun getAll(): List<Category> {
        return categories.value.values.sortedBy(Category::order)
    }

    override fun getAllAsFlow(): Flow<List<Category>> {
        return categories.map { entries -> entries.values.sortedBy(Category::order) }
    }

    override suspend fun getCategoriesByMangaId(mangaId: Long): List<Category> = getAll()

    override fun getCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>> = getAllAsFlow()

    override suspend fun insert(category: Category): Long {
        val id = category.id.takeIf { it != 0L } ?: ((categories.value.keys.maxOrNull() ?: 0L) + 1L)
        categories.value = categories.value + (id to category.copy(id = id))
        return id
    }

    override suspend fun updatePartial(update: CategoryUpdate) {
        updatePartial(listOf(update))
    }

    override suspend fun updatePartial(updates: List<CategoryUpdate>) {
        appliedUpdates += updates
        categories.value = categories.value.mapValues { (_, category) ->
            updates.firstOrNull { it.id == category.id }?.let { update ->
                category.copy(
                    name = update.name ?: category.name,
                    order = update.order ?: category.order,
                    flags = update.flags ?: category.flags,
                    hidden = update.hidden ?: category.hidden,
                )
            } ?: category
        }
    }

    override suspend fun updateAllFlags(flags: Long?) {
        categories.value = categories.value.mapValues { (_, category) ->
            category.copy(flags = flags ?: category.flags)
        }
    }

    override suspend fun delete(categoryId: Long) {
        categories.value = categories.value - categoryId
    }
}
