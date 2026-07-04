package app.chimahon.shared

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceRegistry
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ScriptHttpSource
import eu.kanade.tachiyomi.source.online.resolveScriptSourceUrl
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
    private var backgroundStateLabel =
        "Foreground scheduler; register BGTask identifiers for system wakeups"
    actual val backgroundState: String
        get() = backgroundStateLabel
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
        ChimahonIosBackgroundTasks.schedulerOrRegister()?.let { scheduler ->
            backgroundStateLabel = "BGTaskScheduler bridge ready"
            return scheduler
        }

        backgroundStateLabel =
            "Foreground scheduler; add ${ChimahonIosBackgroundTasks.downloadQueueIdentifier} to Info.plist"
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
            is ScriptHttpSource -> resolveScriptSourceUrl(source.baseUrl, mangaUrl)
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
