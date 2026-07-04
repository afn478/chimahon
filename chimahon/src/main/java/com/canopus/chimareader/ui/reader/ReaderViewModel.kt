package com.canopus.chimareader.ui.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.canopus.chimareader.data.BookMetadata
import com.canopus.chimareader.data.BookStorage
import com.canopus.chimareader.data.Bookmark
import com.canopus.chimareader.data.CustomReaderTheme
import com.canopus.chimareader.data.FileNames
import com.canopus.chimareader.data.FontManager
import com.canopus.chimareader.data.NovelReaderSettings
import com.canopus.chimareader.data.Statistics
import com.canopus.chimareader.data.Theme
import com.canopus.chimareader.data.epub.EpubBook
import com.canopus.chimareader.data.epub.TocEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import tachiyomi.domain.reader.model.NovelReaderSettingsSnapshot
import tachiyomi.domain.reader.model.NovelReaderTocEntry
import tachiyomi.domain.reader.model.NovelReaderTocItem
import tachiyomi.domain.reader.model.NovelReaderWebCommand
import tachiyomi.domain.reader.model.ReaderSettings
import tachiyomi.domain.reader.service.NovelReaderNavigationPolicy
import tachiyomi.domain.reader.service.NovelReaderProgressPolicy
import tachiyomi.domain.reader.service.NovelReaderSessionPolicy
import tachiyomi.domain.reader.service.NovelReaderSettingsPolicy
import tachiyomi.domain.reader.service.NovelReaderStatisticsPolicy
import tachiyomi.domain.reader.service.NovelReaderWebCommandPolicy

// ─── Bridge ───────────────────────────────────────────────────────────────────

class WebViewBridge {
    var chapterUrl: String? by mutableStateOf(null)
        private set
    var chapterTitle: String? by mutableStateOf(null)
        private set
    var progress: Double by mutableDoubleStateOf(0.0)
        private set
    val pendingCommands = mutableStateListOf<NovelReaderWebCommand>()

    fun send(command: NovelReaderWebCommand) {
        pendingCommands += command
    }

    private fun updateState(url: String, progress: Double, title: String? = null) {
        chapterUrl = url
        chapterTitle = title
        this.progress = progress
    }

    fun loadChapter(url: String, progress: Double, title: String? = null) {
        val result = NovelReaderWebCommandPolicy.chapterLoadCommand(
            chapterUrl = url,
            progress = progress,
            chapterTitle = title,
        )
        updateState(result.chapterUrl, result.progress, result.chapterTitle)
        send(result.command)
    }

    fun updateProgress(progress: Double) {
        this.progress = progress
    }

    fun highlightSasayakiCue(id: String, reveal: Boolean) {
        send(NovelReaderWebCommandPolicy.highlightSasayakiCueCommand(id, reveal))
    }

    fun clearSasayakiCue() {
        send(NovelReaderWebCommandPolicy.clearSasayakiCueCommand())
    }

    fun paginate(forward: Boolean) {
        send(NovelReaderWebCommandPolicy.paginateCommand(forward))
    }
}

// ─── Loader ───────────────────────────────────────────────────────────────────

class ReaderLoaderViewModel(
    context: Context,
    book: BookMetadata,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    var document: EpubBook? = null
        private set

    val rootUrl: File? =
        book.folder?.let { BookStorage.getBookDirectory(context, it) }

    init {
        loadBook(book, context)
    }

    private fun loadBook(book: BookMetadata, context: Context) {
        val root = rootUrl ?: return
        val doc = BookStorage.loadEpub(root)
        when (
            val action = NovelReaderSessionPolicy.bookOpenAction(
                book = book,
                rootAvailable = true,
                nowMillis = nowMillis(),
            )
        ) {
            NovelReaderSessionPolicy.BookOpenAction.Ignore -> Unit
            is NovelReaderSessionPolicy.BookOpenAction.PersistLastAccess -> {
                BookStorage.save(action.metadata, root, FileNames.metadata)
            }
        }
        document = doc
    }
}

