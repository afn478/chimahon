package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourcePreferencePolicy

class ToggleExcludeFromDataSaver(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source) {
        preferences.dataSaverExcludedSources().getAndSet {
            SourcePreferencePolicy.toggleDataSaverExcludedSource(it, source.id)
        }
    }
}
