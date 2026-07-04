package tachiyomi.domain.category.service

import tachiyomi.domain.category.model.Category

object CategoryCreationPolicy {
    fun nextOrder(categories: Iterable<Category>): Long {
        return categories.maxOfOrNull { it.order }?.plus(1) ?: 0
    }

    fun create(
        name: String,
        existingCategories: Iterable<Category>,
        flags: Long,
        hidden: Boolean = false,
    ): Category {
        return Category(
            id = 0,
            name = name,
            order = nextOrder(existingCategories),
            flags = flags,
            hidden = hidden,
        )
    }
}
