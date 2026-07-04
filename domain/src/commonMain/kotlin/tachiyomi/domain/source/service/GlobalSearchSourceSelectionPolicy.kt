package tachiyomi.domain.source.service

object GlobalSearchSourceSelectionPolicy {
    fun <T> selectEnabledSources(
        sources: List<T>,
        enabledLanguages: Set<String>,
        disabledSourceIds: Set<String>,
        pinnedSourceIds: Set<String>,
        sourceId: (T) -> Long,
        sourceLanguage: (T) -> String,
        sourceName: (T) -> String,
    ): List<T> {
        return sources
            .filter { sourceLanguage(it) in enabledLanguages }
            .filterNot { sourceId(it).toString() in disabledSourceIds }
            .sortedWith(sourceComparator(pinnedSourceIds, sourceId, sourceLanguage, sourceName))
    }

    fun <T> selectPinnedSources(
        sources: List<T>,
        pinnedSourceIds: Set<String>,
        sourceId: (T) -> Long,
    ): List<T> {
        return sources.filter { sourceId(it).toString() in pinnedSourceIds }
    }

    fun <T> hasPinnedSources(
        sources: List<T>,
        pinnedSourceIds: Set<String>,
        sourceId: (T) -> Long,
    ): Boolean {
        return sources.any { sourceId(it).toString() in pinnedSourceIds }
    }

    private fun <T> sourceComparator(
        pinnedSourceIds: Set<String>,
        sourceId: (T) -> Long,
        sourceLanguage: (T) -> String,
        sourceName: (T) -> String,
    ): Comparator<T> {
        return compareBy(
            { source -> sourceId(source).toString() !in pinnedSourceIds },
            { source -> "${sourceName(source).lowercase()} (${sourceLanguage(source)})" },
        )
    }
}
