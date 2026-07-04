package app.chimahon.shared

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceRegistry
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.ScriptHttpSource
import eu.kanade.tachiyomi.source.online.resolveScriptSourceUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import tachiyomi.core.database.DesktopDatabaseDriverFactory
import tachiyomi.core.platform.background.BackgroundTaskScheduler
import tachiyomi.core.platform.background.BackgroundWorkerRegistry
import tachiyomi.core.platform.background.CoroutineBackgroundTaskScheduler
import tachiyomi.core.platform.background.DesktopBackgroundTaskConstraintMonitor
import tachiyomi.core.platform.javascript.DesktopJavaScriptRuntimeFactory
import tachiyomi.core.platform.javascript.JavaScriptRuntimeFactory
import tachiyomi.core.platform.storage.DesktopPlatformStorageDirectories
import tachiyomi.core.platform.storage.PlatformStorageDirectories
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.DesktopDatabaseHandler

internal actual class ChimahonPlatformServices actual constructor() {
    actual val platformName: String = "Desktop"
    actual val backgroundState: String = "Coroutine scheduler ready"
    actual val storageDirectories: PlatformStorageDirectories = DesktopPlatformStorageDirectories(APP_NAME)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        storageDirectories.ensureChimahonDirectories()
    }

    actual val sourceRegistry: SourceRegistry = SourceRegistry()
    actual val apkExtensionManager = ChimahonPlatformApkExtensionManager(storageDirectories, sourceRegistry)
    private val databaseDriver = DesktopDatabaseDriverFactory(
        databaseDirectory = storageDirectories.filesDir / DATABASE_DIRECTORY,
    ).create(Database.Schema, DATABASE_NAME)
    actual val database: Database = createDatabase(databaseDriver)
    actual val databaseHandler: DatabaseHandler = DesktopDatabaseHandler(database, databaseDriver)
    actual val javaScriptRuntimeFactory: JavaScriptRuntimeFactory = DesktopJavaScriptRuntimeFactory

    actual fun createBackgroundTaskScheduler(
        workerRegistry: BackgroundWorkerRegistry,
    ): BackgroundTaskScheduler {
        return CoroutineBackgroundTaskScheduler(
            workerRegistry = workerRegistry,
            scope = backgroundScope,
            constraintMonitor = DesktopBackgroundTaskConstraintMonitor(),
        )
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun resolveExternalMangaUrl(source: CatalogueSource, manga: SManga): String? {
        return when (source) {
            is ScriptHttpSource -> resolveScriptSourceUrl(source.baseUrl, manga.safeSourceUrl())
            is HttpSource -> runCatching { source.getMangaUrl(manga) }.getOrNull()
            else -> manga.url
        }
    }

    actual fun openExternalUrl(url: String): Boolean {
        return ChimahonPlatformIntegration.openExternalUrl(url)
    }

    actual fun close() {
        backgroundScope.cancel()
        databaseDriver.close()
    }
}
