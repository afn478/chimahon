package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source

object SourceCatalogPolicy {
    fun selectEnabledSources(
        sources: List<Source>,
        enabledLanguages: Set<String>,
        disabledSourceIds: Set<String>,
        hiddenSourceIds: Set<Long>,
        pinnedSourceIds: Set<String>,
        lastUsedSourceId: Long,
        dataSaverExcludedSourceIds: Set<String>,
        sourceCategoryPreferences: Set<String>,
        sourceCategoriesFilterEnabled: Boolean,
        isLocalSource: (Source) -> Boolean,
    ): List<Source> {
        val sourceCategoriesBySourceId = SourceCategoryPolicy.parseSourceCategoryPreferences(sourceCategoryPreferences)
            .groupBy(
                keySelector = SourceCategoryPreference::sourceId,
                valueTransform = SourceCategoryPreference::category,
            )
        val sourcesInSourceCategories = sourceCategoriesBySourceId.keys

        return sources
            .filter { it.lang in enabledLanguages || isLocalSource(it) }
            .filterNot { it.id.toString() in disabledSourceIds || it.id in hiddenSourceIds }
            .sortedWith { left, right -> left.name.compareTo(right.name, ignoreCase = true) }
            .flatMap { source ->
                val pin = if (source.id.toString() in pinnedSourceIds) Pins.pinned else Pins.unpinned
                val categories = sourceCategoriesBySourceId[source.id].orEmpty().toSet()
                val visibleSource = source.copy(
                    pin = pin,
                    isExcludedFromDataSaver = source.id.toString() in dataSaverExcludedSourceIds,
                    categories = categories,
                )
                val rows = buildList {
                    add(visibleSource)

                    if (source.id == lastUsedSourceId) {
                        add(visibleSource.copy(isUsedLast = true, pin = visibleSource.pin - Pin.Actual))
                    }

                    categories.forEach { category ->
                        add(visibleSource.copy(category = category, pin = visibleSource.pin - Pin.Actual))
                    }
                }

                if (
                    sourceCategoriesFilterEnabled &&
                    Pin.Actual !in rows.first().pin &&
                    source.id in sourcesInSourceCategories
                ) {
                    rows.drop(1)
                } else {
                    rows
                }
            }
    }
}
