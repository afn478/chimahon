package tachiyomi.domain.download.service

import tachiyomi.core.common.preference.PreferenceStore

class DownloadPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun downloadOnlyOverWifi() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.DOWNLOAD_ONLY_OVER_WIFI,
        DownloadPreferenceDefaults.DOWNLOAD_ONLY_OVER_WIFI,
    )

    fun saveChaptersAsCBZ() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.SAVE_CHAPTERS_AS_CBZ,
        DownloadPreferenceDefaults.SAVE_CHAPTERS_AS_CBZ,
    )

    fun splitTallImages() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.SPLIT_TALL_IMAGES,
        DownloadPreferenceDefaults.SPLIT_TALL_IMAGES,
    )

    fun autoDownloadWhileReading() = preferenceStore.getInt(
        DownloadPreferenceKeys.AUTO_DOWNLOAD_WHILE_READING,
        DownloadPreferenceDefaults.AUTO_DOWNLOAD_WHILE_READING,
    )

    fun removeAfterReadSlots() = preferenceStore.getInt(
        DownloadPreferenceKeys.REMOVE_AFTER_READ_SLOTS,
        DownloadPreferenceDefaults.REMOVE_AFTER_READ_SLOTS,
    )

    fun removeAfterMarkedAsRead() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.REMOVE_AFTER_MARKED_AS_READ,
        DownloadPreferenceDefaults.REMOVE_AFTER_MARKED_AS_READ,
    )

    fun removeBookmarkedChapters() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.REMOVE_BOOKMARKED_CHAPTERS,
        DownloadPreferenceDefaults.REMOVE_BOOKMARKED_CHAPTERS,
    )

    fun removeExcludeCategories() = preferenceStore.getStringSet(
        DownloadPreferenceKeys.REMOVE_EXCLUDE_CATEGORIES,
        emptySet(),
    )

    fun downloadNewChapters() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.DOWNLOAD_NEW_CHAPTERS,
        DownloadPreferenceDefaults.DOWNLOAD_NEW_CHAPTERS,
    )

    fun downloadNewChapterCategories() = preferenceStore.getStringSet(
        DownloadPreferenceKeys.DOWNLOAD_NEW_CATEGORIES,
        emptySet(),
    )

    fun downloadNewChapterCategoriesExclude() =
        preferenceStore.getStringSet(
            DownloadPreferenceKeys.DOWNLOAD_NEW_CATEGORIES_EXCLUDE,
            emptySet(),
        )

    fun downloadNewUnreadChaptersOnly() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.DOWNLOAD_NEW_UNREAD_CHAPTERS_ONLY,
        DownloadPreferenceDefaults.DOWNLOAD_NEW_UNREAD_CHAPTERS_ONLY,
    )

    fun parallelSourceLimit() = preferenceStore.getInt(
        DownloadPreferenceKeys.PARALLEL_SOURCE_LIMIT,
        DownloadPreferenceDefaults.PARALLEL_SOURCE_LIMIT,
    )

    fun parallelPageLimit() = preferenceStore.getInt(
        DownloadPreferenceKeys.PARALLEL_PAGE_LIMIT,
        DownloadPreferenceDefaults.PARALLEL_PAGE_LIMIT,
    )

    // SY -->
    fun includeChapterUrlHash() = preferenceStore.getBoolean(
        DownloadPreferenceKeys.INCLUDE_CHAPTER_URL_HASH,
        DownloadPreferenceDefaults.INCLUDE_CHAPTER_URL_HASH,
    )
    // SY <--

    // KMK -->
    fun downloadCacheRenewInterval() = preferenceStore.getInt(
        DownloadPreferenceKeys.DOWNLOAD_CACHE_RENEW_INTERVAL,
        DownloadPreferenceDefaults.DOWNLOAD_CACHE_RENEW_INTERVAL,
    )
    // KMK <--

    companion object {
        val categoryPreferenceKeys = DownloadPreferenceKeys.categoryPreferenceKeys
    }
}
