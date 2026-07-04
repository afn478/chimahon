package tachiyomi.domain.extension.service

data class ExtensionSourceSelection<T>(
    val source: T,
    val enabled: Boolean,
    val labelAsName: Boolean,
)

object ExtensionSourcePolicy {
    fun <T> selectSources(
        sources: List<T>,
        disabledSourceIds: Set<String>,
        sourceId: (T) -> Long,
        sourceName: (T) -> String,
    ): List<ExtensionSourceSelection<T>> {
        val isMultiSource = sources.size > 1
        val isMultiLangSingleSource =
            isMultiSource && sources.map(sourceName).distinct().size == 1
        val labelAsName = isMultiSource && !isMultiLangSingleSource

        return sources.map { source ->
            ExtensionSourceSelection(
                source = source,
                enabled = sourceId(source).toString() !in disabledSourceIds,
                labelAsName = labelAsName,
            )
        }
    }
}
