package tachiyomi.domain.release.interactor

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.release.model.ApplicationReleaseArguments
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

class GetApplicationRelease(
    private val service: ReleaseService,
    private val preferenceStore: PreferenceStore,
) {

    private val lastChecked: Preference<Long> by lazy {
        preferenceStore.getLong(
            Preference.appStateKey(ApplicationReleaseCheckPolicy.LAST_CHECKED_KEY),
            ApplicationReleaseCheckPolicy.LAST_CHECKED_DEFAULT,
        )
    }

    suspend fun await(arguments: ApplicationReleaseArguments): Result {
        val now = System.currentTimeMillis()

        // Limit checks to once every 2 days at most
        val shouldCheckForUpdate = ApplicationReleaseCheckPolicy.shouldCheckForUpdate(
            forceCheck = arguments.forceCheck,
            lastCheckedMillis = lastChecked.get(),
            nowMillis = now,
        )
        if (!shouldCheckForUpdate) {
            return Result.NoNewUpdate
        }

        // KMK -->
        val releases = service.releaseNotes(arguments)
            .filter {
                !it.preRelease &&
                    !it.draft &&
                    isNewReleaseVersion(
                        arguments.isPreview,
                        arguments.commitCount,
                        arguments.versionName,
                        it.version,
                    )
            }

        val latest = releases.getLatestRelease() ?: return Result.NoNewUpdate
        // KMK <--

        lastChecked.set(now)

        // Check if latest version is different from current version
        val isNewVersion = isNewReleaseVersion(
            isPreview = arguments.isPreview,
            commitCount = arguments.commitCount,
            versionName = arguments.versionName,
            versionTag = latest.version,
        )
        return when {
            isNewVersion -> Result.NewUpdate(latest)
            else -> Result.NoNewUpdate
        }
    }

    // KMK -->
    suspend fun awaitReleaseNotes(arguments: ApplicationReleaseArguments): Result {
        val releases = service.releaseNotes(arguments)
            .filter { !it.preRelease }

        val latest = releases.getLatestRelease() ?: return Result.NoNewUpdate
        return Result.NewUpdate(latest)
    }
    // KMK <--

    sealed interface Result {
        data class NewUpdate(val release: Release) : Result
        data object NoNewUpdate : Result
        data object OsTooOld : Result
    }
}
