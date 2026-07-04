package eu.kanade.domain.extension.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.extension.service.ExtensionSourcePolicy

class GetExtensionSources(
    private val preferences: SourcePreferences,
) {

    fun subscribe(extension: Extension.Installed): Flow<List<ExtensionSourceItem>> {
        return preferences.disabledSources().changes().map { disabledSources ->
            ExtensionSourcePolicy.selectSources(
                sources = extension.sources,
                disabledSourceIds = disabledSources,
                sourceId = Source::id,
                sourceName = Source::name,
            )
                .map { selection ->
                    ExtensionSourceItem(
                        source = selection.source,
                        enabled = selection.enabled,
                        labelAsName = selection.labelAsName,
                    )
                }
        }
    }
}

data class ExtensionSourceItem(
    val source: Source,
    val enabled: Boolean,
    val labelAsName: Boolean,
)
