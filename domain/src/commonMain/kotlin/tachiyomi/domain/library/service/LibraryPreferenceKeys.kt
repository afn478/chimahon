package tachiyomi.domain.library.service

object LibraryPreferenceKeys {
    const val DISPLAY_MODE = "pref_display_mode_library"
    const val SORTING_MODE = "library_sorting_mode"
    const val RANDOM_SORT_SEED = "library_random_sort_seed"
    const val PORTRAIT_COLUMNS = "pref_library_columns_portrait_key"
    const val LANDSCAPE_COLUMNS = "pref_library_columns_landscape_key"
    const val NOVEL_PORTRAIT_COLUMNS = "pref_novel_library_columns_portrait_key"
    const val NOVEL_LANDSCAPE_COLUMNS = "pref_novel_library_columns_landscape_key"
    const val LAST_UPDATED_TIMESTAMP = "library_update_last_timestamp"
    const val AUTO_UPDATE_INTERVAL = "pref_library_update_interval_key"
    const val SHOW_UPDATING_PROGRESS_BANNER = "pref_show_updating_progress_banner_key"
    const val COVER_RATIOS = "pref_library_cover_ratios_key"
    const val COVER_COLORS = "pref_library_cover_colors_key"
    const val AUTO_UPDATE_DEVICE_RESTRICTIONS = "library_update_restriction"
    const val AUTO_UPDATE_MANGA_RESTRICTIONS = "library_update_manga_restriction"
    const val AUTO_UPDATE_METADATA = "auto_update_metadata"
    const val FETCH_METADATA_ON_ADD = "fetch_metadata_on_add"
    const val FETCH_CHAPTERS_ON_ADD = "fetch_chapters_on_add"
    const val SHOW_CONTINUE_READING_BUTTON = "display_continue_reading_button"
    const val MARK_DUPLICATE_READ_CHAPTER_AS_READ = "mark_duplicate_read_chapter_read"
    const val DEFAULT_CATEGORY = "default_category"
    const val NOVEL_DEFAULT_CATEGORY = "novel_default_category"
    const val LAST_USED_CATEGORY = "last_used_category"
    const val LIBRARY_UPDATE_CATEGORIES = "library_update_categories"
    const val LIBRARY_UPDATE_CATEGORIES_EXCLUDE = "library_update_categories_exclude"
    const val FILTER_DOWNLOADED = "pref_filter_library_downloaded_v2"
    const val FILTER_UNREAD = "pref_filter_library_unread_v2"
    const val FILTER_STARTED = "pref_filter_library_started_v2"
    const val FILTER_BOOKMARKED = "pref_filter_library_bookmarked_v2"
    const val FILTER_COMPLETED = "pref_filter_library_completed_v2"
    const val FILTER_INTERVAL_CUSTOM = "pref_filter_library_interval_custom"
    const val FILTER_LEWD = "pref_filter_library_lewd_v2"
    const val FILTER_CATEGORIES = "pref_filter_library_categories"
    const val FILTER_LIBRARY_CATEGORIES_INCLUDE = "pref_filter_library_categories_include"
    const val FILTER_LIBRARY_CATEGORIES_EXCLUDE = "pref_filter_library_categories_exclude"
    const val DOWNLOAD_BADGE = "display_download_badge"
    const val UNREAD_BADGE = "display_unread_badge"
    const val LOCAL_BADGE = "display_local_badge"
    const val LANGUAGE_BADGE = "display_language_badge"
    const val SOURCE_BADGE = "display_source_badge"
    const val USE_LANGUAGE_ICON = "display_language_text"
    const val SHOW_UPDATES_COUNT = "library_show_updates_count"
    const val NEW_UPDATES_COUNT = "library_unseen_updates_count"
    const val CATEGORY_TABS = "display_category_tabs"
    const val CATEGORY_NUMBER_OF_ITEMS = "display_number_of_items"
    const val CATEGORIZED_DISPLAY_SETTINGS = "categorized_display"
    const val SHOW_HIDDEN_CATEGORIES = "hide_hidden_categories"
    const val DEFAULT_CHAPTER_FILTER_BY_READ = "default_chapter_filter_by_read"
    const val DEFAULT_CHAPTER_FILTER_BY_DOWNLOADED = "default_chapter_filter_by_downloaded"
    const val DEFAULT_CHAPTER_FILTER_BY_BOOKMARKED = "default_chapter_filter_by_bookmarked"
    const val DEFAULT_CHAPTER_SORT_BY_SOURCE_OR_NUMBER = "default_chapter_sort_by_source_or_number"
    const val DEFAULT_CHAPTER_DISPLAY_BY_NAME_OR_NUMBER = "default_chapter_display_by_name_or_number"
    const val DEFAULT_CHAPTER_SORT_BY_ASCENDING_OR_DESCENDING = "default_chapter_sort_by_ascending_or_descending"
    const val AUTO_CLEAR_CHAPTER_CACHE = "auto_clear_chapter_cache"
    const val HIDE_MISSING_CHAPTERS = "pref_hide_missing_chapter_indicators"
    const val SHOW_EMPTY_CATEGORIES_SEARCH = "show_empty_categories_search"
    const val CHAPTER_SWIPE_START_ACTION = "pref_chapter_swipe_start_action"
    const val CHAPTER_SWIPE_END_ACTION = "pref_chapter_swipe_end_action"
    const val UPDATE_MANGA_TITLES = "pref_update_library_manga_titles"
    const val DISALLOW_NON_ASCII_FILENAMES = "disallow_non_ascii_filenames"
    const val SORT_TAGS_FOR_LIBRARY = "sort_tags_for_library"
    const val GROUP_LIBRARY_UPDATE_TYPE = "group_library_update_type"
    const val GROUP_LIBRARY_BY = "group_library_by"

    fun filterTracking(id: Int): String {
        return "pref_filter_library_tracked_${id}_v2"
    }

    val categoryPreferenceKeys = setOf(
        DEFAULT_CATEGORY,
        LIBRARY_UPDATE_CATEGORIES,
        LIBRARY_UPDATE_CATEGORIES_EXCLUDE,
        FILTER_LIBRARY_CATEGORIES_INCLUDE,
        FILTER_LIBRARY_CATEGORIES_EXCLUDE,
    )
}
