package mihon.core.migration.migrations

import android.app.Application
import exh.source.ExhPreferenceKeys
import mihon.core.migration.MigrateUtils
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferenceKeys
import tachiyomi.domain.storage.service.StoragePreferenceKeys

class MoveSettingsToPrivateOrAppStateMigration : Migration {
    override val version: Float = 59f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return@withIOContext false
        val prefsToReplace = listOf(
            "pref_download_only",
            "incognito_mode",
            "last_catalogue_source",
            "trusted_signatures",
            "last_app_closed",
            LibraryPreferenceKeys.LAST_UPDATED_TIMESTAMP,
            LibraryPreferenceKeys.NEW_UPDATES_COUNT,
            LibraryPreferenceKeys.LAST_USED_CATEGORY,
            "last_app_check",
            "last_ext_check",
            "last_version_code",
            "skip_pre_migration",
            ExhPreferenceKeys.AUTO_UPDATE_STATS,
            StoragePreferenceKeys.BASE_STORAGE_DIRECTORY,
        )
        MigrateUtils.replacePreferences(
            preferenceStore = preferenceStore,
            filterPredicate = { it.key in prefsToReplace },
            newKey = { Preference.appStateKey(it) },
        )

        val privatePrefsToReplace = listOf(
            "sql_password",
            "encrypt_database",
            "cbz_password",
            "password_protect_downloads",
            ExhPreferenceKeys.MEMBER_ID,
            ExhPreferenceKeys.ENABLE_EXHENTAI,
            ExhPreferenceKeys.MEMBER_ID,
            ExhPreferenceKeys.PASS_HASH,
            ExhPreferenceKeys.IGNEOUS,
            ExhPreferenceKeys.EH_SETTINGS_PROFILE,
            ExhPreferenceKeys.EXH_SETTINGS_PROFILE,
            ExhPreferenceKeys.SETTINGS_KEY,
            ExhPreferenceKeys.SESSION_COOKIE,
            ExhPreferenceKeys.HATH_PERKS_COOKIE,
        )

        MigrateUtils.replacePreferences(
            preferenceStore = preferenceStore,
            filterPredicate = { it.key in privatePrefsToReplace },
            newKey = { Preference.privateKey(it) },
        )

        // Deleting old download cache index files, but might as well clear it all out
        context.cacheDir.deleteRecursively()

        return@withIOContext true
    }
}
