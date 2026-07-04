package tachiyomi.domain.library.service

import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions
import tachiyomi.domain.manga.model.MangaChapterFlags

object LibraryPreferenceDefaults {
    const val RANDOM_SORT_SEED = 0
    const val PORTRAIT_COLUMNS = 0
    const val LANDSCAPE_COLUMNS = 0
    const val NOVEL_PORTRAIT_COLUMNS = 2
    const val NOVEL_LANDSCAPE_COLUMNS = 2
    const val LAST_UPDATED_TIMESTAMP = 0L
    const val AUTO_UPDATE_INTERVAL = 0
    const val SHOW_UPDATING_PROGRESS_BANNER = true
    val COVER_RATIOS: Set<String> = emptySet()
    val COVER_COLORS: Set<String> = emptySet()
    val AUTO_UPDATE_DEVICE_RESTRICTIONS = setOf(
        LibraryUpdateDeviceRestrictions.DEVICE_ONLY_ON_WIFI,
    )
    val AUTO_UPDATE_MANGA_RESTRICTIONS = LibraryUpdateMangaRestrictions.default
    const val AUTO_UPDATE_METADATA = false
    const val FETCH_METADATA_ON_ADD = false
    const val FETCH_CHAPTERS_ON_ADD = false
    const val SHOW_CONTINUE_READING_BUTTON = false
    val MARK_DUPLICATE_READ_CHAPTER_AS_READ: Set<String> = emptySet()
    const val DEFAULT_CATEGORY = -1
    const val NOVEL_DEFAULT_CATEGORY = ""
    const val LAST_USED_CATEGORY = 0
    const val FILTER_CATEGORIES = false
    const val DOWNLOAD_BADGE = false
    const val UNREAD_BADGE = true
    const val LOCAL_BADGE = true
    const val LANGUAGE_BADGE = true
    const val SOURCE_BADGE = true
    const val USE_LANGUAGE_ICON = true
    const val SHOW_UPDATES_COUNT = true
    const val NEW_UPDATES_COUNT = 0
    const val CATEGORY_TABS = true
    const val CATEGORY_NUMBER_OF_ITEMS = false
    const val CATEGORIZED_DISPLAY_SETTINGS = false
    const val SHOW_HIDDEN_CATEGORIES = false
    const val DEFAULT_CHAPTER_FILTER_BY_READ = MangaChapterFlags.SHOW_ALL
    const val DEFAULT_CHAPTER_FILTER_BY_DOWNLOADED = MangaChapterFlags.SHOW_ALL
    const val DEFAULT_CHAPTER_FILTER_BY_BOOKMARKED = MangaChapterFlags.SHOW_ALL
    const val DEFAULT_CHAPTER_SORT_BY_SOURCE_OR_NUMBER = MangaChapterFlags.CHAPTER_SORTING_SOURCE
    const val DEFAULT_CHAPTER_DISPLAY_BY_NAME_OR_NUMBER = MangaChapterFlags.CHAPTER_DISPLAY_NAME
    const val DEFAULT_CHAPTER_SORT_BY_ASCENDING_OR_DESCENDING = MangaChapterFlags.CHAPTER_SORT_DESC
    const val AUTO_CLEAR_CHAPTER_CACHE = false
    const val HIDE_MISSING_CHAPTERS = false
    const val SHOW_EMPTY_CATEGORIES_SEARCH = false
    const val UPDATE_MANGA_TITLES = false
    const val DISALLOW_NON_ASCII_FILENAMES = false
    val SORT_TAGS_FOR_LIBRARY: Set<String> = emptySet()
    val GROUP_LIBRARY_UPDATE_TYPE = GroupLibraryMode.GLOBAL
    const val GROUP_LIBRARY_BY = LibraryGroup.BY_DEFAULT
}
