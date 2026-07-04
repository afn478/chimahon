package tachiyomi.domain.download.service

import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadPreferenceKeysTest {
    @Test
    fun downloadPreferenceKeysRemainStable() {
        assertEquals(
            "pref_download_only_over_wifi_key",
            DownloadPreferenceKeys.DOWNLOAD_ONLY_OVER_WIFI,
        )
        assertEquals("save_chapter_as_cbz", DownloadPreferenceKeys.SAVE_CHAPTERS_AS_CBZ)
        assertEquals("split_tall_images", DownloadPreferenceKeys.SPLIT_TALL_IMAGES)
        assertEquals(
            "auto_download_while_reading",
            DownloadPreferenceKeys.AUTO_DOWNLOAD_WHILE_READING,
        )
        assertEquals("remove_after_read_slots", DownloadPreferenceKeys.REMOVE_AFTER_READ_SLOTS)
        assertEquals(
            "pref_remove_after_marked_as_read_key",
            DownloadPreferenceKeys.REMOVE_AFTER_MARKED_AS_READ,
        )
        assertEquals("pref_remove_bookmarked", DownloadPreferenceKeys.REMOVE_BOOKMARKED_CHAPTERS)
        assertEquals("download_new", DownloadPreferenceKeys.DOWNLOAD_NEW_CHAPTERS)
        assertEquals(
            "download_new_unread_chapters_only",
            DownloadPreferenceKeys.DOWNLOAD_NEW_UNREAD_CHAPTERS_ONLY,
        )
        assertEquals(
            "download_parallel_source_limit",
            DownloadPreferenceKeys.PARALLEL_SOURCE_LIMIT,
        )
        assertEquals(
            "download_parallel_page_limit",
            DownloadPreferenceKeys.PARALLEL_PAGE_LIMIT,
        )
        assertEquals(
            "download_include_chapter_url_hash",
            DownloadPreferenceKeys.INCLUDE_CHAPTER_URL_HASH,
        )
        assertEquals(
            "download_cache_renew_interval",
            DownloadPreferenceKeys.DOWNLOAD_CACHE_RENEW_INTERVAL,
        )
    }

    @Test
    fun downloadPreferenceDefaultsRemainStable() {
        assertEquals(true, DownloadPreferenceDefaults.DOWNLOAD_ONLY_OVER_WIFI)
        assertEquals(true, DownloadPreferenceDefaults.SAVE_CHAPTERS_AS_CBZ)
        assertEquals(true, DownloadPreferenceDefaults.SPLIT_TALL_IMAGES)
        assertEquals(0, DownloadPreferenceDefaults.AUTO_DOWNLOAD_WHILE_READING)
        assertEquals(-1, DownloadPreferenceDefaults.REMOVE_AFTER_READ_SLOTS)
        assertEquals(false, DownloadPreferenceDefaults.REMOVE_AFTER_MARKED_AS_READ)
        assertEquals(false, DownloadPreferenceDefaults.REMOVE_BOOKMARKED_CHAPTERS)
        assertEquals(false, DownloadPreferenceDefaults.DOWNLOAD_NEW_CHAPTERS)
        assertEquals(false, DownloadPreferenceDefaults.DOWNLOAD_NEW_UNREAD_CHAPTERS_ONLY)
        assertEquals(5, DownloadPreferenceDefaults.PARALLEL_SOURCE_LIMIT)
        assertEquals(5, DownloadPreferenceDefaults.PARALLEL_PAGE_LIMIT)
        assertEquals(true, DownloadPreferenceDefaults.INCLUDE_CHAPTER_URL_HASH)
        assertEquals(1, DownloadPreferenceDefaults.DOWNLOAD_CACHE_RENEW_INTERVAL)
    }

    @Test
    fun categoryPreferenceKeysRemainStableForBackupRestore() {
        assertEquals("remove_exclude_categories", DownloadPreferenceKeys.REMOVE_EXCLUDE_CATEGORIES)
        assertEquals("download_new_categories", DownloadPreferenceKeys.DOWNLOAD_NEW_CATEGORIES)
        assertEquals(
            "download_new_categories_exclude",
            DownloadPreferenceKeys.DOWNLOAD_NEW_CATEGORIES_EXCLUDE,
        )
        assertEquals(
            setOf(
                DownloadPreferenceKeys.REMOVE_EXCLUDE_CATEGORIES,
                DownloadPreferenceKeys.DOWNLOAD_NEW_CATEGORIES,
                DownloadPreferenceKeys.DOWNLOAD_NEW_CATEGORIES_EXCLUDE,
            ),
            DownloadPreferenceKeys.categoryPreferenceKeys,
        )
    }
}
