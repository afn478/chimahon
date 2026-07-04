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
}
