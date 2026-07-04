package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourcePreferencePolicy

class ToggleSource(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source, enable: Boolean = isEnabled(source.id)) {
        await(source.id, enable)
    }

    fun await(sourceId: Long, enable: Boolean = isEnabled(sourceId)) {
        preferences.disabledSources().getAndSet { disabled ->
            SourcePreferencePolicy.setSourceEnabled(
                disabledSourceIds = disabled,
                sourceId = sourceId,
                enabled = enable,
            )
        }
    }

    fun await(sourceIds: List<Long>, enable: Boolean) {
        preferences.disabledSources().getAndSet { disabled ->
            SourcePreferencePolicy.setSourceIdsEnabled(
                disabledSourceIds = disabled,
                sourceIds = sourceIds,
                enabled = enable,
            )
        }
    }

    private fun isEnabled(sourceId: Long): Boolean {
        return sourceId.toString() in preferences.disabledSources().get()
    }
}
