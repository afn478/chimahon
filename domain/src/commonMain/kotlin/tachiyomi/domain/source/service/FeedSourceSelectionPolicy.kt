package tachiyomi.domain.source.service

object FeedSourceSelectionPolicy {
    fun <T> selectEnabledSources(
        sources: List<T>,
        enabledLanguages: Set<String>,
        disabledSourceIds: Set<Long>,
        pinnedSourceIds: Set<String>,
        sourceId: (T) -> Long,
        sourceLanguage: (T) -> String,
        sourceName: (T) -> String,
    ): List<T> {
        return sources
            .filter { sourceLanguage(it) in enabledLanguages }
            .filterNot { sourceId(it) in disabledSourceIds }
            .sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { source ->
                    "(${sourceLanguage(source)}) ${sourceName(source)}"
                },
            )
            .sortedBy { sourceId(it).toString() !in pinnedSourceIds }
    }
}
