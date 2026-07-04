package tachiyomi.domain.source.service

import tachiyomi.domain.source.model.Source

data class SourceLanguageGroup(
    val language: String,
    val sources: List<Source>,
)

object SourceLanguagePolicy {
    fun groupOnlineSourcesByLanguage(
        onlineSources: List<Source>,
        enabledLanguages: Set<String>,
        disabledSourceIds: Set<String>,
        hiddenSourceIds: Set<Long>,
        compareLanguages: (String, String) -> Int,
        compareSourceNames: (String, String) -> Int = { left, right -> left.compareTo(right, ignoreCase = true) },
    ): List<SourceLanguageGroup> {
        return onlineSources
            .filterNot { it.id in hiddenSourceIds }
            .sortedWith { left, right ->
                compareDisabledStatus(left, right, disabledSourceIds)
                    ?: compareSourceNames(left.name, right.name)
            }
            .groupBy(Source::lang)
            .map { (language, sources) ->
                SourceLanguageGroup(
                    language = language,
                    sources = sources,
                )
            }
            .sortedWith { left, right ->
                compareEnabledLanguageStatus(left.language, right.language, enabledLanguages)
                    ?: compareLanguages(left.language, right.language)
            }
    }

    private fun compareDisabledStatus(
        left: Source,
        right: Source,
        disabledSourceIds: Set<String>,
    ): Int? {
        val leftDisabled = left.id.toString() in disabledSourceIds
        val rightDisabled = right.id.toString() in disabledSourceIds

        return when {
            leftDisabled == rightDisabled -> null
            leftDisabled -> 1
            else -> -1
        }
    }

    private fun compareEnabledLanguageStatus(
        left: String,
        right: String,
        enabledLanguages: Set<String>,
    ): Int? {
        val leftEnabled = left in enabledLanguages
        val rightEnabled = right in enabledLanguages

        return when {
            leftEnabled == rightEnabled -> null
            leftEnabled -> -1
            else -> 1
        }
    }
}
