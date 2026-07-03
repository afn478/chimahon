package app.chimahon.shared

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceRegistry
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ScriptHttpSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okio.FileSystem
import platform.posix.time
import tachiyomi.core.database.NativeDatabaseDriverFactory
import tachiyomi.core.platform.background.BackgroundTaskScheduler
import tachiyomi.core.platform.background.BackgroundWorkerRegistry
import tachiyomi.core.platform.background.CoroutineBackgroundTaskScheduler
import tachiyomi.core.platform.javascript.IosJavaScriptRuntimeFactory
import tachiyomi.core.platform.javascript.JavaScriptRuntimeFactory
import tachiyomi.core.platform.storage.IosPlatformStorageDirectories
import tachiyomi.core.platform.storage.PlatformStorageDirectories
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.NativeDatabaseHandler

internal actual class ChimahonPlatformServices actual constructor() {
    actual val platformName: String = "iOS"
    actual val backgroundState: String = "Foreground scheduler; iOS background work follows system limits"
    actual val storageDirectories: PlatformStorageDirectories = IosPlatformStorageDirectories(APP_NAME)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        storageDirectories.ensureChimahonDirectories()
    }

    actual val sourceRegistry: SourceRegistry = SourceRegistry()
    actual val apkExtensionManager = ChimahonPlatformApkExtensionManager(storageDirectories, sourceRegistry)
    private val databaseDirectory = storageDirectories.filesDir / DATABASE_DIRECTORY
    private val databaseDriver = NativeDatabaseDriverFactory().create(
        schema = Database.Schema,
        name = run {
            FileSystem.SYSTEM.createDirectories(databaseDirectory)
            (databaseDirectory / DATABASE_NAME).toString()
        },
    )
    actual val database: Database = createDatabase(databaseDriver)
    actual val databaseHandler: DatabaseHandler = NativeDatabaseHandler(database, databaseDriver)
    actual val javaScriptRuntimeFactory: JavaScriptRuntimeFactory = IosJavaScriptRuntimeFactory

    actual fun createBackgroundTaskScheduler(
        workerRegistry: BackgroundWorkerRegistry,
    ): BackgroundTaskScheduler {
        return CoroutineBackgroundTaskScheduler(
            workerRegistry = workerRegistry,
            scope = backgroundScope,
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun currentTimeMillis(): Long {
        return time(null) * 1_000L
    }

    actual fun resolveExternalMangaUrl(source: CatalogueSource, manga: SManga): String? {
        val mangaUrl = manga.safeSourceUrl()
        return when (source) {
            is ScriptHttpSource -> resolveScriptMangaUrl(source.baseUrl, mangaUrl)
            else -> mangaUrl
        }
    }

    actual fun openExternalUrl(url: String): Boolean {
        return ChimahonPlatformIntegration.openExternalUrl(url)
    }

    actual fun close() {
        backgroundScope.cancel()
        apkExtensionManager.close()
        databaseDriver.close()
    }
}

private fun resolveScriptMangaUrl(baseUrl: String, path: String): String? {
    val candidate = path.trim()
    if (candidate.isBlank()) return null
    if (candidate.hasUrlScheme()) return candidate
    if (candidate.startsWith("//")) return "https:$candidate"

    val root = baseUrl.trimEnd('/')
    if (root.isBlank()) return candidate
    return if (candidate.startsWith("/")) {
        val schemeSplit = root.indexOf("://")
        if (schemeSplit == -1) {
            "$root$candidate"
        } else {
            val hostStart = schemeSplit + 3
            val hostEnd = root.indexOf('/', startIndex = hostStart).takeIf { it >= 0 } ?: root.length
            root.take(hostEnd) + candidate
        }
    } else {
        "$root/${candidate.trimStart('/')}"
    }
}

private fun SManga.safeSourceUrl(): String {
    return runCatching { url }
        .getOrNull()
        .orEmpty()
}

private fun String.hasUrlScheme(): Boolean {
    val colon = indexOf(':')
    if (colon <= 0) return false
    val scheme = take(colon)
    return scheme.first().isLetter() &&
        scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }
}
