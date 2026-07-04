package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.MigrationSourceSortPolicy
import tachiyomi.source.local.isLocal

class GetSourcesWithFavoriteCount(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Pair<Source, Long>>> {
        return combine(
            preferences.migrationSortingDirection().changes(),
            preferences.migrationSortingMode().changes(),
            repository.getSourcesWithFavoriteCount(),
        ) { direction, mode, list ->
            MigrationSourceSortPolicy.sortSourcesWithFavoriteCount(
                sourcesWithCount = list,
                mode = mode,
                direction = direction,
                compareNames = { left, right ->
                    left.lowercase().compareToWithCollator(right.lowercase())
                },
                isLocalSource = { it.isLocal() },
            )
        }
    }
}
