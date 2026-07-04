package tachiyomi.domain.reader.service

object NovelReaderPreferenceKeys {
    const val THEME = "theme"
    const val SYSTEM_LIGHT_SEPIA = "system_light_sepia"
    const val UI_THEME = "ui_theme"
    const val CUSTOM_BACKGROUND_COLOR = "custom_background_color"
    const val CUSTOM_TEXT_COLOR = "custom_text_color"
    const val CUSTOM_THEMES = "custom_themes"
    const val CUSTOM_INFO_COLOR = "custom_info_color"
    const val VERTICAL_WRITING = "vertical_writing"
    const val SELECTED_FONT = "selected_font"
    const val FONT_SIZE = "font_size"
    const val READER_HIDE_FURIGANA = "reader_hide_furigana"
    const val CONTINUOUS_MODE = "continuous_mode"
    const val CHAPTER_SWIPE_DISTANCE = "chapter_swipe_distance"
    const val CHAPTER_TAP_ZONES = "chapter_tap_zones"
    const val HORIZONTAL_PADDING = "horizontal_padding"
    const val VERTICAL_PADDING = "vertical_padding"
    const val AVOID_PAGE_BREAK = "avoid_page_break"
    const val JUSTIFY_TEXT = "justify_text"
    const val LAYOUT_ADVANCED = "layout_advanced"
    const val LINE_HEIGHT = "line_height"
    const val CHARACTER_SPACING = "character_spacing"
    const val PARAGRAPH_SPACING = "paragraph_spacing"
    const val READER_SHOW_TITLE = "reader_show_title"
    const val READER_SHOW_CHARACTERS = "reader_show_characters"
    const val READER_SHOW_PERCENTAGE = "reader_show_percentage"
    const val READER_SHOW_PROGRESS_TOP = "reader_show_progress_top"
    const val ENABLE_STATISTICS = "enable_statistics"
    const val STATISTICS_AUTOSTART_MODE = "statistics_autostart_mode"
    const val READER_SHOW_READING_SPEED = "reader_show_reading_speed"
    const val READER_SHOW_READING_TIME = "reader_show_reading_time"
    const val POPUP_WIDTH = "popup_width"
    const val POPUP_HEIGHT = "popup_height"
    const val POPUP_FULL_WIDTH = "popup_full_width"
    const val POPUP_SWIPE_TO_DISMISS = "popup_swipe_to_dismiss"
    const val POPUP_SWIPE_THRESHOLD = "popup_swipe_threshold"
    const val MAX_RESULTS = "max_results"
    const val SCAN_LENGTH = "scan_length"
    const val KEEP_SCREEN_ON = "keep_screen_on"

    val allKeys = listOf(
        THEME,
        SYSTEM_LIGHT_SEPIA,
        UI_THEME,
        CUSTOM_BACKGROUND_COLOR,
        CUSTOM_TEXT_COLOR,
        CUSTOM_THEMES,
        CUSTOM_INFO_COLOR,
        VERTICAL_WRITING,
        SELECTED_FONT,
        FONT_SIZE,
        READER_HIDE_FURIGANA,
        CONTINUOUS_MODE,
        CHAPTER_SWIPE_DISTANCE,
        CHAPTER_TAP_ZONES,
        HORIZONTAL_PADDING,
        VERTICAL_PADDING,
        AVOID_PAGE_BREAK,
        JUSTIFY_TEXT,
        LAYOUT_ADVANCED,
        LINE_HEIGHT,
        CHARACTER_SPACING,
        PARAGRAPH_SPACING,
        READER_SHOW_TITLE,
        READER_SHOW_CHARACTERS,
        READER_SHOW_PERCENTAGE,
        READER_SHOW_PROGRESS_TOP,
        ENABLE_STATISTICS,
        STATISTICS_AUTOSTART_MODE,
        READER_SHOW_READING_SPEED,
        READER_SHOW_READING_TIME,
        POPUP_WIDTH,
        POPUP_HEIGHT,
        POPUP_FULL_WIDTH,
        POPUP_SWIPE_TO_DISMISS,
        POPUP_SWIPE_THRESHOLD,
        MAX_RESULTS,
        SCAN_LENGTH,
        KEEP_SCREEN_ON,
    )
}
