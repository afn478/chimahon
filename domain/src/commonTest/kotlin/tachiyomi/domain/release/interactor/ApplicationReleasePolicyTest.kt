package tachiyomi.domain.release.interactor

import tachiyomi.domain.release.model.Release
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationReleasePolicyTest {
    @Test
    fun releaseCheckPreferenceContractRemainsStable() {
        assertEquals("last_app_check", ApplicationReleaseCheckPolicy.LAST_CHECKED_KEY)
        assertEquals(0L, ApplicationReleaseCheckPolicy.LAST_CHECKED_DEFAULT)
        assertEquals(172_800_000L, ApplicationReleaseCheckPolicy.CHECK_INTERVAL_MILLIS)
    }

    @Test
    fun releaseChecksAreSkippedUntilTheCacheIntervalExpires() {
        val now = 1_000_000_000L
        val lastChecked = now - ApplicationReleaseCheckPolicy.CHECK_INTERVAL_MILLIS + 1

        assertFalse(
            ApplicationReleaseCheckPolicy.shouldCheckForUpdate(
                forceCheck = false,
                lastCheckedMillis = lastChecked,
                nowMillis = now,
            ),
        )
    }

    @Test
    fun releaseChecksRunWhenForcedOrCacheIntervalHasExpired() {
        val now = 1_000_000_000L

        assertTrue(
            ApplicationReleaseCheckPolicy.shouldCheckForUpdate(
                forceCheck = false,
                lastCheckedMillis = now - ApplicationReleaseCheckPolicy.CHECK_INTERVAL_MILLIS,
                nowMillis = now,
            ),
        )
        assertTrue(
            ApplicationReleaseCheckPolicy.shouldCheckForUpdate(
                forceCheck = true,
                lastCheckedMillis = now,
                nowMillis = now,
            ),
        )
    }

    @Test
    fun previewVersionsCompareAgainstCommitCount() {
        assertTrue(
            isNewReleaseVersion(
                isPreview = true,
                commitCount = 100,
                versionName = "r100",
                versionTag = "r101",
            ),
        )
        assertFalse(
            isNewReleaseVersion(
                isPreview = true,
                commitCount = 100,
                versionName = "r100",
                versionTag = "r100",
            ),
        )
        assertFalse(
            isNewReleaseVersion(
                isPreview = true,
                commitCount = 100,
                versionName = "r100",
                versionTag = "preview",
            ),
        )
    }

    @Test
    fun stableVersionsCompareAgainstVersionName() {
        assertTrue(
            isNewReleaseVersion(
                isPreview = false,
                commitCount = 0,
                versionName = "v1.2.3",
                versionTag = "v1.3.0",
            ),
        )
        assertTrue(
            isNewReleaseVersion(
                isPreview = false,
                commitCount = 0,
                versionName = "v1.2.3",
                versionTag = "v1.2.4",
            ),
        )
        assertFalse(
            isNewReleaseVersion(
                isPreview = false,
                commitCount = 0,
                versionName = "v1.2.3",
                versionTag = "v1.2.3",
            ),
        )
        assertFalse(
            isNewReleaseVersion(
                isPreview = false,
                commitCount = 0,
                versionName = "v1.2.3",
                versionTag = "v1.2.2",
            ),
        )
    }

    @Test
    fun getLatestReleaseCombinesReleaseNotes() {
        val releases = listOf(
            release(version = "v2.0.0", info = "Latest notes"),
            release(version = "v1.0.0", info = "Previous notes"),
        )

        val latest = releases.getLatestRelease()

        assertEquals("v2.0.0", latest?.version)
        assertEquals(
            "## v2.0.0\r\rLatest notes\r-----\r## v1.0.0\r\rPrevious notes",
            latest?.info,
        )
    }
}

private fun release(
    version: String,
    info: String,
): Release {
    return Release(
        version = version,
        info = info,
        releaseLink = "https://example.com/$version",
        downloadLink = "https://example.com/$version.apk",
    )
}
