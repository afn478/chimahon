package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions
import tachiyomi.domain.manga.model.MangaChapterFlags
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryPreferenceKeysTest {
    @Test
    fun displayPreferenceKeysRemainStable() {
        assertEquals("pref_display_mode_library", LibraryPreferenceKeys.DISPLAY_MODE)
        assertEquals("library_sorting_mode", LibraryPreferenceKeys.SORTING_MODE)
        assertEquals("library_random_sort_seed", LibraryPreferenceKeys.RANDOM_SORT_SEED)
        assertEquals(
            "pref_library_columns_portrait_key",
            LibraryPreferenceKeys.PORTRAIT_COLUMNS,
        )
        assertEquals(
            "pref_library_columns_landscape_key",
            LibraryPreferenceKeys.LANDSCAPE_COLUMNS,
        )
        assertEquals(
            "pref_novel_library_columns_portrait_key",
            LibraryPreferenceKeys.NOVEL_PORTRAIT_COLUMNS,
        )
        assertEquals(
            "pref_novel_library_columns_landscape_key",
            LibraryPreferenceKeys.NOVEL_LANDSCAPE_COLUMNS,
        )
    }

    @Test
    fun displayPreferenceDefaultsRemainStable() {
        assertEquals(0, LibraryPreferenceDefaults.RANDOM_SORT_SEED)
        assertEquals(0, LibraryPreferenceDefaults.PORTRAIT_COLUMNS)
        assertEquals(0, LibraryPreferenceDefaults.LANDSCAPE_COLUMNS)
        assertEquals(2, LibraryPreferenceDefaults.NOVEL_PORTRAIT_COLUMNS)
        assertEquals(2, LibraryPreferenceDefaults.NOVEL_LANDSCAPE_COLUMNS)
    }

    @Test
    fun updatePreferenceKeysRemainStable() {
        assertEquals("library_update_last_timestamp", LibraryPreferenceKeys.LAST_UPDATED_TIMESTAMP)
        assertEquals("pref_library_update_interval_key", LibraryPreferenceKeys.AUTO_UPDATE_INTERVAL)
        assertEquals(
            "pref_show_updating_progress_banner_key",
            LibraryPreferenceKeys.SHOW_UPDATING_PROGRESS_BANNER,
        )
        assertEquals("pref_library_cover_ratios_key", LibraryPreferenceKeys.COVER_RATIOS)
        assertEquals("pref_library_cover_colors_key", LibraryPreferenceKeys.COVER_COLORS)
        assertEquals(
            "library_update_restriction",
            LibraryPreferenceKeys.AUTO_UPDATE_DEVICE_RESTRICTIONS,
        )
        assertEquals(
            "library_update_manga_restriction",
            LibraryPreferenceKeys.AUTO_UPDATE_MANGA_RESTRICTIONS,
        )
        assertEquals("auto_update_metadata", LibraryPreferenceKeys.AUTO_UPDATE_METADATA)
        assertEquals("fetch_metadata_on_add", LibraryPreferenceKeys.FETCH_METADATA_ON_ADD)
        assertEquals("fetch_chapters_on_add", LibraryPreferenceKeys.FETCH_CHAPTERS_ON_ADD)
        assertEquals(
            "display_continue_reading_button",
            LibraryPreferenceKeys.SHOW_CONTINUE_READING_BUTTON,
        )
        assertEquals(
            "mark_duplicate_read_chapter_read",
            LibraryPreferenceKeys.MARK_DUPLICATE_READ_CHAPTER_AS_READ,
        )
    }

    @Test
    fun updatePreferenceDefaultsRemainStable() {
        assertEquals(0L, LibraryPreferenceDefaults.LAST_UPDATED_TIMESTAMP)
        assertEquals(0, LibraryPreferenceDefaults.AUTO_UPDATE_INTERVAL)
        assertEquals(true, LibraryPreferenceDefaults.SHOW_UPDATING_PROGRESS_BANNER)
        assertEquals(emptySet(), LibraryPreferenceDefaults.COVER_RATIOS)
        assertEquals(emptySet(), LibraryPreferenceDefaults.COVER_COLORS)
        assertEquals(
            setOf(LibraryUpdateDeviceRestrictions.DEVICE_ONLY_ON_WIFI),
            LibraryPreferenceDefaults.AUTO_UPDATE_DEVICE_RESTRICTIONS,
        )
        assertEquals(
            LibraryUpdateMangaRestrictions.default,
            LibraryPreferenceDefaults.AUTO_UPDATE_MANGA_RESTRICTIONS,
        )
        assertEquals(false, LibraryPreferenceDefaults.AUTO_UPDATE_METADATA)
        assertEquals(false, LibraryPreferenceDefaults.FETCH_METADATA_ON_ADD)
        assertEquals(false, LibraryPreferenceDefaults.FETCH_CHAPTERS_ON_ADD)
        assertEquals(false, LibraryPreferenceDefaults.SHOW_CONTINUE_READING_BUTTON)
        assertEquals(emptySet(), LibraryPreferenceDefaults.MARK_DUPLICATE_READ_CHAPTER_AS_READ)
    }

    @Test
    fun updateDeviceRestrictionValuesRemainStable() {
        assertEquals("wifi", LibraryUpdateDeviceRestrictions.DEVICE_ONLY_ON_WIFI)
        assertEquals("network_not_metered", LibraryUpdateDeviceRestrictions.DEVICE_NETWORK_NOT_METERED)
        assertEquals("ac", LibraryUpdateDeviceRestrictions.DEVICE_CHARGING)
    }

    @Test
    fun duplicateChapterReadPreferenceValuesRemainStable() {
        assertEquals("new", LibraryDuplicateChapterReadPreference.NEW)
        assertEquals("existing", LibraryDuplicateChapterReadPreference.EXISTING)
    }

    @Test
    fun badgePreferenceKeysRemainStable() {
        assertEquals("display_download_badge", LibraryPreferenceKeys.DOWNLOAD_BADGE)
        assertEquals("display_unread_badge", LibraryPreferenceKeys.UNREAD_BADGE)
        assertEquals("display_local_badge", LibraryPreferenceKeys.LOCAL_BADGE)
        assertEquals("display_language_badge", LibraryPreferenceKeys.LANGUAGE_BADGE)
        assertEquals("display_source_badge", LibraryPreferenceKeys.SOURCE_BADGE)
        assertEquals("display_language_text", LibraryPreferenceKeys.USE_LANGUAGE_ICON)
        assertEquals(
            "library_show_updates_count",
            LibraryPreferenceKeys.SHOW_UPDATES_COUNT,
        )
        assertEquals("library_unseen_updates_count", LibraryPreferenceKeys.NEW_UPDATES_COUNT)
    }

    @Test
    fun badgePreferenceDefaultsRemainStable() {
        assertEquals(false, LibraryPreferenceDefaults.DOWNLOAD_BADGE)
        assertEquals(true, LibraryPreferenceDefaults.UNREAD_BADGE)
        assertEquals(true, LibraryPreferenceDefaults.LOCAL_BADGE)
        assertEquals(true, LibraryPreferenceDefaults.LANGUAGE_BADGE)
        assertEquals(true, LibraryPreferenceDefaults.SOURCE_BADGE)
        assertEquals(true, LibraryPreferenceDefaults.USE_LANGUAGE_ICON)
        assertEquals(true, LibraryPreferenceDefaults.SHOW_UPDATES_COUNT)
        assertEquals(0, LibraryPreferenceDefaults.NEW_UPDATES_COUNT)
    }

    @Test
    fun categoryDisplayPreferenceKeysRemainStable() {
        assertEquals("display_category_tabs", LibraryPreferenceKeys.CATEGORY_TABS)
        assertEquals(
            "display_number_of_items",
            LibraryPreferenceKeys.CATEGORY_NUMBER_OF_ITEMS,
        )
        assertEquals(
            "categorized_display",
            LibraryPreferenceKeys.CATEGORIZED_DISPLAY_SETTINGS,
        )
        assertEquals("hide_hidden_categories", LibraryPreferenceKeys.SHOW_HIDDEN_CATEGORIES)
    }

    @Test
    fun categoryDisplayPreferenceDefaultsRemainStable() {
        assertEquals(true, LibraryPreferenceDefaults.CATEGORY_TABS)
        assertEquals(false, LibraryPreferenceDefaults.CATEGORY_NUMBER_OF_ITEMS)
        assertEquals(false, LibraryPreferenceDefaults.CATEGORIZED_DISPLAY_SETTINGS)
        assertEquals(false, LibraryPreferenceDefaults.SHOW_HIDDEN_CATEGORIES)
    }

    @Test
    fun chapterPreferenceKeysRemainStable() {
        assertEquals(
            "default_chapter_filter_by_read",
            LibraryPreferenceKeys.DEFAULT_CHAPTER_FILTER_BY_READ,
        )
        assertEquals(
            "default_chapter_filter_by_downloaded",
            LibraryPreferenceKeys.DEFAULT_CHAPTER_FILTER_BY_DOWNLOADED,
        )
        assertEquals(
            "default_chapter_filter_by_bookmarked",
            LibraryPreferenceKeys.DEFAULT_CHAPTER_FILTER_BY_BOOKMARKED,
        )
        assertEquals(
            "default_chapter_sort_by_source_or_number",
            LibraryPreferenceKeys.DEFAULT_CHAPTER_SORT_BY_SOURCE_OR_NUMBER,
        )
        assertEquals(
            "default_chapter_display_by_name_or_number",
            LibraryPreferenceKeys.DEFAULT_CHAPTER_DISPLAY_BY_NAME_OR_NUMBER,
        )
        assertEquals(
            "default_chapter_sort_by_ascending_or_descending",
            LibraryPreferenceKeys.DEFAULT_CHAPTER_SORT_BY_ASCENDING_OR_DESCENDING,
        )
        assertEquals("auto_clear_chapter_cache", LibraryPreferenceKeys.AUTO_CLEAR_CHAPTER_CACHE)
        assertEquals(
            "pref_hide_missing_chapter_indicators",
            LibraryPreferenceKeys.HIDE_MISSING_CHAPTERS,
        )
        assertEquals("show_empty_categories_search", LibraryPreferenceKeys.SHOW_EMPTY_CATEGORIES_SEARCH)
    }

    @Test
    fun chapterPreferenceDefaultsRemainStable() {
        assertEquals(MangaChapterFlags.SHOW_ALL, LibraryPreferenceDefaults.DEFAULT_CHAPTER_FILTER_BY_READ)
        assertEquals(MangaChapterFlags.SHOW_ALL, LibraryPreferenceDefaults.DEFAULT_CHAPTER_FILTER_BY_DOWNLOADED)
        assertEquals(MangaChapterFlags.SHOW_ALL, LibraryPreferenceDefaults.DEFAULT_CHAPTER_FILTER_BY_BOOKMARKED)
        assertEquals(
            MangaChapterFlags.CHAPTER_SORTING_SOURCE,
            LibraryPreferenceDefaults.DEFAULT_CHAPTER_SORT_BY_SOURCE_OR_NUMBER,
        )
        assertEquals(
            MangaChapterFlags.CHAPTER_DISPLAY_NAME,
            LibraryPreferenceDefaults.DEFAULT_CHAPTER_DISPLAY_BY_NAME_OR_NUMBER,
        )
        assertEquals(
            MangaChapterFlags.CHAPTER_SORT_DESC,
            LibraryPreferenceDefaults.DEFAULT_CHAPTER_SORT_BY_ASCENDING_OR_DESCENDING,
        )
        assertEquals(false, LibraryPreferenceDefaults.AUTO_CLEAR_CHAPTER_CACHE)
        assertEquals(false, LibraryPreferenceDefaults.HIDE_MISSING_CHAPTERS)
        assertEquals(false, LibraryPreferenceDefaults.SHOW_EMPTY_CATEGORIES_SEARCH)
    }

    @Test
    fun swipeAndLibraryMiscPreferenceKeysRemainStable() {
        assertEquals("pref_chapter_swipe_start_action", LibraryPreferenceKeys.CHAPTER_SWIPE_START_ACTION)
        assertEquals("pref_chapter_swipe_end_action", LibraryPreferenceKeys.CHAPTER_SWIPE_END_ACTION)
        assertEquals("pref_update_library_manga_titles", LibraryPreferenceKeys.UPDATE_MANGA_TITLES)
        assertEquals("disallow_non_ascii_filenames", LibraryPreferenceKeys.DISALLOW_NON_ASCII_FILENAMES)
        assertEquals("sort_tags_for_library", LibraryPreferenceKeys.SORT_TAGS_FOR_LIBRARY)
        assertEquals("group_library_update_type", LibraryPreferenceKeys.GROUP_LIBRARY_UPDATE_TYPE)
        assertEquals("group_library_by", LibraryPreferenceKeys.GROUP_LIBRARY_BY)
    }

    @Test
    fun libraryMiscPreferenceDefaultsRemainStable() {
        assertEquals(false, LibraryPreferenceDefaults.UPDATE_MANGA_TITLES)
        assertEquals(false, LibraryPreferenceDefaults.DISALLOW_NON_ASCII_FILENAMES)
        assertEquals(emptySet(), LibraryPreferenceDefaults.SORT_TAGS_FOR_LIBRARY)
        assertEquals(GroupLibraryMode.GLOBAL, LibraryPreferenceDefaults.GROUP_LIBRARY_UPDATE_TYPE)
        assertEquals(LibraryGroup.BY_DEFAULT, LibraryPreferenceDefaults.GROUP_LIBRARY_BY)
    }

    @Test
    fun filterPreferenceKeysRemainStable() {
        assertEquals(
            "pref_filter_library_downloaded_v2",
            LibraryPreferenceKeys.FILTER_DOWNLOADED,
        )
        assertEquals(
            "pref_filter_library_unread_v2",
            LibraryPreferenceKeys.FILTER_UNREAD,
        )
        assertEquals(
            "pref_filter_library_started_v2",
            LibraryPreferenceKeys.FILTER_STARTED,
        )
        assertEquals(
            "pref_filter_library_bookmarked_v2",
            LibraryPreferenceKeys.FILTER_BOOKMARKED,
        )
        assertEquals(
            "pref_filter_library_completed_v2",
            LibraryPreferenceKeys.FILTER_COMPLETED,
        )
        assertEquals(
            "pref_filter_library_interval_custom",
            LibraryPreferenceKeys.FILTER_INTERVAL_CUSTOM,
        )
        assertEquals(
            "pref_filter_library_lewd_v2",
            LibraryPreferenceKeys.FILTER_LEWD,
        )
        assertEquals(
            "pref_filter_library_categories",
            LibraryPreferenceKeys.FILTER_CATEGORIES,
        )
        assertEquals(
            "pref_filter_library_tracked_7_v2",
            LibraryPreferenceKeys.filterTracking(7),
        )
    }

    @Test
    fun filterPreferenceDefaultsRemainStable() {
        assertEquals(false, LibraryPreferenceDefaults.FILTER_CATEGORIES)
    }

    @Test
    fun categoryPreferenceKeysRemainStableForBackupRestore() {
        assertEquals("default_category", LibraryPreferenceKeys.DEFAULT_CATEGORY)
        assertEquals("novel_default_category", LibraryPreferenceKeys.NOVEL_DEFAULT_CATEGORY)
        assertEquals("last_used_category", LibraryPreferenceKeys.LAST_USED_CATEGORY)
        assertEquals("library_update_categories", LibraryPreferenceKeys.LIBRARY_UPDATE_CATEGORIES)
        assertEquals(
            "library_update_categories_exclude",
            LibraryPreferenceKeys.LIBRARY_UPDATE_CATEGORIES_EXCLUDE,
        )
        assertEquals(
            "pref_filter_library_categories_include",
            LibraryPreferenceKeys.FILTER_LIBRARY_CATEGORIES_INCLUDE,
        )
        assertEquals(
            "pref_filter_library_categories_exclude",
            LibraryPreferenceKeys.FILTER_LIBRARY_CATEGORIES_EXCLUDE,
        )
        assertEquals(
            setOf(
                LibraryPreferenceKeys.DEFAULT_CATEGORY,
                LibraryPreferenceKeys.LIBRARY_UPDATE_CATEGORIES,
                LibraryPreferenceKeys.LIBRARY_UPDATE_CATEGORIES_EXCLUDE,
                LibraryPreferenceKeys.FILTER_LIBRARY_CATEGORIES_INCLUDE,
                LibraryPreferenceKeys.FILTER_LIBRARY_CATEGORIES_EXCLUDE,
            ),
            LibraryPreferenceKeys.categoryPreferenceKeys,
        )
    }

    @Test
    fun categoryPreferenceDefaultsRemainStableForAddAndLibraryState() {
        assertEquals(-1, LibraryPreferenceDefaults.DEFAULT_CATEGORY)
        assertEquals("", LibraryPreferenceDefaults.NOVEL_DEFAULT_CATEGORY)
        assertEquals(0, LibraryPreferenceDefaults.LAST_USED_CATEGORY)
    }
}
