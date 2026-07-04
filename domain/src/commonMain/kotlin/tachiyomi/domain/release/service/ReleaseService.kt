package tachiyomi.domain.release.service

import tachiyomi.domain.release.model.ApplicationReleaseArguments
import tachiyomi.domain.release.model.Release

interface ReleaseService {

    suspend fun latest(arguments: ApplicationReleaseArguments): Release?

    // KMK -->
    suspend fun releaseNotes(arguments: ApplicationReleaseArguments): List<Release>
    // KMK <--
}
