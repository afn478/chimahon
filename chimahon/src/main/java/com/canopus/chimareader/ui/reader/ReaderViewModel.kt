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
import com.canopus.chimareader.data.epub.SpineItemType
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
import tachiyomi.domain.reader.service.NovelReaderFileUrlPolicy
import tachiyomi.domain.reader.service.NovelReaderNavigationPolicy
import tachiyomi.domain.reader.service.NovelReaderProgressPolicy
import tachiyomi.domain.reader.service.NovelReaderSettingsPolicy
import tachiyomi.domain.reader.service.NovelReaderStatisticsPolicy

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

    fun updateState(url: String, progress: Double, title: String? = null) {
        chapterUrl = url
        chapterTitle = title
        this.progress = progress
    }

    fun updateProgress(progress: Double) {
        this.progress = progress
    }

    fun highlightSasayakiCue(id: String, reveal: Boolean) {
        send(NovelReaderWebCommand.HighlightSasayakiCue(id, reveal))
    }

    fun clearSasayakiCue() {
        send(NovelReaderWebCommand.ClearSasayakiCue)
    }

    fun paginate(forward: Boolean) {
        send(NovelReaderWebCommand.Paginate(forward))
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
        val bookCopy = book.copy(lastAccess = nowMillis())
        BookStorage.save(bookCopy, root, FileNames.metadata)
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
            return spineItem.type == SpineItemType.IMAGE_ONLY
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
        index = bookmark?.chapterIndex ?: 0
        currentProgress = bookmark?.progress ?: 0.0
        totalExploredCharCount = calculateExploredCharCount(currentProgress)
        lastSavedChapterIndex = index
        lastSavedProgress = currentProgress
        lastSavedCharacterCount = totalExploredCharCount

        val stats = BookStorage.loadStatistics(rootUrl)
        if (stats != null) {
            fullStatistics.addAll(NovelReaderStatisticsPolicy.normalizeLoadedStatistics(stats))
        }

        statisticsTracker = ReaderStatisticsTracker(
            title = document.title ?: "Unknown",
            initialStatistics = fullStatistics,
            enabled = true,
            nowMillis = nowMillis,
            dateKeyProvider = ::currentReaderDateKey,
        )

        scope.launch {
            while (true) {
                delay(1000)
                if (!trackingLocked && !appBackgrounded) {
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

        getCurrentChapter()?.let { file ->
            val fileUrl = NovelReaderFileUrlPolicy.fileUrlForAbsolutePath(file.absolutePath)
            val chapterTitle = getCurrentChapterTitle()
            bridge.updateState(fileUrl, currentProgress, chapterTitle)
            bridge.send(NovelReaderWebCommand.LoadChapter(fileUrl, currentProgress))
        }

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
        val absolutePath = document.chapterAbsolutePath(index.toUInt()) ?: return null
        return File(absolutePath)
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
        if (updateTracker && !trackingLocked && !appBackgrounded) {
            statisticsTracker.update(totalExploredCharCount)
        }
        persistToDisk()
    }

    fun nextChapter(): Boolean {
        if (index >= chapterCount - 1) return false
        loadChapter(index + 1, 0.0)
        return true
    }

    fun previousChapter(): Boolean {
        if (index <= 0) return false
        loadChapter(index - 1, 1.0)
        return true
    }

    fun jumpToChapter(spineIndex: Int, fragment: String? = null) {
        loadChapter(spineIndex, 0.0)
        if (!fragment.isNullOrEmpty()) {
            bridge.send(NovelReaderWebCommand.JumpToFragment(fragment))
        }
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
        if (target != null) {
            saveBookmark(currentProgress)
            jumpToChapter(target.spineIndex, target.fragment)
            return
        }
        android.util.Log.w("ReaderViewModel", "jumpToUrl: no spine match for $url")
    }

    private fun loadChapter(newIndex: Int, progress: Double) {
        // Flush any accumulated session delta to persistent statistics BEFORE we
        // change the index. persistBookmark() calls calculateExploredCharCount()
        // which uses the current index — if we change it first the delta is lost.
        persistBookmark(currentProgress)
        if (!trackingLocked && !appBackgrounded) {
            statisticsTracker.update(totalExploredCharCount)
        }

        index = newIndex
        // Reset tracker baseline to the new position so neither the timer loop
        // nor the saveBookmark call below register a false delta from the jump.
        statisticsTracker.resetBaseline(calculateExploredCharCount(progress))
        saveBookmark(progress, updateTracker = false, force = true)
        getCurrentChapter()?.let { file ->
            val fileUrl = NovelReaderFileUrlPolicy.fileUrlForAbsolutePath(file.absolutePath)
            val chapterTitle = getCurrentChapterTitle()
            bridge.updateState(fileUrl, progress, chapterTitle)
            bridge.send(NovelReaderWebCommand.LoadChapter(fileUrl, progress))
        }
    }

    private fun calculateExploredCharCount(progress: Double): Int {
        return NovelReaderProgressPolicy.exploredCharacterCount(
            chapterIndex = index,
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
            progressEpsilon = BOOKMARK_PROGRESS_EPSILON,
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
        if (locked) {
            if (statisticsTracker.state.isTracking) {
                statisticsTracker.update(totalExploredCharCount)
            }
            trackingLocked = true
        } else {
            statisticsTracker.resetBaseline(totalExploredCharCount)
            trackingLocked = false
        }
    }

    fun togglePause() {
        statisticsTracker.togglePause(totalExploredCharCount)
    }

    fun onAppBackgrounded() {
        appBackgrounded = true
    }

    fun onAppForegrounded() {
        statisticsTracker.resetBaseline(totalExploredCharCount)
        appBackgrounded = false
    }

    private fun persistToDisk() {
        val stats = statisticsTracker.statisticsForPersistence()
        BookStorage.saveStatistics(stats, rootUrl)
    }

    companion object {
        private const val BOOKMARK_PROGRESS_EPSILON = 0.0001
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
