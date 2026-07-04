package tachiyomi.core.platform.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

class NativePlatformStorageDirectories(
    private val appName: String = "chimahon",
    homeDir: String = environment("HOME") ?: environment("USERPROFILE") ?: ".",
    tempDir: String = environment("TMPDIR") ?: environment("TEMP") ?: "/tmp",
) : PlatformStorageDirectories {

    private val homePath = homeDir.toPath()

    override val cacheDir: Path = defaultCacheRoot(homePath).resolveDirectoryName(appName)

    override val filesDir: Path = defaultDataRoot(homePath).resolveDirectoryName(appName)

    override val temporaryDir: Path = tempDir.toPath().resolveDirectoryName(appName)

    override fun defaultDownloadsDir(appName: String): Path {
        return (homePath / "Downloads").resolveDirectoryName(appName)
    }

    override fun fileUri(path: Path): String {
        return encodedFileUri(path)
    }

    private fun defaultDataRoot(homePath: Path): Path {
        return environment("XDG_DATA_HOME")?.toPath() ?: homePath / ".local" / "share"
    }

    private fun defaultCacheRoot(homePath: Path): Path {
        return environment("XDG_CACHE_HOME")?.toPath() ?: homePath / ".cache"
    }

    private companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun environment(name: String): String? = getenv(name)?.toKString()
    }
}
