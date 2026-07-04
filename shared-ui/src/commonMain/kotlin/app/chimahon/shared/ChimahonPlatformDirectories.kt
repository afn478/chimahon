package app.chimahon.shared

import okio.FileSystem
import tachiyomi.core.platform.storage.PlatformStorageDirectories

internal fun PlatformStorageDirectories.ensureChimahonDirectories(
    fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    listOf(
        filesDir,
        filesDir / DATABASE_DIRECTORY,
        cacheDir,
        temporaryDir,
        defaultDownloadsDir(APP_NAME),
    ).forEach { path ->
        runCatching {
            fileSystem.createDirectories(path)
        }
    }
}
