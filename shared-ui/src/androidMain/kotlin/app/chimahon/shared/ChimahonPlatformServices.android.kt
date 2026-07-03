package app.chimahon.shared

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceRegistry
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import tachiyomi.core.database.AndroidDatabaseDriverFactory
import tachiyomi.core.platform.background.BackgroundTaskScheduler
import tachiyomi.core.platform.background.BackgroundWorkerRegistry
import tachiyomi.core.platform.background.CoroutineBackgroundTaskScheduler
import tachiyomi.core.platform.javascript.AndroidJavaScriptRuntimeFactory
import tachiyomi.core.platform.javascript.JavaScriptRuntimeFactory
import tachiyomi.core.platform.storage.AndroidPlatformStorageDirectories
import tachiyomi.core.platform.storage.PlatformStorageDirectories
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler

internal actual class ChimahonPlatformServices actual constructor() {
    private val context = ChimahonAndroidHost.requireContext()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    actual val platformName: String = "Android"
    actual val backgroundState: String = "Shared foreground scheduler; legacy Android runtime owns WorkManager jobs"
    actual val storageDirectories: PlatformStorageDirectories = AndroidPlatformStorageDirectories(context)

    init {
        storageDirectories.ensureChimahonDirectories()
    }

    actual val sourceRegistry: SourceRegistry = SourceRegistry()
    actual val apkExtensionManager = ChimahonPlatformApkExtensionManager(storageDirectories, sourceRegistry)
    private val databaseDriver = AndroidDatabaseDriverFactory(
        context = context,
        openHelperFactory = FrameworkSQLiteOpenHelperFactory(),
        onOpen = ::configureDatabase,
    ).create(Database.Schema, DATABASE_NAME)
    actual val database: Database = createDatabase(databaseDriver)
    actual val databaseHandler: DatabaseHandler = AndroidDatabaseHandler(database, databaseDriver)
    actual val javaScriptRuntimeFactory: JavaScriptRuntimeFactory = AndroidJavaScriptRuntimeFactory

    actual fun createBackgroundTaskScheduler(
        workerRegistry: BackgroundWorkerRegistry,
    ): BackgroundTaskScheduler {
        return CoroutineBackgroundTaskScheduler(
            workerRegistry = workerRegistry,
            scope = backgroundScope,
        )
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun resolveExternalMangaUrl(source: CatalogueSource, manga: SManga): String? {
        return when (source) {
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

private fun configureDatabase(database: SupportSQLiteDatabase) {
    database.execSQL("PRAGMA foreign_keys = ON")
    database.execSQL("PRAGMA journal_mode = WAL")
    database.execSQL("PRAGMA synchronous = NORMAL")
}
