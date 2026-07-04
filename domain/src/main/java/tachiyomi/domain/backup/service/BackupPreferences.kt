package tachiyomi.domain.backup.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class BackupPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun backupInterval() = preferenceStore.getInt(
        BackupPreferenceKeys.BACKUP_INTERVAL,
        BackupPreferenceDefaults.BACKUP_INTERVAL_HOURS,
    )

    fun lastAutoBackupTimestamp() = preferenceStore.getLong(
        Preference.appStateKey(BackupPreferenceKeys.LAST_AUTO_BACKUP_TIMESTAMP),
        BackupPreferenceDefaults.LAST_AUTO_BACKUP_TIMESTAMP,
    )

    // KMK -->
    fun showRestoringProgressBanner() = preferenceStore.getBoolean(
        Preference.appStateKey(BackupPreferenceKeys.SHOW_RESTORING_PROGRESS_BANNER),
        BackupPreferenceDefaults.SHOW_RESTORING_PROGRESS_BANNER,
    )
    // KMK <--
}
