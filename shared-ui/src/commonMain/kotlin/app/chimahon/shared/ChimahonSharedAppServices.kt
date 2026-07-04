package app.chimahon.shared

import app.cash.sqldelight.db.SqlDriver
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceRegistry
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ScriptExtensionSourceManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path
import tachiyomi.core.extensions.LoadedScriptExtension
import tachiyomi.core.extensions.ScriptExtensionInvoker
import tachiyomi.core.extensions.ScriptExtensionLoader
import tachiyomi.core.extensions.ScriptExtensionStore
import tachiyomi.core.platform.background.BackgroundTaskScheduler
import tachiyomi.core.platform.background.BackgroundWorkerRegistry
import tachiyomi.core.platform.javascript.JavaScriptRuntimeFactory
import tachiyomi.core.platform.settings.FilePlatformSettingsStore
import tachiyomi.core.platform.storage.PlatformStorageDirectories
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.Extension_repos
import tachiyomi.data.GetCategories
import tachiyomi.data.GetCategoriesByMangaId
import tachiyomi.data.Mangas
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.libraryUpdateError.LibraryUpdateErrorRepositoryImpl
import tachiyomi.data.libraryUpdateErrorMessage.LibraryUpdateErrorMessageRepositoryImpl
import tachiyomi.domain.libraryUpdateError.interactor.GetLibraryUpdateErrors
import tachiyomi.domain.libraryUpdateErrorMessage.interactor.GetLibraryUpdateErrorMessages
import tachiyomi.view.History
import tachiyomi.view.UpdatesView
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class ChimahonSharedAppServices private constructor(
    private val platformServices: ChimahonPlatformServices = ChimahonPlatformServices(),
) {
    constructor() : this(ChimahonPlatformServices())

    private val libraryUpdateErrorRepository =
        LibraryUpdateErrorRepositoryImpl(platformServices.databaseHandler)
    private val getLibraryUpdateErrors =
        GetLibraryUpdateErrors(libraryUpdateErrorRepository)
    private val libraryUpdateErrorMessageRepository =
        LibraryUpdateErrorMessageRepositoryImpl(platformServices.databaseHandler)
    private val getLibraryUpdateErrorMessages =
        GetLibraryUpdateErrorMessages(libraryUpdateErrorMessageRepository)
    private val settingsStore =
        FilePlatformSettingsStore(platformServices.storageDirectories.filesDir / SETTINGS_FILE_NAME)
    private val settingsRepository = ChimahonSettingsRepository(settingsStore)
    private val serviceSettingsRepository = ChimahonServiceSettingsRepository(settingsStore)
    private val downloadQueueRepository = ChimahonDownloadQueueRepository(settingsStore)
    private val downloadBackgroundCoordinator = ChimahonDownloadBackgroundCoordinator(
        schedulerFactory = platformServices::createBackgroundTaskScheduler,
        loadQueue = downloadQueueRepository::load,
        processNextDownload = ::processNextDownload,
    )
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
        followRedirects = true
    }
    private val scriptExtensionLoader = ScriptExtensionLoader(platformServices.javaScriptRuntimeFactory)
    private val scriptExtensionManager = ScriptExtensionSourceManager(
        store = ScriptExtensionStore(
            storageDirectories = platformServices.storageDirectories,
            loader = scriptExtensionLoader,
        ),
        invoker = ScriptExtensionInvoker(platformServices.javaScriptRuntimeFactory),
        registry = platformServices.sourceRegistry,
    )
    private val readerProgressMutex = Mutex()
    private val readerProgressMarks = mutableMapOf<Long, TimeMark>()
    private val thumbnailCacheMutex = Mutex()
    private val thumbnailCache = mutableMapOf<String, ByteArray>()
    private val extensionMutationMutex = Mutex()
    private var knownScriptExtensions = emptyList<ChimahonInstalledExtensionEntry>()
    private var knownApkExtensions = emptyList<ChimahonInstalledExtensionEntry>()

    suspend fun loadSnapshot(): ChimahonSnapshot {
        val extensionState = loadExtensions()
        val allManga = platformServices.databaseHandler.awaitList {
            mangasQueries.getAllManga()
        }
        val errorMessages = getLibraryUpdateErrorMessages.await().associateBy { it.id }
        val updateErrors = getLibraryUpdateErrors.await()
        val extensionRepos = platformServices.databaseHandler.awaitList {
            extension_reposQueries.findAll()
        }
        val recentUpdates = platformServices.databaseHandler.awaitList {
            updatesViewQueries.getRecentUpdates(
                after = 0L,
                limit = 100L,
            )
        }
        val history = platformServices.databaseHandler.awaitList {
            historyViewQueries.history(
                bookmarkUnmask = CHAPTER_SHOW_NOT_BOOKMARKED,
                bookmarkMask = CHAPTER_SHOW_BOOKMARKED,
                unfinishedManga = null,
                unfinishedChapter = null,
                nonLibraryEntries = null,
                query = "",
            )
        }
        val libraryManga = allManga.filter(Mangas::favorite)
        val categories = platformServices.databaseHandler.awaitList {
            categoriesQueries.getCategories()
        }
        val libraryCategoryMemberships = libraryManga.associate { manga ->
            val categoryIds = platformServices.databaseHandler.awaitList {
                categoriesQueries.getCategoriesByMangaId(manga._id)
            }.map(GetCategoriesByMangaId::id)
            manga._id to categoryIds.ifEmpty { listOf(DEFAULT_LIBRARY_CATEGORY_ID) }
        }
        val detailMangaIds = (
            libraryManga.map(Mangas::_id) +
                recentUpdates.map(UpdatesView::mangaId) +
                history.map(History::mangaId)
            )
            .distinct()
        val chapters = if (detailMangaIds.isEmpty()) {
            emptyList()
        } else {
            platformServices.databaseHandler.awaitList {
                ehQueries.getChaptersByMangaIds(detailMangaIds)
            }
        }
        val mangaById = allManga.associateBy(Mangas::_id)
        val sharedMangaById = allManga
            .map(Mangas::toSharedMangaEntry)
            .associateBy(ChimahonMangaEntry::id)
        val sources = platformServices.sourceRegistry
            .getCatalogueSources()
            .sortedWith(compareBy<CatalogueSource> { it.lang }.thenBy { it.name.lowercase() })

        return ChimahonSnapshot(
            summary = ChimahonSummary(
                extensionCount = extensionState.extensionCount,
                sourceCount = sources.size,
                mangaCount = allManga.size,
            ),
            mangaDetails = sharedMangaById,
            library = libraryManga
                .asSequence()
                .map(Mangas::toSharedMangaEntry)
                .sortedBy { it.title.lowercase() }
                .toList(),
            libraryCategories = categories
                .map(GetCategories::toSharedLibraryCategory)
                .ifEmpty {
                    listOf(
                        ChimahonLibraryCategory(
                            id = DEFAULT_LIBRARY_CATEGORY_ID,
                            name = "",
                            order = -1L,
                            hidden = false,
                        ),
                    )
                },
            libraryCategoryMemberships = libraryCategoryMemberships,
            chaptersByMangaId = chapters
                .map(Chapters::toSharedChapterEntry)
                .groupBy(ChimahonChapterEntry::mangaId)
                .mapValues { (_, entries) ->
                    entries.sortedWith(
                        compareBy<ChimahonChapterEntry> { it.sourceOrder }
                            .thenBy { it.chapterNumber },
                    )
                },
            updates = recentUpdates.map(UpdatesView::toSharedUpdateEntry),
            history = history.map(History::toSharedHistoryEntry),
            sources = sources.map(CatalogueSource::toSharedSourceEntry),
            extensionRepos = extensionRepos
                .map(Extension_repos::toSharedExtensionRepoEntry)
                .sortedBy { it.name.lowercase() },
            installedExtensions = extensionState.installedExtensions,
            updateIssues = updateErrors
                .sortedByDescending { it.lastUpdate }
                .map { error ->
                    ChimahonUpdateIssue(
                        id = error.id,
                        mangaId = error.mangaId,
                        mangaTitle = mangaById[error.mangaId]?.title ?: "Manga #${error.mangaId}",
                        message = errorMessages[error.messageId]?.message ?: "Unknown update error",
                        lastUpdate = formatTimestamp(error.lastUpdate),
                    )
                },
            runtime = ChimahonRuntimeInfo(
                platformName = platformServices.platformName,
                filesDir = platformServices.storageDirectories.filesDir.toString(),
                cacheDir = platformServices.storageDirectories.cacheDir.toString(),
                downloadsDir = platformServices.storageDirectories.defaultDownloadsDir(APP_NAME).toString(),
                databaseState = "Connected",
                extensionState = extensionState.label,
                backgroundState = downloadBackgroundCoordinator.stateLabel(platformServices.backgroundState),
            ),
        )
    }

    suspend fun loadLibraryData(
        downloadedOnly: Boolean? = null,
        includeHiddenCategories: Boolean? = null,
    ): ChimahonLibraryData {
        val state = loadServiceDatabaseState()
        val useDownloadedOnly = downloadedOnly ?: loadDownloadedOnlySetting()
        val showHiddenCategories = includeHiddenCategories ?: loadShowHiddenCategoriesSetting()
        val allEntries = state.libraryManga.map { manga ->
            manga.toLibraryMangaData(state)
        }
        val visibleEntries = allEntries
            .asSequence()
            .filter { !useDownloadedOnly || it.hasDownloads }
            .sortedBy { it.manga.title.lowercase() }
            .toList()
        val visibleMangaIds = visibleEntries.mapTo(mutableSetOf()) { it.manga.id }
        val categoryData = state.categories
            .asSequence()
            .map(GetCategories::toSharedLibraryCategory)
            .filter { showHiddenCategories || !it.hidden }
            .map { category ->
                val categoryEntries = visibleEntries.filter { category.id in it.categoryIds }
                ChimahonCategoryData(
                    category = category,
                    mangaCount = categoryEntries.size,
                    unreadChapterCount = categoryEntries.sumOf(ChimahonLibraryMangaData::unreadChapterCount),
                    downloadedChapterCount = categoryEntries.sumOf(
                        ChimahonLibraryMangaData::downloadedChapterCount,
                    ),
                )
            }
            .filter { category ->
                category.category.id == DEFAULT_LIBRARY_CATEGORY_ID ||
                    category.mangaCount > 0 ||
                    state.libraryCategoryMemberships.values.any { category.category.id in it }
            }
            .toList()

        return ChimahonLibraryData(
            entries = visibleEntries,
            categories = categoryData,
            downloadedOnly = useDownloadedOnly,
            totalMangaCount = allEntries.size,
            downloadedMangaCount = allEntries.count(ChimahonLibraryMangaData::hasDownloads),
        )
    }

    suspend fun loadLibrary(
        downloadedOnly: Boolean? = null,
    ): List<ChimahonLibraryMangaData> {
        return loadLibraryData(downloadedOnly = downloadedOnly).entries
    }

    suspend fun loadDownloadedLibrary(): List<ChimahonLibraryMangaData> {
        return loadLibrary(downloadedOnly = true)
    }

    suspend fun loadDownloadedChapterIds(
        mangaId: Long? = null,
    ): Set<Long> {
        val state = loadServiceDatabaseState()
        if (mangaId == null) return state.downloadedIndex.chapterIds
        val mangaChapterIds = state.chaptersByMangaId[mangaId].orEmpty().mapTo(mutableSetOf(), Chapters::_id)
        return state.downloadedIndex.chapterIds.filterTo(mutableSetOf()) { it in mangaChapterIds }
    }

    suspend fun isChapterDownloaded(chapterId: Long): Boolean {
        return chapterId in loadDownloadedChapterIds()
    }

    suspend fun loadUpdates(
        after: Long = 0L,
        limit: Int = DEFAULT_SERVICE_PAGE_SIZE,
        filter: ChimahonUpdatesFilter = ChimahonUpdatesFilter(),
    ): List<ChimahonRecentUpdateEntry> {
        return loadUpdatesData(after, limit, filter).entries.map(ChimahonUpdateData::update)
    }

    suspend fun loadUpdatesData(
        after: Long = 0L,
        limit: Int = DEFAULT_SERVICE_PAGE_SIZE,
        filter: ChimahonUpdatesFilter = ChimahonUpdatesFilter(),
    ): ChimahonUpdatesData {
        require(limit > 0) { "Updates limit must be positive." }
        val state = loadServiceDatabaseState()
        val effectiveDownloadedFilter = filter.downloadedOnly
            ?: true.takeIf { loadDownloadedOnlySetting() }
        val updates = platformServices.databaseHandler.awaitList {
            updatesViewQueries.getRecentUpdates(
                after = after.coerceAtLeast(0L),
                limit = Long.MAX_VALUE,
            )
        }
            .asSequence()
            .filter { update -> filter.read == null || update.read == filter.read }
            .filter { update ->
                when (filter.started) {
                    true -> !update.read && update.last_page_read > 0L
                    false -> !update.read && update.last_page_read == 0L
                    null -> true
                }
            }
            .filter { update -> filter.bookmarked == null || update.bookmark == filter.bookmarked }
            .map { update ->
                ChimahonUpdateData(
                    update = update.toSharedUpdateEntry(),
                    downloaded = update.chapterId in state.downloadedIndex.chapterIds,
                )
            }
            .filter { entry ->
                effectiveDownloadedFilter == null || entry.downloaded == effectiveDownloadedFilter
            }
            .take(limit + 1)
            .toList()

        return ChimahonUpdatesData(
            entries = updates.take(limit),
            after = after.coerceAtLeast(0L),
            limit = limit,
            hasMore = updates.size > limit,
        )
    }

    suspend fun loadHistory(
        limit: Int = DEFAULT_SERVICE_PAGE_SIZE,
        filter: ChimahonHistoryFilter = ChimahonHistoryFilter(),
    ): List<ChimahonHistoryEntry> {
        return loadHistoryData(limit, filter).entries.map { it.history }
    }

    suspend fun loadHistoryData(
        limit: Int = DEFAULT_SERVICE_PAGE_SIZE,
        filter: ChimahonHistoryFilter = ChimahonHistoryFilter(),
    ): ChimahonHistoryData {
        require(limit > 0) { "History limit must be positive." }
        val state = loadServiceDatabaseState()
        val effectiveDownloadedFilter = filter.downloadedOnly
            ?: true.takeIf { loadDownloadedOnlySetting() }
        val history = platformServices.databaseHandler.awaitList {
            historyViewQueries.history(
                bookmarkUnmask = CHAPTER_SHOW_NOT_BOOKMARKED,
                bookmarkMask = CHAPTER_SHOW_BOOKMARKED,
                unfinishedManga = filter.unfinishedManga?.let { if (it) 1L else 0L },
                unfinishedChapter = filter.unfinishedChapter,
                nonLibraryEntries = false.takeIf { !filter.includeNonLibrary },
                query = filter.query.trim().lowercase(),
            )
        }
            .asSequence()
            .map { historyEntry ->
                ChimahonHistoryDataEntry(
                    history = historyEntry.toSharedHistoryEntry(),
                    downloaded = historyEntry.chapterId in state.downloadedIndex.chapterIds,
                )
            }
            .filter { entry ->
                effectiveDownloadedFilter == null || entry.downloaded == effectiveDownloadedFilter
            }
            .take(limit + 1)
            .toList()

        return ChimahonHistoryData(
            entries = history.take(limit),
            limit = limit,
            hasMore = history.size > limit,
        )
    }

    suspend fun loadLibraryCategories(
        includeDefault: Boolean = true,
        includeHidden: Boolean = true,
    ): List<ChimahonLibraryCategory> {
        return platformServices.databaseHandler.awaitList {
            categoriesQueries.getCategories()
        }
            .asSequence()
            .map(GetCategories::toSharedLibraryCategory)
            .filter { includeDefault || it.id != DEFAULT_LIBRARY_CATEGORY_ID }
            .filter { includeHidden || !it.hidden }
            .toList()
    }

    suspend fun loadCategoryData(
        includeHidden: Boolean = true,
        downloadedOnly: Boolean? = null,
    ): List<ChimahonCategoryData> {
        return loadLibraryData(
            downloadedOnly = downloadedOnly,
            includeHiddenCategories = includeHidden,
        ).categories
    }

    suspend fun renameCategory(
        categoryId: Long,
        name: String,
    ) {
        val normalizedName = name.trim()
        require(categoryId > DEFAULT_LIBRARY_CATEGORY_ID) { "The default category cannot be renamed." }
        require(normalizedName.isNotEmpty()) { "Category name is empty." }
        val categories = platformServices.databaseHandler.awaitList {
            categoriesQueries.getCategories()
        }
        require(categories.any { it.id == categoryId }) { "Category $categoryId does not exist." }
        require(
            categories.none {
                it.id != categoryId && it.name.equals(normalizedName, ignoreCase = true)
            },
        ) {
            "A category named $normalizedName already exists."
        }
        platformServices.databaseHandler.await {
            categoriesQueries.update(
                name = normalizedName,
                order = null,
                flags = null,
                hidden = null,
                categoryId = categoryId,
            )
        }
    }

    suspend fun setCategoryHidden(
        categoryId: Long,
        hidden: Boolean,
    ) {
        require(categoryId > DEFAULT_LIBRARY_CATEGORY_ID) { "The default category cannot be hidden." }
        platformServices.databaseHandler.await {
            categoriesQueries.update(
                name = null,
                order = null,
                flags = null,
                hidden = if (hidden) 1L else 0L,
                categoryId = categoryId,
            )
        }
    }

    suspend fun reorderCategories(categoryIds: List<Long>) {
        val existing = platformServices.databaseHandler.awaitList {
            categoriesQueries.getCategories()
        }
            .filter { it.id > DEFAULT_LIBRARY_CATEGORY_ID }
        val existingIds = existing.mapTo(mutableSetOf(), GetCategories::id)
        val requestedIds = categoryIds
            .asSequence()
            .filter { it in existingIds }
            .distinct()
            .toList()
        val orderedIds = requestedIds + existing.map(GetCategories::id).filterNot { it in requestedIds }
        platformServices.databaseHandler.await(inTransaction = true) {
            orderedIds.forEachIndexed { index, categoryId ->
                categoriesQueries.update(
                    name = null,
                    order = (index + 1).toLong(),
                    flags = null,
                    hidden = null,
                    categoryId = categoryId,
                )
            }
        }
    }

    suspend fun moveCategory(
        categoryId: Long,
        newIndex: Int,
    ) {
        require(categoryId > DEFAULT_LIBRARY_CATEGORY_ID) { "The default category cannot be moved." }
        val categoryIds = platformServices.databaseHandler.awaitList {
            categoriesQueries.getCategories()
        }
            .filter { it.id > DEFAULT_LIBRARY_CATEGORY_ID }
            .map(GetCategories::id)
            .toMutableList()
        val oldIndex = categoryIds.indexOf(categoryId)
        require(oldIndex >= 0) { "Category $categoryId does not exist." }
        val destination = newIndex.coerceIn(0, categoryIds.lastIndex)
        categoryIds.add(destination, categoryIds.removeAt(oldIndex))
        reorderCategories(categoryIds)
    }

    suspend fun loadStatistics(): ChimahonStatisticsData {
        val state = loadServiceDatabaseState()
        val libraryMangaIds = state.libraryManga.mapTo(mutableSetOf(), Mangas::_id)
        val libraryChapters = state.chapters.filter { it.manga_id in libraryMangaIds }
        val history = platformServices.databaseHandler.awaitList {
            historyQueries.getAllHistory()
        }

        return ChimahonStatisticsData(
            libraryMangaCount = state.libraryManga.size,
            startedMangaCount = state.libraryManga.count { manga ->
                state.chaptersByMangaId[manga._id].orEmpty().any {
                    it.read || it.last_page_read > 0L
                }
            },
            completedMangaCount = state.libraryManga.count { it.status == SManga.COMPLETED.toLong() },
            sourceCount = state.libraryManga.map(Mangas::source).distinct().size,
            categoryCount = state.categories.count { it.id > DEFAULT_LIBRARY_CATEGORY_ID },
            totalChapterCount = libraryChapters.size,
            readChapterCount = libraryChapters.count(Chapters::read),
            unreadChapterCount = libraryChapters.count { !it.read },
            bookmarkedChapterCount = libraryChapters.count(Chapters::bookmark),
            downloadedMangaCount = state.downloadedIndex.mangaIds.count { it in libraryMangaIds },
            downloadedChapterCount = state.downloadedIndex.chapterIds.count { chapterId ->
                libraryChapters.any { it._id == chapterId }
            },
            historyEntryCount = history.size,
            totalReadDurationMillis = history.sumOf { it.time_read.coerceAtLeast(0L) },
        )
    }

    suspend fun loadStorageData(): ChimahonStorageData {
        val directories = runCatching { platformServices.storageDirectories }.getOrNull()
        if (directories == null) {
            val empty = ChimahonStorageSection("", false, 0L, 0, 0)
            return ChimahonStorageData(empty, empty, empty, 0L, 0, 0)
        }
        val downloadsDirectory = directories.defaultDownloadsDir(APP_NAME)
        val files = collectTreeStats(directories.filesDir).toStorageSection(directories.filesDir)
        val cache = collectTreeStats(directories.cacheDir).toStorageSection(directories.cacheDir)
        val downloads = collectTreeStats(downloadsDirectory).toStorageSection(downloadsDirectory)
        val downloadedIndex = loadServiceDatabaseState().downloadedIndex

        return ChimahonStorageData(
            files = files,
            cache = cache,
            downloads = downloads,
            totalSizeBytes = files.sizeBytes + cache.sizeBytes + downloads.sizeBytes,
            downloadedMangaCount = downloadedIndex.mangaIds.size,
            downloadedChapterCount = downloadedIndex.chapterIds.size,
        )
    }

    suspend fun loadThumbnailCacheData(): ChimahonThumbnailCacheData {
        return thumbnailCacheMutex.withLock {
            ChimahonThumbnailCacheData(
                entryCount = thumbnailCache.size,
                sizeBytes = thumbnailCache.values.sumOf { it.size.toLong() },
            )
        }
    }

    suspend fun loadUpdateIssueSummary(): ChimahonUpdateIssueSummary {
        val issues = platformServices.databaseHandler.awaitList {
            libraryUpdateErrorQueries.getAllErrors()
        }
        val messages = platformServices.databaseHandler.awaitList {
            libraryUpdateErrorMessageQueries.getAllErrorMessages()
        }.associate { it._id to it.message }
        val mangaById = platformServices.databaseHandler.awaitList {
            mangasQueries.getAllManga()
        }.associateBy(Mangas::_id)

        val groups = issues
            .groupBy { it.message_id }
            .map { (messageId, groupedIssues) ->
                ChimahonUpdateIssueAggregate(
                    messageId = messageId,
                    message = messages[messageId] ?: "Unknown update error",
                    issueCount = groupedIssues.size,
                    affectedMangaCount = groupedIssues.map { it.manga_id }.distinct().size,
                    latestUpdateEpochSeconds = groupedIssues.maxOfOrNull { it.last_update } ?: 0L,
                )
            }
            .sortedWith(
                compareByDescending<ChimahonUpdateIssueAggregate> { it.issueCount }
                    .thenBy { it.message.lowercase() },
            )

        return ChimahonUpdateIssueSummary(
            totalIssueCount = issues.size,
            affectedMangaCount = issues.map { it.manga_id }.distinct().size,
            staleIssueCount = issues.count { issue ->
                mangaById[issue.manga_id]?.favorite != true
            },
            groups = groups,
        )
    }

    suspend fun loadDatabaseMaintenanceData(): ChimahonDatabaseMaintenanceData {
        val historyEntryCount = platformServices.databaseHandler.awaitList {
            historyQueries.getAllHistory()
        }.size
        return ChimahonDatabaseMaintenanceData(
            historyEntryCount = historyEntryCount,
            updateIssues = loadUpdateIssueSummary(),
        )
    }

    suspend fun loadDataMaintenanceSnapshot(): ChimahonDataMaintenanceSnapshot {
        return ChimahonDataMaintenanceSnapshot(
            storage = loadStorageData(),
            thumbnailCache = loadThumbnailCacheData(),
            database = loadDatabaseMaintenanceData(),
        )
    }

    suspend fun clearThumbnailCache(): ChimahonCacheClearResult {
        val cleared = thumbnailCacheMutex.withLock {
            val data = ChimahonThumbnailCacheData(
                entryCount = thumbnailCache.size,
                sizeBytes = thumbnailCache.values.sumOf { it.size.toLong() },
            )
            thumbnailCache.clear()
            data
        }
        return ChimahonCacheClearResult(
            target = ChimahonCacheClearTarget.Thumbnails,
            diskBytesRemoved = 0L,
            diskFilesRemoved = 0,
            diskDirectoriesRemoved = 0,
            memoryBytesRemoved = cleared.sizeBytes,
            memoryEntriesRemoved = cleared.entryCount,
        )
    }

    suspend fun clearCacheData(): ChimahonCacheClearResult {
        val memoryResult = clearThumbnailCache()
        val diskResult = clearDirectoryContents(platformServices.storageDirectories.cacheDir)
        return ChimahonCacheClearResult(
            target = ChimahonCacheClearTarget.ApplicationCache,
            diskBytesRemoved = diskResult.bytesRemoved,
            diskFilesRemoved = diskResult.filesRemoved,
            diskDirectoriesRemoved = diskResult.directoriesRemoved,
            memoryBytesRemoved = memoryResult.memoryBytesRemoved,
            memoryEntriesRemoved = memoryResult.memoryEntriesRemoved,
            failures = diskResult.failures,
        )
    }

    suspend fun addExtensionRepo(input: String): ChimahonExtensionRepoEntry {
        val baseUrl = normalizeExtensionRepoBaseUrl(input)
        val repo = if (baseUrl.endsWith(SCRIPT_EXTENSION_SUFFIX, ignoreCase = true)) {
            val script = fetchText(baseUrl)
            val extension = scriptExtensionLoader.load(script)
            ChimahonExtensionRepoEntry(
                baseUrl = baseUrl,
                name = extension.manifest.name,
                shortName = extension.manifest.name.take(16),
                website = baseUrl,
                signingKeyFingerprint = "script-${extension.manifest.id}",
            )
        } else {
            val metadata = fetchRepoMetadata(baseUrl)
            loadExtensionRepoCatalog(metadata)
            metadata
        }

        platformServices.databaseHandler.await {
            extension_reposQueries.upsert(
                base_url = repo.baseUrl,
                name = repo.name,
                short_name = repo.shortName,
                website = repo.website,
                fingerprint = repo.signingKeyFingerprint,
            )
        }
        return repo
    }

    suspend fun deleteExtensionRepo(baseUrl: String) {
        platformServices.databaseHandler.await {
            extension_reposQueries.delete(baseUrl)
        }
    }

    suspend fun loadExtensionRepoCatalog(
        repo: ChimahonExtensionRepoEntry,
    ): ChimahonExtensionRepoCatalog {
        val extensions = if (repo.baseUrl.endsWith(SCRIPT_EXTENSION_SUFFIX, ignoreCase = true)) {
            val script = fetchText(repo.baseUrl)
            val extension = scriptExtensionLoader.load(script)
            listOf(extension.toRepoExtensionEntry(repo.baseUrl, repo.baseUrl))
        } else {
            fetchFirstCompatibleRepoCatalog(
                repoBaseUrl = repo.baseUrl,
                urls = repoCatalogUrls(repo.baseUrl),
            )
        }
        require(extensions.isNotEmpty()) {
            "Repository has no compatible JavaScript or Android APK extensions."
        }
        return ChimahonExtensionRepoCatalog(
            repo = repo,
            extensions = extensions.sortedBy { it.name.lowercase() },
        )
    }

    suspend fun installExtension(
        extension: ChimahonRepoExtensionEntry,
    ): ChimahonInstalledExtensionEntry = extensionMutationMutex.withLock {
        try {
            val installed = when (extension.packageType) {
                ChimahonExtensionPackageType.JavaScript -> {
                    val script = fetchText(extension.artifactUrl)
                    val candidate = scriptExtensionLoader.load(script)
                    require(candidate.manifest.id == extension.id) {
                        "Downloaded extension id ${candidate.manifest.id} does not match ${extension.id}."
                    }
                    scriptExtensionManager.install(script).toSharedInstalledExtensionEntry()
                }
                ChimahonExtensionPackageType.AndroidApk -> {
                    platformServices.apkExtensionManager.install(
                        extension = extension,
                        apkBytes = fetchBytes(extension.artifactUrl),
                    )
                }
            }
            rememberInstalledExtension(installed)
            refreshExtensionsLocked().installedExtensions
                .firstOrNull { it.id == installed.id && it.packageType == installed.packageType }
                ?: installed
        } catch (error: Throwable) {
            refreshExtensionsLocked()
            throw error
        }
    }

    suspend fun uninstallExtension(
        packageId: String,
        packageType: ChimahonExtensionPackageType,
    ): ChimahonExtensionUninstallResult = extensionMutationMutex.withLock {
        require(packageId.isNotBlank()) { "Extension package/id cannot be blank." }
        val apkStatusBefore = platformServices.apkExtensionManager.status()
        val supported = packageType != ChimahonExtensionPackageType.AndroidApk ||
            apkStatusBefore.isSupported
        val removed = try {
            when (packageType) {
                ChimahonExtensionPackageType.JavaScript -> scriptExtensionManager.uninstall(packageId)
                ChimahonExtensionPackageType.AndroidApk -> {
                    if (supported) {
                        platformServices.apkExtensionManager.uninstall(packageId)
                    } else {
                        false
                    }
                }
            }
        } catch (error: Throwable) {
            refreshExtensionsLocked()
            throw error
        }
        if (removed) {
            forgetInstalledExtension(packageId, packageType)
        }
        val refreshed = refreshExtensionsLocked()
        val unsupportedError = if (supported) {
            emptyList()
        } else {
            listOf("Android APK extension management is unavailable on this platform.")
        }
        ChimahonExtensionUninstallResult(
            extensionId = packageId,
            packageType = packageType,
            supported = supported,
            removed = removed,
            installedExtensions = refreshed.installedExtensions,
            errors = (refreshed.errors + unsupportedError).distinct(),
        )
    }

    suspend fun refreshInstalledExtensions(): List<ChimahonInstalledExtensionEntry> {
        return extensionMutationMutex.withLock {
            refreshExtensionsLocked().installedExtensions
        }
    }

    suspend fun loadExtensionManagementData(): ChimahonExtensionManagementData {
        val extensionState = extensionMutationMutex.withLock {
            refreshExtensionsLocked()
        }
        val repos = platformServices.databaseHandler.awaitList {
            extension_reposQueries.findAll()
        }.map(Extension_repos::toSharedExtensionRepoEntry)
        val availableExtensions = mutableListOf<ChimahonRepoExtensionEntry>()
        val catalogErrors = mutableListOf<String>()
        repos.forEach { repo ->
            runCatching { loadExtensionRepoCatalog(repo) }
                .onSuccess { catalog -> availableExtensions += catalog.extensions }
                .onFailure { error ->
                    catalogErrors += "${repo.name}: ${error.message ?: "repository unavailable"}"
                }
        }
        return buildExtensionManagementData(
            installedExtensions = extensionState.installedExtensions,
            availableExtensions = availableExtensions,
            apkStatus = platformServices.apkExtensionManager.status(),
            errors = extensionState.errors + catalogErrors,
        )
    }

    private suspend fun fetchRepoMetadata(baseUrl: String): ChimahonExtensionRepoEntry {
        val metadata = runCatching {
            val payload = fetchText("$baseUrl/repo.json")
            val root = json.parseToJsonElement(payload).jsonObject
            val meta = (root["meta"] as? JsonObject) ?: root
            val name = meta.stringValue("name")
                ?: baseUrl.repoHost()
            ChimahonExtensionRepoEntry(
                baseUrl = baseUrl,
                name = name,
                shortName = meta.stringValue("shortName", "short_name") ?: name.take(16),
                website = meta.stringValue("website") ?: baseUrl,
                signingKeyFingerprint = meta.stringValue(
                    "signingKeyFingerprint",
                    "signing_key_fingerprint",
                ) ?: "shared-${baseUrl.stableRepoHash()}",
            )
        }.getOrNull()

        return metadata ?: ChimahonExtensionRepoEntry(
            baseUrl = baseUrl,
            name = baseUrl.repoHost(),
            shortName = baseUrl.repoHost().substringBefore(".").take(16),
            website = baseUrl,
            signingKeyFingerprint = "shared-${baseUrl.stableRepoHash()}",
        )
    }

    private suspend fun fetchFirstAvailable(urls: List<String>): String {
        var lastError: Throwable? = null
        for (url in urls) {
            runCatching { fetchText(url) }
                .onSuccess { return it }
                .onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("No repository index URL was available.")
    }

    private suspend fun fetchFirstCompatibleRepoCatalog(
        repoBaseUrl: String,
        urls: List<String>,
    ): List<ChimahonRepoExtensionEntry> {
        var lastError: Throwable? = null
        for (url in urls) {
            runCatching {
                parseRepoExtensions(
                    json = json,
                    repoBaseUrl = repoBaseUrl,
                    payload = fetchText(url),
                )
            }
                .onSuccess { extensions ->
                    if (extensions.isNotEmpty()) return extensions
                }
                .onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException(
            "Repository indexes did not contain compatible JavaScript or Android APK extensions.",
        )
    }

    private suspend fun fetchText(url: String): String {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) {
            "HTTP ${response.status.value} while loading $url"
        }
        return response.bodyAsText()
    }

    private suspend fun fetchBytes(url: String): ByteArray {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) {
            "HTTP ${response.status.value} while loading $url"
        }
        return response.readRawBytes()
    }

    suspend fun loadSourcePreview(
        sourceId: Long,
        mode: ChimahonSourceBrowseMode,
        query: String,
        pageNumber: Int,
    ): ChimahonSourcePreview {
        require(pageNumber > 0) { "Page number must be positive." }
        val source = platformServices.sourceRegistry
            .getCatalogueSources()
            .firstOrNull { it.id == sourceId }
            ?: error("Source $sourceId is not loaded.")
        val page = when (mode) {
            ChimahonSourceBrowseMode.Popular -> source.getPopularManga(pageNumber)
            ChimahonSourceBrowseMode.Latest -> {
                if (source.supportsLatest) {
                    source.getLatestUpdates(pageNumber)
                } else {
                    source.getPopularManga(pageNumber)
                }
            }
            ChimahonSourceBrowseMode.Search -> {
                require(query.isNotBlank()) { "Search query is empty." }
                source.getSearchManga(pageNumber, query, source.getFilterList())
            }
        }

        return ChimahonSourcePreview(
            sourceId = source.id,
            sourceName = source.name,
            mode = mode,
            entries = page.mangas.map { it.toRemoteMangaEntry(source.id) },
            hasNextPage = page.hasNextPage,
        )
    }

    suspend fun loadRemoteMangaDetail(
        remoteManga: ChimahonRemoteMangaEntry,
    ): ChimahonRemoteMangaDetail {
        val source = platformServices.sourceRegistry
            .getCatalogueSources()
            .firstOrNull { it.id == remoteManga.sourceId }
            ?: error("Source ${remoteManga.sourceId} is not loaded.")
        val seed = SManga(
            url = remoteManga.url,
            title = remoteManga.title,
            author = remoteManga.author,
            status = 0,
            thumbnail_url = remoteManga.thumbnailUrl,
            initialized = remoteManga.initialized,
        )
        val details = runCatching {
            source.getMangaDetails(seed).withSeedFallback(seed)
        }.getOrElse { error ->
            if (error.isUninitializedSourceUrlFailure()) {
                seed.withSeedFallback(seed)
            } else {
                throw error
            }
        }
        val chapters = source.getChapterList(details)

        return ChimahonRemoteMangaDetail(
            sourceId = source.id,
            sourceName = source.name,
            title = details.title,
            artist = details.artist,
            author = details.author,
            description = details.description,
            genres = details.getGenres().orEmpty(),
            status = mangaStatus(details.status.toLong()),
            statusCode = details.status.toLong(),
            url = details.safeUrl(remoteManga.url),
            thumbnailUrl = details.thumbnail_url,
            initialized = details.initialized,
            chapters = chapters
                .map(SChapter::toRemoteChapterEntry)
                .sortedWith(
                    compareByDescending<ChimahonRemoteChapterEntry> { it.chapterNumber }
                        .thenBy { it.name },
            ),
        )
    }

    fun openExternalUrl(url: String): Boolean {
        val externalUrl = url.normalizedExternalWebUrlOrNull() ?: return false
        return platformServices.openExternalUrl(externalUrl)
    }

    fun openRemoteMangaUrl(
        detail: ChimahonRemoteMangaDetail,
    ): Boolean {
        val source = platformServices.sourceRegistry
            .getCatalogueSources()
            .firstOrNull { it.id == detail.sourceId }
        val manga = SManga(
            url = detail.url,
            title = detail.title,
            artist = detail.artist,
            author = detail.author,
            description = detail.description,
            genre = detail.genres.joinToString(", ").takeIf { it.isNotBlank() },
            status = detail.statusCode.toInt(),
            thumbnail_url = detail.thumbnailUrl,
            initialized = detail.initialized,
        )
        val externalUrl = source
            ?.let { platformServices.resolveExternalMangaUrl(it, manga) }
            ?.normalizedExternalWebUrlOrNull()
            ?: detail.url.normalizedExternalWebUrlOrNull()
            ?: return false

        return platformServices.openExternalUrl(externalUrl)
    }

    suspend fun addRemoteMangaToLibrary(
        detail: ChimahonRemoteMangaDetail,
    ): Long {
        return importRemoteMangaToLibrary(
            databaseHandler = platformServices.databaseHandler,
            detail = detail,
            now = platformServices.currentTimeMillis(),
        )
    }

    suspend fun refreshManga(mangaId: Long) {
        val manga = platformServices.databaseHandler.awaitOneOrNull {
            mangasQueries.getMangaById(mangaId)
        } ?: error("Manga $mangaId is not present in the shared database.")
        val detail = loadRemoteMangaDetail(
            ChimahonRemoteMangaEntry(
                sourceId = manga.source,
                title = manga.title,
                author = manga.author,
                status = mangaStatus(manga.status),
                url = manga.url,
                thumbnailUrl = manga.thumbnail_url,
                initialized = manga.initialized,
            ),
        )
        importRemoteMangaToLibrary(
            databaseHandler = platformServices.databaseHandler,
            detail = detail,
            now = platformServices.currentTimeMillis(),
        )
    }

    suspend fun createCategory(name: String): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Category name is empty." }
        val categories = platformServices.databaseHandler.awaitList {
            categoriesQueries.getCategories()
        }
        require(categories.none { it.name.equals(normalizedName, ignoreCase = true) }) {
            "A category named $normalizedName already exists."
        }
        return platformServices.databaseHandler.awaitOneExecutable(inTransaction = true) {
            categoriesQueries.insert(
                name = normalizedName,
                order = (categories.maxOfOrNull { it.order } ?: 0L) + 1L,
                flags = 0L,
                hidden = 0L,
            )
            categoriesQueries.selectLastInsertedRowId()
        }
    }

    suspend fun deleteCategory(categoryId: Long) {
        require(categoryId > DEFAULT_LIBRARY_CATEGORY_ID) { "The default category cannot be deleted." }
        platformServices.databaseHandler.await {
            categoriesQueries.delete(categoryId)
        }
    }

    suspend fun setMangaCategories(
        mangaId: Long,
        categoryIds: Set<Long>,
    ) {
        val selectedIds = categoryIds
            .filter { it >= DEFAULT_LIBRARY_CATEGORY_ID }
            .ifEmpty { listOf(DEFAULT_LIBRARY_CATEGORY_ID) }
        platformServices.databaseHandler.await(inTransaction = true) {
            mangas_categoriesQueries.deleteMangaCategoryByMangaId(mangaId)
            selectedIds.forEach { categoryId ->
                mangas_categoriesQueries.insert(mangaId, categoryId)
            }
        }
    }

    suspend fun setMangasCategories(
        mangaIds: Collection<Long>,
        categoryIds: Collection<Long>,
    ): ChimahonLibraryBulkActionResult {
        val normalizedMangaIds = mangaIds.distinct()
        val normalizedCategoryIds = categoryIds
            .filter { it >= DEFAULT_LIBRARY_CATEGORY_ID }
            .distinct()
            .ifEmpty { listOf(DEFAULT_LIBRARY_CATEGORY_ID) }
        updateMangasCategories(
            databaseHandler = platformServices.databaseHandler,
            mangaIds = normalizedMangaIds,
            categoryIds = normalizedCategoryIds,
        )
        return ChimahonLibraryBulkActionResult(
            mangaCount = normalizedMangaIds.size,
            categoryCount = normalizedCategoryIds.size,
        )
    }

    suspend fun setMangaFavorite(
        mangaId: Long,
        favorite: Boolean,
    ) {
        updateMangaFavorite(
            databaseHandler = platformServices.databaseHandler,
            mangaId = mangaId,
            favorite = favorite,
            now = platformServices.currentTimeMillis(),
        )
    }

    suspend fun setMangasFavorite(
        mangaIds: Collection<Long>,
        favorite: Boolean,
    ): ChimahonLibraryBulkActionResult {
        val normalizedMangaIds = mangaIds.distinct()
        updateMangasFavorite(
            databaseHandler = platformServices.databaseHandler,
            mangaIds = normalizedMangaIds,
            favorite = favorite,
            now = platformServices.currentTimeMillis(),
        )
        return ChimahonLibraryBulkActionResult(mangaCount = normalizedMangaIds.size)
    }

    suspend fun setMangaNotes(
        mangaId: Long,
        notes: String,
    ) {
        updateMangaNotes(
            databaseHandler = platformServices.databaseHandler,
            mangaId = mangaId,
            notes = notes.trim(),
        )
    }

    suspend fun resetHistoryEntry(historyId: Long) {
        platformServices.databaseHandler.await(inTransaction = true) {
            historyQueries.resetHistoryByIds(listOf(historyId))
            historyQueries.removeResettedHistory()
        }
    }

    suspend fun resetHistoryForManga(mangaId: Long) {
        platformServices.databaseHandler.await(inTransaction = true) {
            historyQueries.resetHistoryByMangaIds(listOf(mangaId))
            historyQueries.removeResettedHistory()
        }
    }

    suspend fun clearHistory() {
        clearHistoryWithResult()
    }

    suspend fun clearHistoryWithResult(): ChimahonDatabaseMaintenanceResult {
        val before = loadDatabaseMaintenanceData()
        platformServices.databaseHandler.await {
            historyQueries.removeAllHistory()
        }
        return databaseMaintenanceResult(before)
    }

    suspend fun dismissUpdateIssue(issueId: Long) {
        dismissUpdateIssueWithResult(issueId)
    }

    suspend fun dismissUpdateIssueWithResult(issueId: Long): ChimahonDatabaseMaintenanceResult {
        val before = loadDatabaseMaintenanceData()
        platformServices.databaseHandler.await {
            libraryUpdateErrorQueries.deleteErrors(listOf(issueId))
        }
        return databaseMaintenanceResult(before)
    }

    suspend fun clearUpdateIssues() {
        clearUpdateIssuesWithResult()
    }

    suspend fun clearUpdateIssuesWithResult(): ChimahonDatabaseMaintenanceResult {
        val before = loadDatabaseMaintenanceData()
        platformServices.databaseHandler.await {
            libraryUpdateErrorQueries.deleteAllErrors()
        }
        return databaseMaintenanceResult(before)
    }

    suspend fun runDatabaseMaintenance(): ChimahonDatabaseMaintenanceResult {
        val before = loadDatabaseMaintenanceData()
        platformServices.databaseHandler.await(inTransaction = true) {
            historyQueries.removeResettedHistory()
            libraryUpdateErrorQueries.cleanUnrelevantMangaErrors()
        }
        return databaseMaintenanceResult(before)
    }

    suspend fun setMangaChaptersRead(
        mangaId: Long,
        read: Boolean,
    ) {
        updateMangaChaptersRead(
            databaseHandler = platformServices.databaseHandler,
            mangaId = mangaId,
            read = read,
        )
    }

    suspend fun setMangasChaptersRead(
        mangaIds: Collection<Long>,
        read: Boolean,
    ): ChimahonLibraryBulkActionResult {
        val normalizedMangaIds = mangaIds.distinct()
        val chapterCount = normalizedMangaIds.sumOf { mangaId ->
            platformServices.databaseHandler.awaitList {
                chaptersQueries.getChaptersByMangaId(
                    mangaId = mangaId,
                    applyFilter = 0L,
                    bookmarkUnmask = 0L,
                    bookmarkMask = 0L,
                )
            }.size
        }
        updateMangasChaptersRead(
            databaseHandler = platformServices.databaseHandler,
            mangaIds = normalizedMangaIds,
            read = read,
        )
        return ChimahonLibraryBulkActionResult(
            mangaCount = normalizedMangaIds.size,
            chapterCount = chapterCount,
        )
    }

    suspend fun setChapterRead(
        chapterId: Long,
        read: Boolean,
    ) {
        updateChapterRead(
            databaseHandler = platformServices.databaseHandler,
            chapterId = chapterId,
            read = read,
        )
    }

    suspend fun setChapterBookmark(
        chapterId: Long,
        bookmarked: Boolean,
    ) {
        updateChapterBookmark(
            databaseHandler = platformServices.databaseHandler,
            chapterId = chapterId,
            bookmarked = bookmarked,
        )
    }

    suspend fun setChaptersBookmark(
        chapterIds: Collection<Long>,
        bookmarked: Boolean,
    ): ChimahonLibraryBulkActionResult {
        val normalizedChapterIds = chapterIds.distinct()
        updateChaptersBookmark(
            databaseHandler = platformServices.databaseHandler,
            chapterIds = normalizedChapterIds,
            bookmarked = bookmarked,
        )
        return ChimahonLibraryBulkActionResult(
            mangaCount = 0,
            chapterCount = normalizedChapterIds.size,
        )
    }

    suspend fun loadReaderChapter(
        request: ChimahonReaderRequest,
    ): ChimahonReaderChapter {
        val source = platformServices.sourceRegistry
            .getCatalogueSources()
            .firstOrNull { it.id == request.sourceId }
            ?: error("Source ${request.sourceId} is not loaded.")
        val chapter = SChapter(
            url = request.chapterUrl,
            name = request.chapterName,
            date_upload = request.dateUpload,
            chapter_number = request.chapterNumber.toFloat(),
            scanlator = request.scanlator,
        )
        val pages = source.getPageList(chapter)
        require(pages.isNotEmpty()) {
            "${source.name} returned no pages for ${request.chapterName}."
        }
        return ChimahonReaderChapter(
            request = request,
            pages = pages.mapIndexed { index, page ->
                ChimahonReaderPage(
                    sourceId = source.id,
                    index = index,
                    url = page.url,
                    imageUrl = page.imageUrl,
                )
            },
        )
    }

    suspend fun loadReaderPageImage(
        readerPage: ChimahonReaderPage,
    ): ByteArray {
        val source = platformServices.sourceRegistry
            .getCatalogueSources()
            .firstOrNull { it.id == readerPage.sourceId }
            ?: error("Source ${readerPage.sourceId} is not loaded.")
        return fetchSourcePageImage(
            source = source,
            page = Page(
                index = readerPage.index,
                url = readerPage.url,
                imageUrl = readerPage.imageUrl,
            ),
        )
    }

    suspend fun loadThumbnailImage(url: String): ByteArray {
        require(url.isNotBlank()) { "Thumbnail URL is empty." }
        thumbnailCacheMutex.withLock {
            thumbnailCache[url]?.let { return it }
        }
        val bytes = fetchBytes(url)
        thumbnailCacheMutex.withLock {
            if (thumbnailCache.size >= MAX_THUMBNAIL_CACHE_ENTRIES) {
                thumbnailCache.keys.firstOrNull()?.let(thumbnailCache::remove)
            }
            thumbnailCache[url] = bytes
        }
        return bytes
    }

    suspend fun loadSettings(): ChimahonSettings {
        return settingsRepository.loadSettings()
    }

    suspend fun saveAppearanceSettings(
        settings: ChimahonAppearanceSettings,
    ): ChimahonAppearanceSettings {
        return settingsRepository.saveAppearanceSettings(settings)
    }

    suspend fun saveReaderSettings(settings: ChimahonReaderSettings): ChimahonReaderSettings {
        return settingsRepository.saveReaderSettings(settings)
    }

    suspend fun saveLibrarySettings(settings: ChimahonLibrarySettings): ChimahonLibrarySettings {
        return settingsRepository.saveLibrarySettings(settings)
    }

    suspend fun saveDownloadPreferences(
        settings: ChimahonDownloadPreferences,
    ): ChimahonDownloadPreferences {
        return settingsRepository.saveDownloadSettings(settings)
    }

    suspend fun saveBrowseSettings(settings: ChimahonBrowseSettings): ChimahonBrowseSettings {
        return settingsRepository.saveBrowseSettings(settings)
    }

    suspend fun saveTrackingSettings(settings: ChimahonTrackingSettings): ChimahonTrackingSettings {
        return settingsRepository.saveTrackingSettings(settings)
    }

    suspend fun saveConnectionSettings(settings: ChimahonConnectionSettings): ChimahonConnectionSettings {
        return settingsRepository.saveConnectionSettings(settings)
    }

    suspend fun saveDictionarySettings(settings: ChimahonDictionarySettings): ChimahonDictionarySettings {
        return settingsRepository.saveDictionarySettings(settings)
    }

    suspend fun saveSecuritySettings(settings: ChimahonSecuritySettings): ChimahonSecuritySettings {
        return settingsRepository.saveSecuritySettings(settings)
    }

    suspend fun setDownloadedOnly(enabled: Boolean): ChimahonAppModeSettings {
        return settingsRepository.setDownloadedOnly(enabled)
    }

    suspend fun setIncognitoMode(enabled: Boolean): ChimahonAppModeSettings {
        return settingsRepository.setIncognitoMode(enabled)
    }

    suspend fun loadServiceSettings(): ChimahonServiceSettings {
        return serviceSettingsRepository.load()
    }

    suspend fun loadUiSettings(): ChimahonUiSettings {
        return serviceSettingsRepository.loadUiSettings()
    }

    suspend fun saveUiSettings(settings: ChimahonUiSettings): ChimahonUiSettings {
        return serviceSettingsRepository.saveUiSettings(settings)
    }

    suspend fun setKeepReaderControlsVisible(enabled: Boolean): ChimahonUiSettings {
        return serviceSettingsRepository.saveUiSettings(
            serviceSettingsRepository.loadUiSettings().copy(
                keepReaderControlsVisible = enabled,
            ),
        )
    }

    suspend fun setRightToLeftByDefault(enabled: Boolean): ChimahonUiSettings {
        return serviceSettingsRepository.saveUiSettings(
            serviceSettingsRepository.loadUiSettings().copy(
                rightToLeftByDefault = enabled,
            ),
        )
    }

    suspend fun setShowUnreadBadges(enabled: Boolean): ChimahonUiSettings {
        return serviceSettingsRepository.saveUiSettings(
            serviceSettingsRepository.loadUiSettings().copy(
                showUnreadBadges = enabled,
            ),
        )
    }

    suspend fun setShowCategoryTabs(enabled: Boolean): ChimahonUiSettings {
        return serviceSettingsRepository.saveUiSettings(
            serviceSettingsRepository.loadUiSettings().copy(
                showCategoryTabs = enabled,
            ),
        )
    }

    suspend fun setShowHiddenCategories(enabled: Boolean): ChimahonUiSettings {
        return serviceSettingsRepository.saveUiSettings(
            serviceSettingsRepository.loadUiSettings().copy(
                showHiddenCategories = enabled,
            ),
        )
    }

    suspend fun loadDownloadSettings(): ChimahonDownloadSettings {
        return serviceSettingsRepository.loadDownloadSettings()
    }

    suspend fun saveDownloadSettings(
        settings: ChimahonDownloadSettings,
    ): ChimahonDownloadSettings {
        return serviceSettingsRepository.saveDownloadSettings(settings)
    }

    suspend fun loadDownloadQueue(): ChimahonDownloadQueueData {
        return downloadQueueRepository.load()
    }

    suspend fun loadDownloadSnapshot(): ChimahonDownloadSnapshot {
        val state = loadServiceDatabaseState()
        val queue = downloadQueueRepository.load()
        return buildChimahonDownloadSnapshot(
            queue = queue,
            downloadedIndex = state.downloadedIndex,
        )
    }

    suspend fun enqueueDownload(chapterId: Long): ChimahonDownloadQueueData {
        return enqueueDownloads(listOf(chapterId))
    }

    suspend fun enqueueDownloads(chapterIds: Collection<Long>): ChimahonDownloadQueueData {
        val requestedIds = chapterIds.toSet()
        if (requestedIds.isEmpty()) return downloadQueueRepository.load()
        val state = loadServiceDatabaseState()
        val mangaById = state.allManga.associateBy(Mangas::_id)
        val paused = downloadQueueRepository.load().paused
        val entries = state.chapters
            .asSequence()
            .filter { it._id in requestedIds }
            .mapNotNull { chapter ->
                val manga = mangaById[chapter.manga_id] ?: return@mapNotNull null
                ChimahonDownloadQueueEntry(
                    id = chapter._id.toString(),
                    mangaId = manga._id,
                    chapterId = chapter._id,
                    sourceId = manga.source,
                    mangaTitle = manga.title,
                    chapterName = chapter.name,
                    chapterUrl = chapter.url,
                    status = if (paused) {
                        ChimahonDownloadState.Paused
                    } else {
                        ChimahonDownloadState.Queued
                    },
                    addedAt = platformServices.currentTimeMillis(),
                )
            }
            .toList()
        require(entries.size == requestedIds.size) {
            "One or more chapters are not present in the shared database."
        }
        return downloadQueueRepository.enqueue(entries)
            .also(downloadBackgroundCoordinator::scheduleIfNeeded)
    }

    suspend fun removeDownload(chapterId: Long): ChimahonDownloadQueueData {
        return removeDownloads(listOf(chapterId))
    }

    suspend fun removeDownloads(chapterIds: Collection<Long>): ChimahonDownloadQueueData {
        return downloadQueueRepository.remove(chapterIds.toSet())
    }

    suspend fun reorderDownloads(chapterIds: List<Long>): ChimahonDownloadQueueData {
        return downloadQueueRepository.reorder(chapterIds)
    }

    suspend fun moveDownloadToTop(chapterId: Long): ChimahonDownloadQueueData {
        return downloadQueueRepository.moveToTop(chapterId)
    }

    suspend fun moveDownloadToBottom(chapterId: Long): ChimahonDownloadQueueData {
        return downloadQueueRepository.moveToBottom(chapterId)
    }

    suspend fun clearDownloadQueue(): ChimahonDownloadQueueData {
        return downloadQueueRepository.clear(completedOnly = false)
    }

    suspend fun clearCompletedDownloads(): ChimahonDownloadQueueData {
        return downloadQueueRepository.clear(completedOnly = true)
    }

    suspend fun setDownloadQueuePaused(paused: Boolean): ChimahonDownloadQueueData {
        return downloadQueueRepository.setPaused(paused)
            .also(downloadBackgroundCoordinator::scheduleIfNeeded)
    }

    suspend fun pauseDownloadQueue(): ChimahonDownloadQueueData {
        return setDownloadQueuePaused(true)
    }

    suspend fun resumeDownloadQueue(): ChimahonDownloadQueueData {
        return setDownloadQueuePaused(false)
    }

    suspend fun pauseDownload(chapterId: Long): ChimahonDownloadQueueData {
        return downloadQueueRepository.pause(chapterId)
    }

    suspend fun resumeDownload(chapterId: Long): ChimahonDownloadQueueData {
        return downloadQueueRepository.resume(chapterId)
            .also(downloadBackgroundCoordinator::scheduleIfNeeded)
    }

    suspend fun claimNextDownload(): ChimahonDownloadQueueEntry? {
        return downloadQueueRepository.claimNext()
    }

    suspend fun retryDownload(chapterId: Long): ChimahonDownloadQueueData {
        return downloadQueueRepository.retry(chapterId)
            .also(downloadBackgroundCoordinator::scheduleIfNeeded)
    }

    suspend fun updateDownloadProgress(
        chapterId: Long,
        progress: Int,
        downloadedBytes: Long = 0L,
        totalBytes: Long? = null,
        status: ChimahonDownloadState = ChimahonDownloadState.Downloading,
    ): ChimahonDownloadQueueData {
        return downloadQueueRepository.update(chapterId) { entry ->
            entry.copy(
                status = status,
                progress = if (status == ChimahonDownloadState.Downloaded) 100 else progress,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                errorMessage = null,
            )
        }
    }

    suspend fun markDownloadCompleted(
        chapterId: Long,
        downloadedBytes: Long = 0L,
    ): ChimahonDownloadQueueData {
        return updateDownloadProgress(
            chapterId = chapterId,
            progress = 100,
            downloadedBytes = downloadedBytes,
            totalBytes = downloadedBytes.takeIf { it > 0L },
            status = ChimahonDownloadState.Downloaded,
        )
    }

    suspend fun markDownloadFailed(
        chapterId: Long,
        message: String,
    ): ChimahonDownloadQueueData {
        return downloadQueueRepository.update(chapterId) { entry ->
            entry.copy(
                status = ChimahonDownloadState.Error,
                errorMessage = message.trim().takeIf(String::isNotEmpty) ?: "Download failed.",
            )
        }
    }

    suspend fun processNextDownload(): ChimahonDownloadQueueData {
        val claimed = claimNextDownload() ?: return loadDownloadQueue()
        return try {
            val context = loadDownloadProcessingContext(claimed)
            updateDownloadProgress(
                chapterId = claimed.chapterId,
                progress = 0,
                downloadedBytes = 0L,
                totalBytes = null,
            )

            val pages = context.source.getPageList(context.chapter)
            require(pages.isNotEmpty()) {
                "${context.source.name} returned no pages for ${context.chapter.name}."
            }

            val payloads = mutableListOf<ChimahonChapterPagePayload>()
            var downloadedBytes = 0L
            pages.forEachIndexed { index, page ->
                currentCoroutineContext().ensureActive()
                val bytes = fetchSourcePageImage(context.source, page)
                payloads += ChimahonChapterPagePayload(bytes = bytes)
                downloadedBytes += bytes.size.toLong()
                updateDownloadProgress(
                    chapterId = claimed.chapterId,
                    progress = downloadPageProgress(index + 1, pages.size),
                    downloadedBytes = downloadedBytes,
                    totalBytes = null,
                )
            }

            updateDownloadProgress(
                chapterId = claimed.chapterId,
                progress = DOWNLOAD_WRITING_PROGRESS_PERCENT,
                downloadedBytes = downloadedBytes,
                totalBytes = downloadedBytes.takeIf { it > 0L },
            )
            val writer = ChimahonChapterDownloadWriter(
                downloadsRoot = platformServices.storageDirectories.defaultDownloadsDir(APP_NAME),
            )
            when (val result = writer.write(context.metadata, payloads)) {
                is ChimahonChapterDownloadResult.Success -> {
                    markDownloadCompleted(
                        chapterId = claimed.chapterId,
                        downloadedBytes = result.bytesWritten,
                    )
                }
                is ChimahonChapterDownloadResult.Failure -> {
                    markDownloadFailed(
                        chapterId = claimed.chapterId,
                        message = result.message,
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            markDownloadFailed(
                chapterId = claimed.chapterId,
                message = error.downloadFailureMessage(),
            )
        }
    }

    suspend fun processDownloadQueue(): ChimahonDownloadQueueData {
        var queue = loadDownloadQueue()
        while (queue.hasRunnableDownloads()) {
            queue = processNextDownload()
        }
        return queue
    }

    suspend fun saveReaderProgress(
        request: ChimahonReaderRequest,
        pageIndex: Int,
        completed: Boolean,
    ) {
        val chapterId = request.chapterId ?: return
        if (settingsRepository.isIncognitoModeEnabled()) return
        readerProgressMutex.withLock {
            val elapsedMillis = readerProgressMarks[chapterId]
                ?.elapsedNow()
                ?.inWholeMilliseconds
                ?.coerceAtLeast(0L)
                ?: 0L
            readerProgressMarks[chapterId] = TimeSource.Monotonic.markNow()

            val storedChapter = platformServices.databaseHandler.awaitOneOrNull {
                chaptersQueries.getChapterById(chapterId)
            } ?: return@withLock
            val isRead = storedChapter.read || completed
            platformServices.databaseHandler.await(inTransaction = true) {
                chaptersQueries.update(
                    mangaId = null,
                    url = null,
                    name = null,
                    scanlator = null,
                    read = true.takeIf { completed },
                    bookmark = null,
                    lastPageRead = if (isRead) 0L else pageIndex.coerceAtLeast(0).toLong(),
                    chapterNumber = null,
                    sourceOrder = null,
                    dateFetch = null,
                    dateUpload = null,
                    version = null,
                    isSyncing = 0L,
                    ocrReady = null,
                    chapterId = chapterId,
                )
                historyQueries.upsert(
                    chapterId = chapterId,
                    readAt = platformServices.currentTimeMillis(),
                    time_read = elapsedMillis,
                )
            }
        }
    }

    private suspend fun loadServiceDatabaseState(): ServiceDatabaseState {
        val allManga = platformServices.databaseHandler.awaitList {
            mangasQueries.getAllManga()
        }
        val chapters = if (allManga.isEmpty()) {
            emptyList()
        } else {
            platformServices.databaseHandler.awaitList {
                ehQueries.getChaptersByMangaIds(allManga.map(Mangas::_id))
            }
        }
        val libraryManga = allManga.filter(Mangas::favorite)
        val categories = platformServices.databaseHandler.awaitList {
            categoriesQueries.getCategories()
        }
        val libraryCategoryMemberships = libraryManga.associate { manga ->
            val categoryIds = platformServices.databaseHandler.awaitList {
                categoriesQueries.getCategoriesByMangaId(manga._id)
            }.map(GetCategoriesByMangaId::id)
            manga._id to categoryIds.ifEmpty { listOf(DEFAULT_LIBRARY_CATEGORY_ID) }
        }
        val sourceDirectoryNames = runCatching {
            platformServices.sourceRegistry.getCatalogueSources().associate { source ->
                source.id to setOf(
                    source.toString(),
                    source.name,
                    "${source.name} (${source.lang.uppercase()})",
                )
            }
        }.getOrDefault(emptyMap())
        val downloadedIndex = runCatching {
            val downloadsDirectory = platformServices.storageDirectories.defaultDownloadsDir(APP_NAME)
            ChimahonDownloadStorage(downloadsDirectory).scanDownloadedChapters(
                manga = allManga,
                chapters = chapters,
                sourceDirectoryNames = sourceDirectoryNames,
            )
        }.getOrDefault(ChimahonDownloadedIndex.Empty)

        return ServiceDatabaseState(
            allManga = allManga,
            libraryManga = libraryManga,
            chapters = chapters,
            chaptersByMangaId = chapters.groupBy(Chapters::manga_id),
            categories = categories,
            libraryCategoryMemberships = libraryCategoryMemberships,
            downloadedIndex = downloadedIndex,
        )
    }

    private fun Mangas.toLibraryMangaData(
        state: ServiceDatabaseState,
    ): ChimahonLibraryMangaData {
        val mangaChapters = state.chaptersByMangaId[_id].orEmpty()
            .sortedWith(
                compareBy<Chapters> { it.source_order }
                    .thenBy { it.chapter_number },
            )
        return ChimahonLibraryMangaData(
            manga = toSharedMangaEntry(),
            chapters = mangaChapters.map(Chapters::toSharedChapterEntry),
            categoryIds = state.libraryCategoryMemberships[_id]
                .orEmpty()
                .ifEmpty { listOf(DEFAULT_LIBRARY_CATEGORY_ID) },
            unreadChapterCount = mangaChapters.count { !it.read },
            readChapterCount = mangaChapters.count(Chapters::read),
            bookmarkedChapterCount = mangaChapters.count(Chapters::bookmark),
            downloadedChapterCount = mangaChapters.count { it._id in state.downloadedIndex.chapterIds },
        )
    }

    private suspend fun loadDownloadedOnlySetting(): Boolean {
        return runCatching {
            settingsRepository.loadAppModeSettings().downloadedOnly
        }.getOrDefault(false)
    }

    private suspend fun loadShowHiddenCategoriesSetting(): Boolean {
        return runCatching {
            serviceSettingsRepository.loadUiSettings().showHiddenCategories
        }.getOrDefault(false)
    }

    private suspend fun loadDownloadProcessingContext(
        entry: ChimahonDownloadQueueEntry,
    ): DownloadProcessingContext {
        val manga = platformServices.databaseHandler.awaitOneOrNull {
            mangasQueries.getMangaById(entry.mangaId)
        } ?: error("Manga ${entry.mangaId} is not present in the shared database.")
        val storedChapter = platformServices.databaseHandler.awaitOneOrNull {
            chaptersQueries.getChapterById(entry.chapterId)
        } ?: error("Chapter ${entry.chapterId} is not present in the shared database.")
        require(storedChapter.manga_id == manga._id) {
            "Chapter ${entry.chapterId} does not belong to manga ${entry.mangaId}."
        }
        val source = platformServices.sourceRegistry
            .getCatalogueSources()
            .firstOrNull { it.id == manga.source }
            ?: error("Source ${manga.source} is not loaded.")
        val chapter = SChapter(
            url = storedChapter.url.ifBlank { entry.chapterUrl },
            name = storedChapter.name.ifBlank { entry.chapterName },
            date_upload = storedChapter.date_upload,
            chapter_number = storedChapter.chapter_number.toFloat(),
            scanlator = storedChapter.scanlator,
        )
        return DownloadProcessingContext(
            source = source,
            chapter = chapter,
            metadata = ChimahonChapterDownloadMetadata(
                sourceName = source.toString(),
                mangaTitle = manga.title.ifBlank { entry.mangaTitle },
                chapterName = chapter.name.ifBlank { entry.chapterName },
                scanlator = chapter.scanlator,
                collisionSuffix = chapter.url.stableDownloadCollisionSuffix(),
            ),
        )
    }

    private suspend fun databaseMaintenanceResult(
        before: ChimahonDatabaseMaintenanceData,
    ): ChimahonDatabaseMaintenanceResult {
        val after = loadDatabaseMaintenanceData()
        return ChimahonDatabaseMaintenanceResult(
            historyEntriesRemoved = (
                before.historyEntryCount - after.historyEntryCount
                ).coerceAtLeast(0),
            updateIssuesRemoved = (
                before.updateIssues.totalIssueCount - after.updateIssues.totalIssueCount
                ).coerceAtLeast(0),
            remainingHistoryEntryCount = after.historyEntryCount,
            remainingUpdateIssueCount = after.updateIssues.totalIssueCount,
        )
    }

    fun close() {
        httpClient.close()
        platformServices.apkExtensionManager.close()
        platformServices.close()
    }

    private suspend fun loadExtensions(): ExtensionStartupState {
        return extensionMutationMutex.withLock {
            refreshExtensionsLocked()
        }
    }

    private suspend fun refreshExtensionsLocked(): ExtensionStartupState {
        val errors = mutableListOf<String>()
        runCatching {
            scriptExtensionManager.reload()
                .map(LoadedScriptExtension::toSharedInstalledExtensionEntry)
        }
            .onSuccess { knownScriptExtensions = it }
            .onFailure { error ->
                errors += "JavaScript extension reload failed: ${error.message ?: "unknown error"}"
            }
        runCatching {
            platformServices.apkExtensionManager.reload()
        }
            .onSuccess { knownApkExtensions = it }
            .onFailure { error ->
                errors += "APK extension reload failed: ${error.message ?: "unknown error"}"
            }
        errors += platformServices.apkExtensionManager.status().errors
        val installedExtensions = (knownScriptExtensions + knownApkExtensions)
            .distinctBy { it.id to it.packageType }
            .sortedBy { it.name.lowercase() }
        val sourceCount = platformServices.sourceRegistry.sources.value.size
        val label = if (errors.isEmpty()) {
            "$sourceCount catalogue sources"
        } else {
            "$sourceCount catalogue sources; ${errors.size} extension issue(s)"
        }
        return ExtensionStartupState(
            extensionCount = installedExtensions.size,
            label = label,
            installedExtensions = installedExtensions,
            errors = errors.distinct(),
        )
    }

    private fun rememberInstalledExtension(extension: ChimahonInstalledExtensionEntry) {
        val current = when (extension.packageType) {
            ChimahonExtensionPackageType.JavaScript -> knownScriptExtensions
            ChimahonExtensionPackageType.AndroidApk -> knownApkExtensions
        }
        val updated = (current.filterNot { it.id == extension.id } + extension)
            .sortedBy { it.name.lowercase() }
        when (extension.packageType) {
            ChimahonExtensionPackageType.JavaScript -> knownScriptExtensions = updated
            ChimahonExtensionPackageType.AndroidApk -> knownApkExtensions = updated
        }
    }

    private fun forgetInstalledExtension(
        extensionId: String,
        packageType: ChimahonExtensionPackageType,
    ) {
        when (packageType) {
            ChimahonExtensionPackageType.JavaScript -> {
                knownScriptExtensions = knownScriptExtensions.filterNot { it.id == extensionId }
            }
            ChimahonExtensionPackageType.AndroidApk -> {
                knownApkExtensions = knownApkExtensions.filterNot { it.id == extensionId }
            }
        }
    }

    private data class ExtensionStartupState(
        val extensionCount: Int,
        val label: String,
        val installedExtensions: List<ChimahonInstalledExtensionEntry>,
        val errors: List<String>,
    )

    private data class ServiceDatabaseState(
        val allManga: List<Mangas>,
        val libraryManga: List<Mangas>,
        val chapters: List<Chapters>,
        val chaptersByMangaId: Map<Long, List<Chapters>>,
        val categories: List<GetCategories>,
        val libraryCategoryMemberships: Map<Long, List<Long>>,
        val downloadedIndex: ChimahonDownloadedIndex,
    )

    private data class DownloadProcessingContext(
        val source: CatalogueSource,
        val chapter: SChapter,
        val metadata: ChimahonChapterDownloadMetadata,
    )
}

internal expect class ChimahonPlatformServices() {
    val platformName: String
    val backgroundState: String
    val storageDirectories: PlatformStorageDirectories
    val sourceRegistry: SourceRegistry
    val database: Database
    val databaseHandler: DatabaseHandler
    val javaScriptRuntimeFactory: JavaScriptRuntimeFactory
    val apkExtensionManager: ChimahonPlatformApkExtensionManager

    fun createBackgroundTaskScheduler(workerRegistry: BackgroundWorkerRegistry): BackgroundTaskScheduler
    fun currentTimeMillis(): Long
    fun resolveExternalMangaUrl(source: CatalogueSource, manga: SManga): String?
    fun openExternalUrl(url: String): Boolean
    fun close()
}

internal fun createDatabase(driver: SqlDriver): Database {
    return Database(
        driver = driver,
        mangasAdapter = Mangas.Adapter(
            genreAdapter = StringListColumnAdapter,
        ),
    )
}

private fun Mangas.toSharedMangaEntry(): ChimahonMangaEntry {
    return ChimahonMangaEntry(
        id = _id,
        sourceId = source,
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genres = genre.orEmpty(),
        status = mangaStatus(status),
        thumbnailUrl = thumbnail_url,
        favorite = favorite,
        initialized = initialized,
        dateAdded = date_added,
        lastUpdate = last_update,
        notes = notes,
    )
}

internal fun SManga.withSeedFallback(seed: SManga): SManga {
    if (safeUrl().isBlank()) {
        url = seed.safeUrl()
    }
    if (title.isBlank()) {
        title = seed.title
    }
    if (thumbnail_url.isNullOrBlank()) {
        thumbnail_url = seed.thumbnail_url
    }
    if (author.isNullOrBlank()) {
        author = seed.author
    }
    initialized = true
    return this
}

private fun Throwable.isUninitializedSourceUrlFailure(): Boolean {
    val detail = message.orEmpty()
    return detail.contains("url", ignoreCase = true) &&
        detail.contains("initialized", ignoreCase = true)
}

private fun SManga.safeUrl(fallback: String = ""): String {
    return runCatching { url }
        .getOrNull()
        .orEmpty()
        .ifBlank { fallback }
}

private fun SChapter.safeUrl(fallback: String = ""): String {
    return runCatching { url }
        .getOrNull()
        .orEmpty()
        .ifBlank { fallback }
}

private fun SChapter.safeName(): String {
    return runCatching { name }
        .getOrNull()
        .orEmpty()
        .ifBlank { "Chapter" }
}

private fun CatalogueSource.toSharedSourceEntry(): ChimahonSourceEntry {
    return ChimahonSourceEntry(
        id = id,
        name = name,
        language = lang,
        supportsLatest = supportsLatest,
    )
}

private fun UpdatesView.toSharedUpdateEntry(): ChimahonRecentUpdateEntry {
    return ChimahonRecentUpdateEntry(
        mangaId = mangaId,
        chapterId = chapterId,
        mangaTitle = mangaTitle,
        chapterName = chapterName,
        scanlator = scanlator,
        sourceId = source,
        read = read,
        bookmarked = bookmark,
        lastPageRead = last_page_read,
        dateFetch = datefetch,
    )
}

private fun History.toSharedHistoryEntry(): ChimahonHistoryEntry {
    return ChimahonHistoryEntry(
        id = id,
        mangaId = mangaId,
        chapterId = chapterId,
        title = title,
        sourceId = source,
        chapterNumber = chapterNumber,
        read = read,
        lastPageRead = lastPageRead,
        totalCount = totalCountCalculated,
        readCount = readCountCalculated,
        readAt = readAt,
        readDuration = readDuration,
    )
}

private fun Chapters.toSharedChapterEntry(): ChimahonChapterEntry {
    return ChimahonChapterEntry(
        id = _id,
        mangaId = manga_id,
        url = url,
        name = name,
        scanlator = scanlator,
        read = read,
        bookmarked = bookmark,
        lastPageRead = last_page_read,
        chapterNumber = chapter_number,
        sourceOrder = source_order,
        dateFetch = date_fetch,
        dateUpload = date_upload,
    )
}

private fun GetCategories.toSharedLibraryCategory(): ChimahonLibraryCategory {
    return ChimahonLibraryCategory(
        id = id,
        name = name,
        order = order,
        hidden = hidden != 0L,
    )
}

private fun Extension_repos.toSharedExtensionRepoEntry(): ChimahonExtensionRepoEntry {
    return ChimahonExtensionRepoEntry(
        baseUrl = base_url,
        name = name,
        shortName = short_name,
        website = website,
        signingKeyFingerprint = signing_key_fingerprint,
    )
}

private fun LoadedScriptExtension.toRepoExtensionEntry(
    repoBaseUrl: String,
    scriptUrl: String,
): ChimahonRepoExtensionEntry {
    return ChimahonRepoExtensionEntry(
        repoBaseUrl = repoBaseUrl,
        id = manifest.id,
        name = manifest.name,
        version = manifest.version,
        artifactUrl = scriptUrl,
    )
}

private fun LoadedScriptExtension.toSharedInstalledExtensionEntry(): ChimahonInstalledExtensionEntry {
    return ChimahonInstalledExtensionEntry(
        id = manifest.id,
        name = manifest.name,
        version = manifest.version,
        sourceCount = manifest.sources.size,
        packageType = ChimahonExtensionPackageType.JavaScript,
    )
}

private fun SManga.toRemoteMangaEntry(sourceId: Long): ChimahonRemoteMangaEntry {
    return ChimahonRemoteMangaEntry(
        sourceId = sourceId,
        title = title,
        author = author ?: artist,
        status = mangaStatus(status.toLong()),
        url = safeUrl(),
        thumbnailUrl = thumbnail_url,
        initialized = initialized,
    )
}

private fun SChapter.toRemoteChapterEntry(): ChimahonRemoteChapterEntry {
    return ChimahonRemoteChapterEntry(
        name = safeName(),
        url = safeUrl(),
        chapterNumber = chapter_number.toDouble(),
        scanlator = scanlator,
        dateUpload = date_upload,
    )
}

private fun normalizeExtensionRepoBaseUrl(input: String): String {
    val cleaned = input.trim().trimEnd('/')
    require(cleaned.startsWith("https://")) { "Extension repo URL must start with https://." }
    val baseUrl = cleaned
        .removeSuffix("/chimahon.json")
        .removeSuffix("/index.json")
        .removeSuffix("/index.min.json")
        .trimEnd('/')
    require(baseUrl.length > "https://".length) { "Extension repo URL is empty." }
    return baseUrl
}

internal fun repoCatalogUrls(baseUrl: String): List<String> {
    return listOf(
        "$baseUrl/chimahon.json",
        "$baseUrl/index.min.json",
        "$baseUrl/index.json",
    )
}

internal fun buildChimahonDownloadSnapshot(
    queue: ChimahonDownloadQueueData,
    downloadedIndex: ChimahonDownloadedIndex,
): ChimahonDownloadSnapshot {
    val chaptersById = linkedMapOf<Long, ChimahonChapterDownloadStatus>()
    downloadedIndex.chapterPaths.keys.sorted().forEach { chapterId ->
        val downloadedBytes = downloadedIndex.chapterSizes[chapterId] ?: 0L
        chaptersById[chapterId] = ChimahonChapterDownloadStatus(
            chapterId = chapterId,
            status = ChimahonDownloadState.Downloaded,
            progress = 100,
            downloadedBytes = downloadedBytes,
            totalBytes = downloadedBytes.takeIf { it > 0L },
            downloadedOnDisk = true,
        )
    }

    queue.entries.forEach { entry ->
        val downloadedStatus = chaptersById[entry.chapterId]
        val downloadedBytes = when {
            entry.downloadedBytes > 0L -> entry.downloadedBytes
            downloadedStatus != null -> downloadedStatus.downloadedBytes
            else -> 0L
        }
        chaptersById[entry.chapterId] = ChimahonChapterDownloadStatus(
            chapterId = entry.chapterId,
            status = entry.status,
            progress = if (entry.status == ChimahonDownloadState.Downloaded) {
                100
            } else {
                entry.progress.coerceIn(0, 100)
            },
            downloadedBytes = downloadedBytes,
            totalBytes = entry.totalBytes
                ?: downloadedStatus?.totalBytes
                ?: downloadedBytes.takeIf { entry.status == ChimahonDownloadState.Downloaded && it > 0L },
            downloadedOnDisk = downloadedStatus?.downloadedOnDisk == true,
            errorMessage = entry.errorMessage,
        )
    }

    return ChimahonDownloadSnapshot(
        chaptersById = chaptersById.toMap(),
        queue = queue,
    )
}

private fun JsonObject.stringValue(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key ->
        runCatching { this[key]?.jsonPrimitive?.content }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}

private fun JsonObject.intValue(key: String): Int? {
    return runCatching { this[key]?.jsonPrimitive?.content?.toInt() }.getOrNull()
}

internal fun parseRepoExtensions(
    json: Json,
    repoBaseUrl: String,
    payload: String,
): List<ChimahonRepoExtensionEntry> {
    val root = json.parseToJsonElement(payload)
    val entries = when (root) {
        is JsonArray -> root
        is JsonObject -> {
            (root["extensions"] as? JsonArray)
                ?: (root["packages"] as? JsonArray)
                ?: JsonArray(emptyList())
        }
        else -> JsonArray(emptyList())
    }
    val objects = entries.mapNotNull { it as? JsonObject }
    val scriptEntries = objects.mapNotNull { entry ->
        val scriptPath = entry.stringValue(
            "scriptUrl",
            "script_url",
            "downloadUrl",
            "download_url",
            "url",
            "script",
        )?.takeIf { it.startsWith("https://") || it.startsWith("http://") || it.endsWith(".js") }
            ?: return@mapNotNull null
        val scriptUrl = resolveRepoUrl(repoBaseUrl, scriptPath)
        val fallbackId = scriptUrl.substringAfterLast("/").substringBeforeLast(".")
        ChimahonRepoExtensionEntry(
            repoBaseUrl = repoBaseUrl,
            id = entry.stringValue("id", "pkg", "package") ?: fallbackId,
            name = entry.stringValue("name") ?: fallbackId,
            version = entry.stringValue("version", "versionName", "version_name") ?: "remote",
            artifactUrl = scriptUrl,
        )
    }
    val apkEntries = objects.mapNotNull { entry ->
        val apkName = entry.stringValue("apk") ?: return@mapNotNull null
        val packageName = entry.stringValue("pkg", "package", "id") ?: return@mapNotNull null
        val name = entry.stringValue("name")
            ?.substringAfter("Tachiyomi: ")
            ?.substringAfter("Mihon: ")
            ?: packageName.substringAfterLast(".")
        ChimahonRepoExtensionEntry(
            repoBaseUrl = repoBaseUrl,
            id = packageName,
            name = name,
            version = entry.stringValue("version", "versionName", "version_name") ?: "remote",
            artifactUrl = resolveRepoUrl(repoBaseUrl, "apk/$apkName"),
            packageType = ChimahonExtensionPackageType.AndroidApk,
            language = entry.stringValue("lang", "language").orEmpty(),
            sourceCount = (entry["sources"] as? JsonArray)?.size ?: 0,
            isNsfw = entry.intValue("nsfw") == 1,
        )
    }
    return scriptEntries + apkEntries
}

private fun resolveRepoUrl(baseUrl: String, value: String): String {
    if (value.startsWith("https://") || value.startsWith("http://")) return value
    if (value.startsWith("/")) {
        return "https://${baseUrl.removePrefix("https://").substringBefore("/")}$value"
    }
    return "${baseUrl.trimEnd('/')}/${value.trimStart('/')}"
}

private fun String.normalizedExternalWebUrlOrNull(): String? {
    val normalized = trim()
    if (normalized.any(Char::isWhitespace)) return null
    return normalized.takeIf {
        it.startsWith("https://", ignoreCase = true) ||
            it.startsWith("http://", ignoreCase = true)
    }
}

private fun String.repoHost(): String {
    return removePrefix("https://")
        .removePrefix("http://")
        .substringBefore("/")
        .ifBlank { "Extension repo" }
}

private fun String.stableRepoHash(): String {
    var hash = 1125899906842597L
    forEach { character ->
        hash = (hash * 31) + character.code
    }
    return hash.toString().replace("-", "n")
}

private fun String.stableDownloadCollisionSuffix(): String {
    var hash = 1125899906842597L
    forEach { character ->
        hash = (hash * 31) + character.code
    }
    var value = hash and Long.MAX_VALUE
    val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
    return buildString(DOWNLOAD_COLLISION_SUFFIX_LENGTH) {
        repeat(DOWNLOAD_COLLISION_SUFFIX_LENGTH) {
            append(alphabet[(value % alphabet.length).toInt()])
            value /= alphabet.length
        }
    }.reversed()
}

private fun downloadPageProgress(
    pagesDownloaded: Int,
    totalPages: Int,
): Int {
    if (totalPages <= 0) return 0
    val progress = pagesDownloaded.coerceAtLeast(0) * DOWNLOAD_PAGE_FETCH_PROGRESS_PERCENT / totalPages
    return progress.coerceIn(0, DOWNLOAD_PAGE_FETCH_PROGRESS_PERCENT)
}

private fun Throwable.downloadFailureMessage(): String {
    return message?.trim()?.takeIf(String::isNotEmpty) ?: "Download failed."
}

private fun mangaStatus(status: Long): String = when (status) {
    1L -> "Ongoing"
    2L -> "Completed"
    3L -> "Licensed"
    4L -> "Publishing finished"
    5L -> "Cancelled"
    6L -> "On hiatus"
    else -> "Unknown"
}

private fun formatTimestamp(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "Unknown time"
    return "Unix $epochSeconds"
}

private fun ChimahonFileTreeStats.toStorageSection(path: Path): ChimahonStorageSection {
    return ChimahonStorageSection(
        path = path.toString(),
        exists = exists,
        sizeBytes = sizeBytes,
        fileCount = fileCount,
        directoryCount = directoryCount,
    )
}

internal data class ChimahonDirectoryClearResult(
    val bytesRemoved: Long,
    val filesRemoved: Int,
    val directoriesRemoved: Int,
    val failures: List<ChimahonMaintenanceFailure>,
)

internal fun clearDirectoryContents(
    path: Path,
    fileSystem: FileSystem = FileSystem.SYSTEM,
): ChimahonDirectoryClearResult {
    if (path.segments.isEmpty()) {
        return ChimahonDirectoryClearResult(
            bytesRemoved = 0L,
            filesRemoved = 0,
            directoriesRemoved = 0,
            failures = listOf(
                ChimahonMaintenanceFailure(
                    path = path.toString(),
                    reason = "Refusing to clear a filesystem root.",
                ),
            ),
        )
    }

    val before = collectTreeStats(path, fileSystem)
    val failures = mutableListOf<ChimahonMaintenanceFailure>()
    val children = runCatching {
        if (before.exists) fileSystem.list(path) else emptyList()
    }.getOrElse { error ->
        failures += ChimahonMaintenanceFailure(
            path = path.toString(),
            reason = error.message ?: "Unable to list cache directory.",
        )
        emptyList()
    }

    if (failures.isEmpty()) {
        children.forEach { child ->
            runCatching {
                fileSystem.deleteRecursively(child)
            }.onFailure { error ->
                failures += ChimahonMaintenanceFailure(
                    path = child.toString(),
                    reason = error.message ?: "Unable to delete cache entry.",
                )
            }
        }
        runCatching {
            fileSystem.createDirectories(path)
        }.onFailure { error ->
            failures += ChimahonMaintenanceFailure(
                path = path.toString(),
                reason = error.message ?: "Unable to recreate cache directory.",
            )
        }
    }

    val after = collectTreeStats(path, fileSystem)
    return ChimahonDirectoryClearResult(
        bytesRemoved = (before.sizeBytes - after.sizeBytes).coerceAtLeast(0L),
        filesRemoved = (before.fileCount - after.fileCount).coerceAtLeast(0),
        directoriesRemoved = (
            before.directoryCount - after.directoryCount
            ).coerceAtLeast(0),
        failures = failures,
    )
}

internal const val APP_NAME = "chimahon"
internal const val DATABASE_DIRECTORY = "database"
internal const val DATABASE_NAME = "chimahon.db"
internal const val SETTINGS_FILE_NAME = "chimahon.settings"

private const val CHAPTER_SHOW_BOOKMARKED = 0x00000020L
private const val CHAPTER_SHOW_NOT_BOOKMARKED = 0x00000040L
private const val DEFAULT_SERVICE_PAGE_SIZE = 50
private const val DEFAULT_LIBRARY_CATEGORY_ID = 0L
private const val MAX_THUMBNAIL_CACHE_ENTRIES = 192
private const val SCRIPT_EXTENSION_SUFFIX = ".js"
private const val DOWNLOAD_PAGE_FETCH_PROGRESS_PERCENT = 90
private const val DOWNLOAD_WRITING_PROGRESS_PERCENT = 95
private const val DOWNLOAD_COLLISION_SUFFIX_LENGTH = 6
