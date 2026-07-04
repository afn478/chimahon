package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source

data class SourceListGroup(
    val key: String,
    val header: String,
    val isCategory: Boolean,
    val sources: List<Source>,
)

object SourceListGroupingPolicy {
    const val PINNED_KEY = "pinned"
    const val LAST_USED_KEY = "last_used"
    const val CATEGORY_KEY_PREFIX = "category-"

    fun groupSources(sources: List<Source>): List<SourceListGroup> {
        return sources
            .groupBy(::groupKey)
            .map { (key, groupedSources) ->
                SourceListGroup(
                    key = key,
                    header = key.removePrefix(CATEGORY_KEY_PREFIX),
                    isCategory = key.startsWith(CATEGORY_KEY_PREFIX),
                    sources = groupedSources,
                )
            }
            .sortedWith { left, right -> compareGroupKeys(left.key, right.key) }
    }

    private fun groupKey(source: Source): String {
        return when {
            source.category != null -> "$CATEGORY_KEY_PREFIX${source.category}"
            source.isUsedLast -> LAST_USED_KEY
            Pin.Actual in source.pin -> PINNED_KEY
            else -> source.lang
        }
    }

    private fun compareGroupKeys(left: String, right: String): Int {
        return when {
            left == LAST_USED_KEY && right != LAST_USED_KEY -> -1
            right == LAST_USED_KEY && left != LAST_USED_KEY -> 1
            left == PINNED_KEY && right != PINNED_KEY -> -1
            right == PINNED_KEY && left != PINNED_KEY -> 1
            left.startsWith(CATEGORY_KEY_PREFIX) && !right.startsWith(CATEGORY_KEY_PREFIX) -> -1
            right.startsWith(CATEGORY_KEY_PREFIX) && !left.startsWith(CATEGORY_KEY_PREFIX) -> 1
            left == "" && right != "" -> 1
            right == "" && left != "" -> -1
            else -> left.compareTo(right)
        }
    }
}
