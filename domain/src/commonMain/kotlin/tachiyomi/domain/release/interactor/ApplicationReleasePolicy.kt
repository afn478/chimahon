package tachiyomi.domain.release.interactor

import tachiyomi.domain.release.model.Release

object ApplicationReleaseCheckPolicy {
    const val LAST_CHECKED_KEY = "last_app_check"
    const val LAST_CHECKED_DEFAULT = 0L
    const val CHECK_INTERVAL_MILLIS = 2L * 24 * 60 * 60 * 1000

    fun shouldCheckForUpdate(
        forceCheck: Boolean,
        lastCheckedMillis: Long,
        nowMillis: Long,
    ): Boolean {
        return forceCheck || nowMillis - lastCheckedMillis >= CHECK_INTERVAL_MILLIS
    }
}

fun isNewReleaseVersion(
    isPreview: Boolean,
    commitCount: Int,
    versionName: String,
    versionTag: String,
): Boolean {
    // Removes prefixes like "r" or "v"
    val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")
    return if (isPreview) {
        // Preview builds: based on releases in "chimahon/chimahon-preview" repo
        // tagged as something like "r1234"
        newVersion.toIntOrNull()?.let { it > commitCount } ?: false
    } else {
        // Release builds: based on releases in "chimahon/chimahon" repo
        // tagged as something like "v0.1.2"
        val oldVersion = versionName.replace("[^\\d.]".toRegex(), "")

        val newSemVer = parseSemVerParts(newVersion)
        val oldSemVer = parseSemVerParts(oldVersion)
        val compareLength = maxOf(newSemVer.size, oldSemVer.size)

        for (index in 0 until compareLength) {
            val newPart = newSemVer.getOrElse(index) { 0 }
            val oldPart = oldSemVer.getOrElse(index) { 0 }
            if (newPart > oldPart) {
                return true
            }
            if (newPart < oldPart) return false
        }

        false
    }
}

fun List<Release>.getLatestRelease(): Release? {
    return firstOrNull()
        ?.copy(
            info = joinToString("\r-----\r") {
                "## ${it.version}\r\r" +
                    it.info
            },
        )
}

private fun parseSemVerParts(version: String): List<Int> {
    return version
        .split(".")
        .mapNotNull { it.toIntOrNull() }
}
