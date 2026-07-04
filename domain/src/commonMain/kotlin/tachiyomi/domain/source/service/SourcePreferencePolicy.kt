package tachiyomi.domain.source.service

object SourcePreferencePolicy {
    fun toggleLanguage(
        enabledLanguages: Set<String>,
        language: String,
    ): Set<String> {
        return toggleStringValue(enabledLanguages, language)
    }

    fun setSourceEnabled(
        disabledSourceIds: Set<String>,
        sourceId: Long,
        enabled: Boolean,
    ): Set<String> {
        return setSourceIdsEnabled(
            disabledSourceIds = disabledSourceIds,
            sourceIds = listOf(sourceId),
            enabled = enabled,
        )
    }

    fun setSourceIdsEnabled(
        disabledSourceIds: Set<String>,
        sourceIds: List<Long>,
        enabled: Boolean,
    ): Set<String> {
        val sourceIdStrings = sourceIds.map(Long::toString)
        return if (enabled) {
            disabledSourceIds - sourceIdStrings
        } else {
            disabledSourceIds + sourceIdStrings
        }
    }

    fun togglePinnedSource(
        pinnedSourceIds: Set<String>,
        sourceId: Long,
    ): Set<String> {
        return toggleSourceIdStringValue(pinnedSourceIds, sourceId)
    }

    fun toggleDataSaverExcludedSource(
        excludedSourceIds: Set<String>,
        sourceId: Long,
    ): Set<String> {
        return toggleSourceIdStringValue(excludedSourceIds, sourceId)
    }

    private fun toggleSourceIdStringValue(
        values: Set<String>,
        sourceId: Long,
    ): Set<String> {
        return toggleStringValue(values, sourceId.toString())
    }

    private fun toggleStringValue(
        values: Set<String>,
        value: String,
    ): Set<String> {
        return if (value in values) {
            values - value
        } else {
            values + value
        }
    }
}
