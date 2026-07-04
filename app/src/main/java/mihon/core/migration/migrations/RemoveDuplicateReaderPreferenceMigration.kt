package mihon.core.migration.migrations

import android.content.SharedPreferences
import androidx.core.content.edit
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryDuplicateChapterReadPreference
import tachiyomi.domain.library.service.LibraryPreferenceKeys

class RemoveDuplicateReaderPreferenceMigration : Migration {
    override val version: Float = 75f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val prefs = migrationContext.get<SharedPreferences>() ?: return@withIOContext false

        if (prefs.getBoolean("mark_read_dupe", false)) {
            val readPrefSet = prefs
                .getStringSet(LibraryPreferenceKeys.MARK_DUPLICATE_READ_CHAPTER_AS_READ, emptySet())
                ?.toMutableSet()
            readPrefSet?.add(LibraryDuplicateChapterReadPreference.EXISTING)
            prefs.edit {
                putStringSet(LibraryPreferenceKeys.MARK_DUPLICATE_READ_CHAPTER_AS_READ, readPrefSet)
                remove("mark_read_dupe")
            }
        }

        return@withIOContext true
    }
}
