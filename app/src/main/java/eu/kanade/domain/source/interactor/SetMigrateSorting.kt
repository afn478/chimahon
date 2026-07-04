package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.domain.source.model.MigrationSourceSortDirection
import tachiyomi.domain.source.model.MigrationSourceSortMode

class SetMigrateSorting(
    private val preferences: SourcePreferences,
) {

    fun await(mode: MigrationSourceSortMode, direction: MigrationSourceSortDirection) {
        preferences.migrationSortingMode().set(mode)
        preferences.migrationSortingDirection().set(direction)
    }
}
