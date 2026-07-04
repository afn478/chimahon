package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.service.SourcePreferencePolicy

class ToggleLanguage(
    val preferences: SourcePreferences,
) {

    fun await(language: String) {
        preferences.enabledLanguages().getAndSet { enabled ->
            SourcePreferencePolicy.toggleLanguage(enabled, language)
        }
    }
}
