package tachiyomi.domain.release.model

data class ApplicationReleaseArguments(
    val isFoss: Boolean,
    /** If current version is Preview (beta) build */
    val isPreview: Boolean,
    /** Commit count of current version */
    val commitCount: Int,
    /** Current version name, could be version tag (v0.1.2) or commit count (r1234) */
    val versionName: String,
    /** Repository name */
    val repository: String,
    /** Force check for new update */
    val forceCheck: Boolean = false,
)
