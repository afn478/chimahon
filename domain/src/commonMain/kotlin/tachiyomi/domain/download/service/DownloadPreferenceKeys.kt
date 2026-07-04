package tachiyomi.domain.download.service

object DownloadPreferenceKeys {
    const val DOWNLOAD_ONLY_OVER_WIFI = "pref_download_only_over_wifi_key"
    const val SAVE_CHAPTERS_AS_CBZ = "save_chapter_as_cbz"
    const val SPLIT_TALL_IMAGES = "split_tall_images"
    const val AUTO_DOWNLOAD_WHILE_READING = "auto_download_while_reading"
    const val REMOVE_AFTER_READ_SLOTS = "remove_after_read_slots"
    const val REMOVE_AFTER_MARKED_AS_READ = "pref_remove_after_marked_as_read_key"
    const val REMOVE_BOOKMARKED_CHAPTERS = "pref_remove_bookmarked"
    const val REMOVE_EXCLUDE_CATEGORIES = "remove_exclude_categories"
    const val DOWNLOAD_NEW_CHAPTERS = "download_new"
    const val DOWNLOAD_NEW_CATEGORIES = "download_new_categories"
    const val DOWNLOAD_NEW_CATEGORIES_EXCLUDE = "download_new_categories_exclude"
    const val DOWNLOAD_NEW_UNREAD_CHAPTERS_ONLY = "download_new_unread_chapters_only"
    const val PARALLEL_SOURCE_LIMIT = "download_parallel_source_limit"
    const val PARALLEL_PAGE_LIMIT = "download_parallel_page_limit"
    const val INCLUDE_CHAPTER_URL_HASH = "download_include_chapter_url_hash"
    const val DOWNLOAD_CACHE_RENEW_INTERVAL = "download_cache_renew_interval"

    val categoryPreferenceKeys = setOf(
        REMOVE_EXCLUDE_CATEGORIES,
        DOWNLOAD_NEW_CATEGORIES,
        DOWNLOAD_NEW_CATEGORIES_EXCLUDE,
    )
}
