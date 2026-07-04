package tachiyomi.domain.release.model

import tachiyomi.domain.release.service.AppUpdatePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ReleaseTest {
    @Test
    fun applicationReleaseArgumentsDefaultToCachedChecks() {
        val arguments = ApplicationReleaseArguments(
            isFoss = false,
            isPreview = true,
            commitCount = 100,
            versionName = "r100",
            repository = "owner/repo",
        )

        assertFalse(arguments.forceCheck)
    }

    @Test
    fun releaseDefaultsToStablePublishedBuild() {
        val release = Release(
            version = "v1.0.0",
            info = "Notes",
            releaseLink = "https://example.com/release",
            downloadLink = "https://example.com/download",
        )

        assertFalse(release.preRelease)
        assertFalse(release.draft)
    }

    @Test
    fun appUpdatePoliciesKeepStoredValuesStable() {
        assertEquals("wifi", AppUpdatePolicy.DEVICE_ONLY_ON_WIFI)
        assertEquals("network_not_metered", AppUpdatePolicy.DEVICE_NETWORK_NOT_METERED)
        assertEquals("ac", AppUpdatePolicy.DEVICE_CHARGING)
        assertEquals("disable", AppUpdatePolicy.DISABLE_AUTO_DOWNLOAD)
    }
}
