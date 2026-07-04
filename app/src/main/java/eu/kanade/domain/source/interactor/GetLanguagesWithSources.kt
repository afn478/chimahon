package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.util.system.LocaleHelper
import exh.source.BlacklistedSources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceLanguagePolicy

class GetLanguagesWithSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<Map<String, List<Source>>> {
        return combine(
            preferences.enabledLanguages().changes(),
            preferences.disabledSources().changes(),
            repository.getOnlineSources(),
        ) { enabledLanguage, disabledSource, onlineSources ->
            SourceLanguagePolicy.groupOnlineSourcesByLanguage(
                onlineSources = onlineSources,
                enabledLanguages = enabledLanguage,
                disabledSourceIds = disabledSource,
                hiddenSourceIds = BlacklistedSources.HIDDEN_SOURCES,
                compareLanguages = LocaleHelper.comparator,
                compareSourceNames = { left, right -> String.CASE_INSENSITIVE_ORDER.compare(left, right) },
            ).associate { group ->
                group.language to group.sources
            }
        }
    }
}
