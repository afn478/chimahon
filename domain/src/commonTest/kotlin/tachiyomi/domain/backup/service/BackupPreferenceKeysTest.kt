package tachiyomi.domain.backup.service

import kotlin.test.Test
import kotlin.test.assertEquals

class BackupPreferenceKeysTest {
    @Test
    fun backupPreferenceKeysRemainStable() {
        assertEquals("backup_interval", BackupPreferenceKeys.BACKUP_INTERVAL)
        assertEquals("last_auto_backup_timestamp", BackupPreferenceKeys.LAST_AUTO_BACKUP_TIMESTAMP)
        assertEquals(
            "pref_show_restoring_progress_banner_key",
            BackupPreferenceKeys.SHOW_RESTORING_PROGRESS_BANNER,
        )
    }

    @Test
    fun backupPreferenceDefaultsRemainStable() {
        assertEquals(12, BackupPreferenceDefaults.BACKUP_INTERVAL_HOURS)
        assertEquals(0L, BackupPreferenceDefaults.LAST_AUTO_BACKUP_TIMESTAMP)
        assertEquals(true, BackupPreferenceDefaults.SHOW_RESTORING_PROGRESS_BANNER)
    }
}
