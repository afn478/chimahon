package tachiyomi.domain.library.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.LibraryUpdateMangaRestrictions
import tachiyomi.domain.manga.model.Manga

class LibraryPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun displayMode() = preferenceStore.getObjectFromString(
        LibraryPreferenceKeys.DISPLAY_MODE,
        LibraryDisplayMode.default,
        LibraryDisplayMode.Serializer::serialize,
        LibraryDisplayMode.Serializer::deserialize,
    )

    fun sortingMode() = preferenceStore.getObjectFromString(
        LibraryPreferenceKeys.SORTING_MODE,
        LibrarySort.default,
        LibrarySort.Serializer::serialize,
        LibrarySort.Serializer::deserialize,
    )

    fun randomSortSeed() = preferenceStore.getInt(
        LibraryPreferenceKeys.RANDOM_SORT_SEED,
        LibraryPreferenceDefaults.RANDOM_SORT_SEED,
    )

    fun portraitColumns() = preferenceStore.getInt(
        LibraryPreferenceKeys.PORTRAIT_COLUMNS,
        LibraryPreferenceDefaults.PORTRAIT_COLUMNS,
    )
    fun landscapeColumns() = preferenceStore.getInt(
        LibraryPreferenceKeys.LANDSCAPE_COLUMNS,
        LibraryPreferenceDefaults.LANDSCAPE_COLUMNS,
    )

    fun novelPortraitColumns() = preferenceStore.getInt(
        LibraryPreferenceKeys.NOVEL_PORTRAIT_COLUMNS,
        LibraryPreferenceDefaults.NOVEL_PORTRAIT_COLUMNS,
    )
    fun novelLandscapeColumns() = preferenceStore.getInt(
        LibraryPreferenceKeys.NOVEL_LANDSCAPE_COLUMNS,
        LibraryPreferenceDefaults.NOVEL_LANDSCAPE_COLUMNS,
    )

    fun lastUpdatedTimestamp() = preferenceStore.getLong(
        Preference.appStateKey(LibraryPreferenceKeys.LAST_UPDATED_TIMESTAMP),
        LibraryPreferenceDefaults.LAST_UPDATED_TIMESTAMP,
    )
    fun autoUpdateInterval() = preferenceStore.getInt(
        LibraryPreferenceKeys.AUTO_UPDATE_INTERVAL,
        LibraryPreferenceDefaults.AUTO_UPDATE_INTERVAL,
    )

    // KMK -->
    fun showUpdatingProgressBanner() = preferenceStore.getBoolean(
        Preference.appStateKey(LibraryPreferenceKeys.SHOW_UPDATING_PROGRESS_BANNER),
        LibraryPreferenceDefaults.SHOW_UPDATING_PROGRESS_BANNER,
    )
    // KMK <--

    fun coverRatios() = preferenceStore.getStringSet(
        Preference.appStateKey(LibraryPreferenceKeys.COVER_RATIOS),
        LibraryPreferenceDefaults.COVER_RATIOS,
    )

    fun coverColors() = preferenceStore.getStringSet(
        Preference.appStateKey(LibraryPreferenceKeys.COVER_COLORS),
        LibraryPreferenceDefaults.COVER_COLORS,
    )
    // KMK <--

    fun autoUpdateDeviceRestrictions() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.AUTO_UPDATE_DEVICE_RESTRICTIONS,
        LibraryPreferenceDefaults.AUTO_UPDATE_DEVICE_RESTRICTIONS,
    )
    fun autoUpdateMangaRestrictions() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.AUTO_UPDATE_MANGA_RESTRICTIONS,
        LibraryPreferenceDefaults.AUTO_UPDATE_MANGA_RESTRICTIONS,
    )

    fun autoUpdateMetadata() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.AUTO_UPDATE_METADATA,
        LibraryPreferenceDefaults.AUTO_UPDATE_METADATA,
    )

    // KMK -->
    fun fetchMetadataOnAdd() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.FETCH_METADATA_ON_ADD,
        LibraryPreferenceDefaults.FETCH_METADATA_ON_ADD,
    )
    fun fetchChaptersOnAdd() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.FETCH_CHAPTERS_ON_ADD,
        LibraryPreferenceDefaults.FETCH_CHAPTERS_ON_ADD,
    )
    // KMK <--

    fun showContinueReadingButton() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.SHOW_CONTINUE_READING_BUTTON,
        LibraryPreferenceDefaults.SHOW_CONTINUE_READING_BUTTON,
    )

    fun markDuplicateReadChapterAsRead() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.MARK_DUPLICATE_READ_CHAPTER_AS_READ,
        LibraryPreferenceDefaults.MARK_DUPLICATE_READ_CHAPTER_AS_READ,
    )

    // region Filter

    fun filterDownloaded() = preferenceStore.getEnum(
        LibraryPreferenceKeys.FILTER_DOWNLOADED,
        TriState.DISABLED,
    )

    fun filterUnread() = preferenceStore.getEnum(
        LibraryPreferenceKeys.FILTER_UNREAD,
        TriState.DISABLED,
    )

    fun filterStarted() = preferenceStore.getEnum(
        LibraryPreferenceKeys.FILTER_STARTED,
        TriState.DISABLED,
    )

    fun filterBookmarked() = preferenceStore.getEnum(
        LibraryPreferenceKeys.FILTER_BOOKMARKED,
        TriState.DISABLED,
    )

    fun filterCompleted() = preferenceStore.getEnum(
        LibraryPreferenceKeys.FILTER_COMPLETED,
        TriState.DISABLED,
    )

    fun filterIntervalCustom() = preferenceStore.getEnum(
        LibraryPreferenceKeys.FILTER_INTERVAL_CUSTOM,
        TriState.DISABLED,
    )

    // SY -->
    fun filterLewd() = preferenceStore.getEnum(
        LibraryPreferenceKeys.FILTER_LEWD,
        TriState.DISABLED,
    )
    // SY <--

    // KMK -->
    fun filterCategories() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.FILTER_CATEGORIES,
        LibraryPreferenceDefaults.FILTER_CATEGORIES,
    )

    fun filterCategoriesInclude() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.FILTER_LIBRARY_CATEGORIES_INCLUDE,
        emptySet(),
    )

    fun filterCategoriesExclude() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.FILTER_LIBRARY_CATEGORIES_EXCLUDE,
        emptySet(),
    )
    // KMK <--

    fun filterTracking(id: Int) = preferenceStore.getEnum(
        LibraryPreferenceKeys.filterTracking(id),
        TriState.DISABLED,
    )

    // endregion

    // region Badges

    fun downloadBadge() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.DOWNLOAD_BADGE,
        LibraryPreferenceDefaults.DOWNLOAD_BADGE,
    )

    fun unreadBadge() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.UNREAD_BADGE,
        LibraryPreferenceDefaults.UNREAD_BADGE,
    )

    fun localBadge() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.LOCAL_BADGE,
        LibraryPreferenceDefaults.LOCAL_BADGE,
    )

    fun languageBadge() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.LANGUAGE_BADGE,
        LibraryPreferenceDefaults.LANGUAGE_BADGE,
    )

    // KMK -->
    fun sourceBadge() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.SOURCE_BADGE,
        LibraryPreferenceDefaults.SOURCE_BADGE,
    )

    fun useLangIcon() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.USE_LANGUAGE_ICON,
        LibraryPreferenceDefaults.USE_LANGUAGE_ICON,
    )
    // KMK <--

    fun newShowUpdatesCount() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.SHOW_UPDATES_COUNT,
        LibraryPreferenceDefaults.SHOW_UPDATES_COUNT,
    )
    fun newUpdatesCount() = preferenceStore.getInt(
        Preference.appStateKey(LibraryPreferenceKeys.NEW_UPDATES_COUNT),
        LibraryPreferenceDefaults.NEW_UPDATES_COUNT,
    )

    // endregion

    // region Category

    fun defaultCategory() = preferenceStore.getInt(
        DEFAULT_CATEGORY_PREF_KEY,
        LibraryPreferenceDefaults.DEFAULT_CATEGORY,
    )

    fun novelDefaultCategory() = preferenceStore.getString(
        NOVEL_DEFAULT_CATEGORY_PREF_KEY,
        LibraryPreferenceDefaults.NOVEL_DEFAULT_CATEGORY,
    )

    fun lastUsedCategory() = preferenceStore.getInt(
        Preference.appStateKey(LibraryPreferenceKeys.LAST_USED_CATEGORY),
        LibraryPreferenceDefaults.LAST_USED_CATEGORY,
    )

    fun categoryTabs() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.CATEGORY_TABS,
        LibraryPreferenceDefaults.CATEGORY_TABS,
    )

    fun categoryNumberOfItems() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.CATEGORY_NUMBER_OF_ITEMS,
        LibraryPreferenceDefaults.CATEGORY_NUMBER_OF_ITEMS,
    )

    fun categorizedDisplaySettings() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.CATEGORIZED_DISPLAY_SETTINGS,
        LibraryPreferenceDefaults.CATEGORIZED_DISPLAY_SETTINGS,
    )

    // KMK -->
    fun showHiddenCategories() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.SHOW_HIDDEN_CATEGORIES,
        LibraryPreferenceDefaults.SHOW_HIDDEN_CATEGORIES,
    )
    // KMK <--

    fun updateCategories() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.LIBRARY_UPDATE_CATEGORIES,
        emptySet(),
    )

    fun updateCategoriesExclude() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.LIBRARY_UPDATE_CATEGORIES_EXCLUDE,
        emptySet(),
    )

    // endregion

    // region Chapter

    fun filterChapterByRead() = preferenceStore.getLong(
        LibraryPreferenceKeys.DEFAULT_CHAPTER_FILTER_BY_READ,
        LibraryPreferenceDefaults.DEFAULT_CHAPTER_FILTER_BY_READ,
    )

    fun filterChapterByDownloaded() = preferenceStore.getLong(
        LibraryPreferenceKeys.DEFAULT_CHAPTER_FILTER_BY_DOWNLOADED,
        LibraryPreferenceDefaults.DEFAULT_CHAPTER_FILTER_BY_DOWNLOADED,
    )

    fun filterChapterByBookmarked() = preferenceStore.getLong(
        LibraryPreferenceKeys.DEFAULT_CHAPTER_FILTER_BY_BOOKMARKED,
        LibraryPreferenceDefaults.DEFAULT_CHAPTER_FILTER_BY_BOOKMARKED,
    )

    // and upload date
    fun sortChapterBySourceOrNumber() = preferenceStore.getLong(
        LibraryPreferenceKeys.DEFAULT_CHAPTER_SORT_BY_SOURCE_OR_NUMBER,
        LibraryPreferenceDefaults.DEFAULT_CHAPTER_SORT_BY_SOURCE_OR_NUMBER,
    )

    fun displayChapterByNameOrNumber() = preferenceStore.getLong(
        LibraryPreferenceKeys.DEFAULT_CHAPTER_DISPLAY_BY_NAME_OR_NUMBER,
        LibraryPreferenceDefaults.DEFAULT_CHAPTER_DISPLAY_BY_NAME_OR_NUMBER,
    )

    fun sortChapterByAscendingOrDescending() = preferenceStore.getLong(
        LibraryPreferenceKeys.DEFAULT_CHAPTER_SORT_BY_ASCENDING_OR_DESCENDING,
        LibraryPreferenceDefaults.DEFAULT_CHAPTER_SORT_BY_ASCENDING_OR_DESCENDING,
    )

    fun setChapterSettingsDefault(manga: Manga) {
        filterChapterByRead().set(manga.unreadFilterRaw)
        filterChapterByDownloaded().set(manga.downloadedFilterRaw)
        filterChapterByBookmarked().set(manga.bookmarkedFilterRaw)
        sortChapterBySourceOrNumber().set(manga.sorting)
        displayChapterByNameOrNumber().set(manga.displayMode)
        sortChapterByAscendingOrDescending().set(
            if (manga.sortDescending()) Manga.CHAPTER_SORT_DESC else Manga.CHAPTER_SORT_ASC,
        )
    }

    fun autoClearChapterCache() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.AUTO_CLEAR_CHAPTER_CACHE,
        LibraryPreferenceDefaults.AUTO_CLEAR_CHAPTER_CACHE,
    )

    fun hideMissingChapters() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.HIDE_MISSING_CHAPTERS,
        LibraryPreferenceDefaults.HIDE_MISSING_CHAPTERS,
    )

    // KMK -->
    fun showEmptyCategoriesSearch() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.SHOW_EMPTY_CATEGORIES_SEARCH,
        LibraryPreferenceDefaults.SHOW_EMPTY_CATEGORIES_SEARCH,
    )
    // KMK <--
    // endregion

    // region Swipe Actions

    fun swipeToStartAction() = preferenceStore.getEnum(
        LibraryPreferenceKeys.CHAPTER_SWIPE_END_ACTION,
        ChapterSwipeAction.ToggleBookmark,
    )

    fun swipeToEndAction() = preferenceStore.getEnum(
        LibraryPreferenceKeys.CHAPTER_SWIPE_START_ACTION,
        ChapterSwipeAction.ToggleRead,
    )

    fun updateMangaTitles() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.UPDATE_MANGA_TITLES,
        LibraryPreferenceDefaults.UPDATE_MANGA_TITLES,
    )

    fun disallowNonAsciiFilenames() = preferenceStore.getBoolean(
        LibraryPreferenceKeys.DISALLOW_NON_ASCII_FILENAMES,
        LibraryPreferenceDefaults.DISALLOW_NON_ASCII_FILENAMES,
    )

    // endregion

    enum class ChapterSwipeAction {
        ToggleRead,
        ToggleBookmark,
        Download,
        Disabled,
    }

    // SY -->

    fun sortTagsForLibrary() = preferenceStore.getStringSet(
        LibraryPreferenceKeys.SORT_TAGS_FOR_LIBRARY,
        LibraryPreferenceDefaults.SORT_TAGS_FOR_LIBRARY,
    )

    fun groupLibraryUpdateType() = preferenceStore.getEnum(
        LibraryPreferenceKeys.GROUP_LIBRARY_UPDATE_TYPE,
        LibraryPreferenceDefaults.GROUP_LIBRARY_UPDATE_TYPE,
    )

    fun groupLibraryBy() = preferenceStore.getInt(
        LibraryPreferenceKeys.GROUP_LIBRARY_BY,
        LibraryPreferenceDefaults.GROUP_LIBRARY_BY,
    )

    // SY <--

    companion object {
        const val DEVICE_ONLY_ON_WIFI = LibraryUpdateDeviceRestrictions.DEVICE_ONLY_ON_WIFI
        const val DEVICE_NETWORK_NOT_METERED = LibraryUpdateDeviceRestrictions.DEVICE_NETWORK_NOT_METERED
        const val DEVICE_CHARGING = LibraryUpdateDeviceRestrictions.DEVICE_CHARGING

        const val MANGA_NON_COMPLETED = LibraryUpdateMangaRestrictions.MANGA_NON_COMPLETED
        const val MANGA_HAS_UNREAD = LibraryUpdateMangaRestrictions.MANGA_HAS_UNREAD
        const val MANGA_NON_READ = LibraryUpdateMangaRestrictions.MANGA_NON_READ
        const val MANGA_OUTSIDE_RELEASE_PERIOD = LibraryUpdateMangaRestrictions.MANGA_OUTSIDE_RELEASE_PERIOD

        const val MARK_DUPLICATE_CHAPTER_READ_NEW = LibraryDuplicateChapterReadPreference.NEW
        const val MARK_DUPLICATE_CHAPTER_READ_EXISTING = LibraryDuplicateChapterReadPreference.EXISTING

        const val DEFAULT_CATEGORY_PREF_KEY = LibraryPreferenceKeys.DEFAULT_CATEGORY
        const val NOVEL_DEFAULT_CATEGORY_PREF_KEY = LibraryPreferenceKeys.NOVEL_DEFAULT_CATEGORY

        val categoryPreferenceKeys = LibraryPreferenceKeys.categoryPreferenceKeys
    }
}