// ─── Reader ───────────────────────────────────────────────────────────────────

class ReaderViewModel(
    val document: EpubBook,
    val rootUrl: File,
    val settings: NovelReaderSettings,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    var index by mutableIntStateOf(0)
    var currentProgress by mutableDoubleStateOf(0.0)

    // Settings state
    var theme by mutableStateOf(Theme.SYSTEM)
    var fontSize by mutableDoubleStateOf(18.0)
    var lineHeight by mutableDoubleStateOf(1.6)
    var horizontalPadding by mutableDoubleStateOf(10.0)
    var verticalPadding by mutableDoubleStateOf(10.0)
    var selectedFont by mutableStateOf("System")
    var continuousMode by mutableStateOf(false)
    var customBackgroundColor by mutableIntStateOf(0xFFF2E2C9.toInt())
    var customTextColor by mutableIntStateOf(0xFF000000.toInt())
    var customThemes by mutableStateOf<List<CustomReaderTheme>>(emptyList())
    var sasayakiPlayer: SasayakiPlayer? by mutableStateOf(null)
    var verticalWriting by mutableStateOf(true)
    var characterSpacing by mutableDoubleStateOf(0.0)
    var paragraphSpacing by mutableDoubleStateOf(0.0)
    var justifyText by mutableStateOf(false)
    var avoidPageBreak by mutableStateOf(true)
    var hideFurigana by mutableStateOf(false)
    var layoutAdvanced by mutableStateOf(false)
    var tapZonePercent by mutableIntStateOf(20)
    var keepScreenOn by mutableStateOf(false)
    var systemLightSepia by mutableStateOf(false)

    // Tracks statistics for current reading session
    var totalExploredCharCount by mutableIntStateOf(0)

    val accumulatedCharCounts = androidx.compose.runtime.mutableStateMapOf<Int, Int>()

    val totalCharacters: Int
        get() = accumulatedCharCounts[document.linearSpineItems.size] ?: 0

    val currentCharacter: Int
        get() = totalExploredCharCount

    val currentChapterEndCharacter: Int
        get() = accumulatedCharCounts.getOrDefault(index + 1, 0)

    var fullStatistics = mutableStateListOf<Statistics>()
    private var lastPersistTimeMs = nowMillis()
    private var lastSavedChapterIndex = 0
    private var lastSavedProgress = 0.0
    private var lastSavedCharacterCount = 0

    val statisticsTracker: ReaderStatisticsTracker
    private var trackingLocked = false
    private var appBackgrounded = false

    val bridge = WebViewBridge()
    val chapterCount = document.spine().items.size
    private val domainTableOfContents = document.tableOfContents.toDomainTocEntries()

    /**
     * Returns true if the current chapter is an image-only page
     */
    val isCurrentChapterImageOnly: Boolean
        get() {
            val spineItem = document.linearSpineItems.getOrNull(index) ?: return false
            return NovelReaderSessionPolicy.isImageOnlySpineItem(spineItem.type)
        }

    /**
     * Gets the image URL for the current chapter if it's image-only
     */
    val currentImageUrl: String?
        get() = document.getImageUrl(index)

    init {
        // Initialize state from settings (blocking first value for initial render)
        runBlocking(Dispatchers.IO) {
            theme = settings.theme.first()
            fontSize = settings.fontSize.first()
            lineHeight = settings.lineHeight.first()
            horizontalPadding = settings.horizontalPadding.first()
            verticalPadding = settings.verticalPadding.first()
            selectedFont = settings.selectedFont.first()
            continuousMode = settings.continuousMode.first()
            customBackgroundColor = settings.customBackgroundColor.first()
            customTextColor = settings.customTextColor.first()
            customThemes = settings.customThemes.first()
            verticalWriting = settings.verticalWriting.first()
            paragraphSpacing = settings.paragraphSpacing.first()
            avoidPageBreak = settings.avoidPageBreak.first()
            hideFurigana = settings.readerHideFurigana.first()
            tapZonePercent = settings.chapterTapZones.first()
            keepScreenOn = settings.keepScreenOn.first()
            systemLightSepia = settings.systemLightSepia.first()
        }

        val bookmark = BookStorage.loadBookmark(rootUrl)
        val restoredBookmark = NovelReaderSessionPolicy.restoredBookmarkState(
            bookmark = bookmark,
            chapterCharacterCount = document::getChapterCharacters,
        )
        index = restoredBookmark.chapterIndex
        currentProgress = restoredBookmark.progress
        totalExploredCharCount = restoredBookmark.characterCount
        lastSavedChapterIndex = index
        lastSavedProgress = currentProgress
        lastSavedCharacterCount = totalExploredCharCount

        val stats = BookStorage.loadStatistics(rootUrl)
        if (stats != null) {
            fullStatistics.addAll(NovelReaderStatisticsPolicy.normalizeLoadedStatistics(stats))
        }

        statisticsTracker = ReaderStatisticsTracker(
            title = NovelReaderSessionPolicy.statisticsTitle(document.title),
            initialStatistics = fullStatistics,
            enabled = true,
            nowMillis = nowMillis,
            dateKeyProvider = ::currentReaderDateKey,
        )

        scope.launch {
            while (true) {
                delay(1000)
                if (
                    NovelReaderSessionPolicy.shouldTickStatistics(
                        trackingLocked = trackingLocked,
                        appBackgrounded = appBackgrounded,
                    )
                ) {
                    statisticsTracker.update(totalExploredCharCount)
                }
                val now = nowMillis()
                if (NovelReaderProgressPolicy.shouldPersistPeriodically(now, lastPersistTimeMs)) {
                    persistToDisk()
                    lastPersistTimeMs = now
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            val chapterCharacterCounts = document.linearSpineItems.indices.map { document.getChapterCharacters(it) }
            NovelReaderProgressPolicy.accumulatedCharacterCounts(chapterCharacterCounts).forEachIndexed { index, count ->
                accumulatedCharCounts[index] = count
            }
        }

        loadCurrentChapterInBridge(currentProgress)

        // Start collecting updates from settings flow in the background
        scope.launch {
            settings.theme.collect { theme = it }
        }
        scope.launch {
            settings.fontSize.collect { fontSize = it }
        }
        scope.launch {
            settings.lineHeight.collect { lineHeight = it }
        }
        scope.launch {
            settings.horizontalPadding.collect { horizontalPadding = it }
        }
        scope.launch {
            settings.verticalPadding.collect { verticalPadding = it }
        }
        scope.launch {
            settings.selectedFont.collect { selectedFont = it }
        }
        scope.launch {
            settings.continuousMode.collect { continuousMode = it }
        }
        scope.launch {
            settings.customBackgroundColor.collect { customBackgroundColor = it }
        }
        scope.launch {
            settings.customTextColor.collect { customTextColor = it }
        }
        scope.launch {
            settings.customThemes.collect { customThemes = it }
        }
        scope.launch {
            settings.verticalWriting.collect { verticalWriting = it }
        }
        scope.launch {
            settings.characterSpacing.collect { characterSpacing = it }
        }
        scope.launch {
            settings.paragraphSpacing.collect { paragraphSpacing = it }
        }
        scope.launch {
            settings.avoidPageBreak.collect { avoidPageBreak = it }
        }
        scope.launch {
            settings.readerHideFurigana.collect { hideFurigana = it }
        }
        scope.launch {
            settings.chapterTapZones.collect { tapZonePercent = it }
        }
        scope.launch {
            settings.justifyText.collect { justifyText = it }
        }
        scope.launch {
            settings.layoutAdvanced.collect { layoutAdvanced = it }
        }
        scope.launch {
            settings.keepScreenOn.collect { keepScreenOn = it }
        }
        scope.launch {
            settings.systemLightSepia.collect { systemLightSepia = it }
        }
    }

    fun updateTheme(value: Theme) = scope.launch { settings.setTheme(value) }
    fun updateFontSize(value: Double) = scope.launch { settings.setFontSize(value) }
    fun updateLineHeight(value: Double) = scope.launch { settings.setLineHeight(value) }
    fun updateHorizontalPadding(value: Double) = scope.launch { settings.setHorizontalPadding(value) }
    fun updateVerticalPadding(value: Double) = scope.launch { settings.setVerticalPadding(value) }
    fun updateSelectedFont(value: String) = scope.launch { settings.setSelectedFont(value) }
    fun updateContinuousMode(value: Boolean) = scope.launch { settings.setContinuousMode(value) }
    fun updateCustomBackgroundColor(value: Int) = scope.launch { settings.setCustomBackgroundColor(value) }
    fun updateCustomTextColor(value: Int) = scope.launch { settings.setCustomTextColor(value) }
    fun applyCustomTheme(value: CustomReaderTheme) = scope.launch { settings.setCustomTheme(value) }
    fun addCustomTheme(value: CustomReaderTheme) = scope.launch { settings.addCustomTheme(value) }
    fun deleteCustomTheme(value: CustomReaderTheme) = scope.launch { settings.deleteCustomTheme(value) }
    fun renameCustomTheme(value: CustomReaderTheme, newName: String) = scope.launch { settings.renameCustomTheme(value, newName) }
    fun updateVerticalWriting(value: Boolean) = scope.launch { settings.setVerticalWriting(value) }
    fun updateJustifyText(value: Boolean) = scope.launch { settings.setJustifyText(value) }
    fun updateAvoidPageBreak(value: Boolean) = scope.launch { settings.setAvoidPageBreak(value) }
    fun updateHideFurigana(value: Boolean) = scope.launch { settings.setReaderHideFurigana(value) }
    fun updateCharacterSpacing(value: Double) = scope.launch { settings.setCharacterSpacing(value) }
    fun updateParagraphSpacing(value: Double) = scope.launch { settings.setParagraphSpacing(value) }
    fun updateLayoutAdvanced(value: Boolean) = scope.launch { settings.setLayoutAdvanced(value) }
    fun updateTapZonePercent(value: Int) = scope.launch { settings.setChapterTapZones(value) }
    fun updateKeepScreenOn(value: Boolean) = scope.launch { settings.setKeepScreenOn(value) }
    fun updateSystemLightSepia(value: Boolean) = scope.launch { settings.setSystemLightSepia(value) }

    fun getReaderSettings(context: Context): ReaderSettings {
        val fontUrl = if (FontManager.isCustomFont(context, selectedFont)) {
            FontManager.getFontUri(context, selectedFont)
        } else {
            null
        }

        return NovelReaderSettingsPolicy.buildReaderSettings(
            snapshot = NovelReaderSettingsSnapshot(
                theme = theme,
                fontSize = fontSize,
                lineHeight = lineHeight,
                characterSpacing = characterSpacing,
                paragraphSpacing = paragraphSpacing,
                horizontalPadding = horizontalPadding,
                verticalPadding = verticalPadding,
                selectedFont = selectedFont,
                customBackgroundColor = customBackgroundColor,
                customTextColor = customTextColor,
                verticalWriting = verticalWriting,
                justifyText = justifyText,
                avoidPageBreak = avoidPageBreak,
                hideFurigana = hideFurigana,
                layoutAdvanced = layoutAdvanced,
                tapZonePercent = tapZonePercent,
                continuousMode = continuousMode,
                systemLightSepia = systemLightSepia,
            ),
            systemDark = context.isReaderSystemDark(),
            fontUrl = fontUrl,
        )
    }

    fun getCurrentChapter(): File? {
        return getCurrentChapterAbsolutePath()?.let(::File)
    }

    private fun getCurrentChapterAbsolutePath(): String? {
        return document.chapterAbsolutePath(index.toUInt())
    }

    fun getCurrentChapterTitle(): String? {
        return getChapterTitle(index)
    }

    fun getChapterTitle(chapterIndex: Int): String? {
        return NovelReaderNavigationPolicy.chapterTitle(
            chapterHref = document.getChapterHref(chapterIndex),
            tableOfContents = domainTableOfContents,
        )
    }

    fun getFlattenedToc(): List<NovelReaderTocItem> {
        return NovelReaderNavigationPolicy.flattenToc(domainTableOfContents)
    }

    fun getSpineIndexForHref(href: String): Int? {
        return NovelReaderNavigationPolicy.spineIndexForHref(
            href = href,
            chapterHrefs = document.linearSpineItems.indices.map { document.getChapterHref(it) },
        )
    }

    fun saveBookmark(progress: Double, updateTracker: Boolean = true, force: Boolean = false) {
        currentProgress = progress
        bridge.updateProgress(progress)
        persistBookmark(progress, force)
        if (
            NovelReaderSessionPolicy.shouldUpdateStatistics(
                updateTracker = updateTracker,
                trackingLocked = trackingLocked,
                appBackgrounded = appBackgrounded,
            )
        ) {
            statisticsTracker.update(totalExploredCharCount)
        }
        persistToDisk()
    }

    fun nextChapter(): Boolean {
        return when (
            val action = NovelReaderSessionPolicy.nextChapterAction(
                currentIndex = index,
                chapterCount = chapterCount,
            )
        ) {
            NovelReaderSessionPolicy.ChapterNavigationAction.Ignore -> false
            is NovelReaderSessionPolicy.ChapterNavigationAction.LoadChapter -> {
                loadChapter(action.index, action.progress)
                true
            }
        }
    }

    fun previousChapter(): Boolean {
        return when (
            val action = NovelReaderSessionPolicy.previousChapterAction(
                currentIndex = index,
            )
        ) {
            NovelReaderSessionPolicy.ChapterNavigationAction.Ignore -> false
            is NovelReaderSessionPolicy.ChapterNavigationAction.LoadChapter -> {
                loadChapter(action.index, action.progress)
                true
            }
        }
    }

    fun jumpToChapter(spineIndex: Int, fragment: String? = null) {
        loadChapter(spineIndex, 0.0)
        NovelReaderWebCommandPolicy.jumpToFragmentCommand(fragment)?.let(bridge::send)
    }

    /**
     * Resolves a raw `file://` URL (possibly carrying a #fragment) that came from an in-page
     * link click inside the WebView, finds the matching spine index, saves the current progress
     * bookmark, and then delegates to [jumpToChapter].
     *
     * If the URL cannot be matched to any spine item (e.g. external http link) the call is
     * silently ignored – the WebView already blocked the navigation via shouldOverrideUrlLoading.
     */
    fun jumpToUrl(url: String) {
        val target = NovelReaderNavigationPolicy.resolveInternalFileLink(
            url = url,
            chapterPaths = document.linearSpineItems.indices.map { document.chapterAbsolutePath(it.toUInt()) },
        )
        when (val action = NovelReaderSessionPolicy.internalLinkAction(target)) {
            NovelReaderSessionPolicy.InternalLinkAction.Ignore -> {
                android.util.Log.w("ReaderViewModel", "jumpToUrl: no spine match for $url")
            }
            is NovelReaderSessionPolicy.InternalLinkAction.JumpToChapter -> {
                if (action.saveCurrentBookmark) {
                    saveBookmark(currentProgress)
                }
                jumpToChapter(action.spineIndex, action.fragment)
            }
        }
    }

    private fun loadChapter(newIndex: Int, progress: Double) {
        // Flush any accumulated session delta to persistent statistics BEFORE we
        // change the index. persistBookmark() calls calculateExploredCharCount()
        // which uses the current index — if we change it first the delta is lost.
        persistBookmark(currentProgress)
        val change = NovelReaderSessionPolicy.chapterChangeState(
            newIndex = newIndex,
            progress = progress,
            baselineCharacterCount = calculateExploredCharCount(
                chapterIndex = newIndex,
                progress = progress,
            ),
            trackingLocked = trackingLocked,
            appBackgrounded = appBackgrounded,
        )
        if (change.updateStatisticsBeforeChange) {
            statisticsTracker.update(totalExploredCharCount)
        }

        index = change.index
        // Reset tracker baseline to the new position so neither the timer loop
        // nor the saveBookmark call below register a false delta from the jump.
        statisticsTracker.resetBaseline(change.baselineCharacterCount)
        saveBookmark(change.progress, updateTracker = false, force = true)
        loadCurrentChapterInBridge(change.progress)
    }

    private fun loadCurrentChapterInBridge(progress: Double) {
        val display = NovelReaderSessionPolicy.chapterDisplayState(
            chapterAbsolutePath = getCurrentChapterAbsolutePath(),
            progress = progress,
            chapterTitle = getCurrentChapterTitle(),
        ) ?: return

        bridge.loadChapter(display.fileUrl, display.progress, display.chapterTitle)
    }

    private fun calculateExploredCharCount(progress: Double): Int {
        return calculateExploredCharCount(
            chapterIndex = index,
            progress = progress,
        )
    }

    private fun calculateExploredCharCount(
        chapterIndex: Int,
        progress: Double,
    ): Int {
        return NovelReaderProgressPolicy.exploredCharacterCount(
            chapterIndex = chapterIndex,
            progress = progress,
            chapterCharacterCount = document::getChapterCharacters,
        )
    }

    private fun persistBookmark(progress: Double, force: Boolean = false) {
        val characterCount = calculateExploredCharCount(progress)
        totalExploredCharCount = characterCount

        val changed = NovelReaderProgressPolicy.shouldPersistBookmark(
            force = force,
            chapterIndex = index,
            progress = progress,
            characterCount = characterCount,
            lastChapterIndex = lastSavedChapterIndex,
            lastProgress = lastSavedProgress,
            lastCharacterCount = lastSavedCharacterCount,
            progressEpsilon = NovelReaderSessionPolicy.BOOKMARK_PROGRESS_EPSILON,
        )

        if (!changed) return

        BookStorage.save(
            Bookmark(
                chapterIndex = index,
                progress = progress,
                characterCount = characterCount,
                lastModified = nowMillis(),
            ),
            rootUrl,
            FileNames.bookmark,
        )
        lastSavedChapterIndex = index
        lastSavedProgress = progress
        lastSavedCharacterCount = characterCount
    }

    fun setTrackingLocked(locked: Boolean) {
        val transition = NovelReaderSessionPolicy.trackingLockTransition(
            locked = locked,
            currentlyTracking = statisticsTracker.state.isTracking,
        )
        if (transition.updateBeforeLock) {
            statisticsTracker.update(totalExploredCharCount)
        }
        if (transition.resetBaselineAfterUnlock) {
            statisticsTracker.resetBaseline(totalExploredCharCount)
        }
        trackingLocked = transition.locked
    }

    fun togglePause() {
        statisticsTracker.togglePause(totalExploredCharCount)
    }

    fun onAppBackgrounded() {
        applyAppVisibilityTransition(backgrounded = true)
    }

    fun onAppForegrounded() {
        applyAppVisibilityTransition(backgrounded = false)
    }

    private fun applyAppVisibilityTransition(backgrounded: Boolean) {
        val transition = NovelReaderSessionPolicy.appVisibilityTransition(backgrounded)
        if (transition.resetStatisticsBaseline) {
            statisticsTracker.resetBaseline(totalExploredCharCount)
        }
        appBackgrounded = transition.appBackgrounded
    }

    private fun persistToDisk() {
        val stats = statisticsTracker.statisticsForPersistence()
        BookStorage.saveStatistics(stats, rootUrl)
    }

}

private fun List<TocEntry>.toDomainTocEntries(): List<NovelReaderTocEntry> {
    return map { entry ->
        NovelReaderTocEntry(
            label = entry.label,
            href = entry.href,
            children = entry.children.toDomainTocEntries(),
        )
    }
}

private fun currentReaderDateKey(): String {
    val today = LocalDate.now()
    return NovelReaderStatisticsPolicy.dateKey(
        year = today.year,
        monthNumber = today.monthValue,
        dayOfMonth = today.dayOfMonth,
    )
}
