package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.model.StatisticsAutostartMode

object NovelReaderSettingsDefaults {
    const val maxCustomThemes = 24

    val theme = NovelReaderTheme.SYSTEM
    const val systemLightSepia = false
    val uiTheme = NovelReaderTheme.SYSTEM
    val customBackgroundColor = 0xFFF2E2C9.toInt()
    val customTextColor = 0xFF000000.toInt()
    val customInfoColor = 0xFF444444.toInt()
    const val verticalWriting = true
    const val selectedFont = "System"
    const val fontSize = 18.0
    const val readerHideFurigana = false
    const val continuousMode = false
    const val chapterSwipeDistance = 96
    const val chapterTapZones = 20
    const val horizontalPadding = 10.0
    const val verticalPadding = 10.0
    const val avoidPageBreak = true
    const val justifyText = false
    const val layoutAdvanced = false
    const val lineHeight = 1.6
    const val characterSpacing = 0.0
    const val paragraphSpacing = 0.0
    const val readerShowTitle = true
    const val readerShowCharacters = true
    const val readerShowPercentage = true
    const val readerShowProgressTop = true
    const val enableStatistics = true
    val statisticsAutostartMode = StatisticsAutostartMode.ON
    const val readerShowReadingSpeed = true
    const val readerShowReadingTime = true
    const val popupWidth = 300
    const val popupHeight = 200
    const val popupFullWidth = false
    const val popupSwipeToDismiss = true
    const val popupSwipeThreshold = 50
    const val maxResults = 10
    const val scanLength = 50
    const val keepScreenOn = false
}
