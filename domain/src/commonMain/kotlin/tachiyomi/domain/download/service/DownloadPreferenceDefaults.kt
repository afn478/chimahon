package tachiyomi.domain.download.service

object DownloadPreferenceDefaults {
    const val DOWNLOAD_ONLY_OVER_WIFI = true
    const val SAVE_CHAPTERS_AS_CBZ = true
    const val SPLIT_TALL_IMAGES = true
    const val AUTO_DOWNLOAD_WHILE_READING = 0
    const val REMOVE_AFTER_READ_SLOTS = -1
    const val REMOVE_AFTER_MARKED_AS_READ = false
    const val REMOVE_BOOKMARKED_CHAPTERS = false
    const val DOWNLOAD_NEW_CHAPTERS = false
    const val DOWNLOAD_NEW_UNREAD_CHAPTERS_ONLY = false
    const val PARALLEL_SOURCE_LIMIT = 5
    const val PARALLEL_PAGE_LIMIT = 5
    const val INCLUDE_CHAPTER_URL_HASH = true
    const val DOWNLOAD_CACHE_RENEW_INTERVAL = 1
}
