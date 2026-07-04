package tachiyomi.domain.storage.service

import kotlin.test.Test
import kotlin.test.assertEquals

class StorageContractTest {
    @Test
    fun storagePreferenceKeysRemainStable() {
        assertEquals("storage_dir", StoragePreferenceKeys.BASE_STORAGE_DIRECTORY)
    }

    @Test
    fun storageDirectoryNamesRemainStable() {
        assertEquals("autobackup", StorageDirectoryNames.AUTOMATIC_BACKUPS)
        assertEquals("downloads", StorageDirectoryNames.DOWNLOADS)
        assertEquals("local", StorageDirectoryNames.LOCAL_SOURCE)
        assertEquals("logs", StorageDirectoryNames.LOGS)
    }

    @Test
    fun novelStorageFileNamesRemainStable() {
        assertEquals("metadata.json", NovelStorageFileNames.METADATA)
        assertEquals("bookmark.json", NovelStorageFileNames.BOOKMARK)
        assertEquals("bookinfo.json", NovelStorageFileNames.BOOK_INFO)
        assertEquals("statistics.json", NovelStorageFileNames.STATISTICS)
        assertEquals("sasayaki_matches.json", NovelStorageFileNames.SASAYAKI_MATCHES)
        assertEquals("sasayaki_playback.json", NovelStorageFileNames.SASAYAKI_PLAYBACK)
        assertEquals("anki_stats.json", NovelStorageFileNames.ANKI_STATS)
        assertEquals("manga_stats.json", NovelStorageFileNames.MANGA_STATS)
    }
}
