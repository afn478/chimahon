package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.model.StatisticsAutostartMode

class NovelReaderPreferenceKeysTest {
    @Test
    fun readerThemeValuesRemainStable() {
        assertEquals(
            listOf("SYSTEM", "LIGHT", "DARK", "SEPIA", "CUSTOM", "PURE_BLACK"),
            NovelReaderTheme.entries.map { it.name },
        )
    }

    @Test
    fun statisticsAutostartModeValuesRemainStable() {
        assertEquals(
            listOf("OFF", "ON", "PAGETURN"),
            StatisticsAutostartMode.entries.map { it.name },
        )
    }

    @Test
    fun preferenceKeysRemainStable() {
        assertEquals(
            listOf(
                "theme",
                "system_light_sepia",
                "ui_theme",
                "custom_background_color",
                "custom_text_color",
                "custom_themes",
                "custom_info_color",
                "vertical_writing",
                "selected_font",
                "font_size",
                "reader_hide_furigana",
                "continuous_mode",
                "chapter_swipe_distance",
                "chapter_tap_zones",
                "horizontal_padding",
                "vertical_padding",
                "avoid_page_break",
                "justify_text",
                "layout_advanced",
                "line_height",
                "character_spacing",
                "paragraph_spacing",
                "reader_show_title",
                "reader_show_characters",
                "reader_show_percentage",
                "reader_show_progress_top",
                "enable_statistics",
                "statistics_autostart_mode",
                "reader_show_reading_speed",
                "reader_show_reading_time",
                "popup_width",
                "popup_height",
                "popup_full_width",
                "popup_swipe_to_dismiss",
                "popup_swipe_threshold",
                "max_results",
                "scan_length",
                "keep_screen_on",
            ),
            NovelReaderPreferenceKeys.allKeys,
        )
    }

    @Test
    fun preferenceDefaultsRemainStable() {
        assertEquals(24, NovelReaderSettingsDefaults.maxCustomThemes)
        assertEquals(NovelReaderTheme.SYSTEM, NovelReaderSettingsDefaults.theme)
        assertEquals(false, NovelReaderSettingsDefaults.systemLightSepia)
        assertEquals(NovelReaderTheme.SYSTEM, NovelReaderSettingsDefaults.uiTheme)
        assertEquals(0xFFF2E2C9.toInt(), NovelReaderSettingsDefaults.customBackgroundColor)
        assertEquals(0xFF000000.toInt(), NovelReaderSettingsDefaults.customTextColor)
        assertEquals(0xFF444444.toInt(), NovelReaderSettingsDefaults.customInfoColor)
        assertEquals(true, NovelReaderSettingsDefaults.verticalWriting)
        assertEquals("System", NovelReaderSettingsDefaults.selectedFont)
        assertEquals(18.0, NovelReaderSettingsDefaults.fontSize)
        assertEquals(false, NovelReaderSettingsDefaults.readerHideFurigana)
        assertEquals(false, NovelReaderSettingsDefaults.continuousMode)
        assertEquals(96, NovelReaderSettingsDefaults.chapterSwipeDistance)
        assertEquals(20, NovelReaderSettingsDefaults.chapterTapZones)
        assertEquals(10.0, NovelReaderSettingsDefaults.horizontalPadding)
        assertEquals(10.0, NovelReaderSettingsDefaults.verticalPadding)
        assertEquals(true, NovelReaderSettingsDefaults.avoidPageBreak)
        assertEquals(false, NovelReaderSettingsDefaults.justifyText)
        assertEquals(false, NovelReaderSettingsDefaults.layoutAdvanced)
        assertEquals(1.6, NovelReaderSettingsDefaults.lineHeight)
        assertEquals(0.0, NovelReaderSettingsDefaults.characterSpacing)
        assertEquals(0.0, NovelReaderSettingsDefaults.paragraphSpacing)
        assertEquals(true, NovelReaderSettingsDefaults.readerShowTitle)
        assertEquals(true, NovelReaderSettingsDefaults.readerShowCharacters)
        assertEquals(true, NovelReaderSettingsDefaults.readerShowPercentage)
        assertEquals(true, NovelReaderSettingsDefaults.readerShowProgressTop)
        assertEquals(true, NovelReaderSettingsDefaults.enableStatistics)
        assertEquals(StatisticsAutostartMode.ON, NovelReaderSettingsDefaults.statisticsAutostartMode)
        assertEquals(true, NovelReaderSettingsDefaults.readerShowReadingSpeed)
        assertEquals(true, NovelReaderSettingsDefaults.readerShowReadingTime)
        assertEquals(300, NovelReaderSettingsDefaults.popupWidth)
        assertEquals(200, NovelReaderSettingsDefaults.popupHeight)
        assertEquals(false, NovelReaderSettingsDefaults.popupFullWidth)
        assertEquals(true, NovelReaderSettingsDefaults.popupSwipeToDismiss)
        assertEquals(50, NovelReaderSettingsDefaults.popupSwipeThreshold)
        assertEquals(10, NovelReaderSettingsDefaults.maxResults)
        assertEquals(50, NovelReaderSettingsDefaults.scanLength)
        assertEquals(false, NovelReaderSettingsDefaults.keepScreenOn)
    }
}
