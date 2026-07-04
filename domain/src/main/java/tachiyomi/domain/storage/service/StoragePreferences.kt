package tachiyomi.domain.storage.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.storage.FolderProvider

class StoragePreferences(
    private val folderProvider: FolderProvider,
    private val preferenceStore: PreferenceStore,
) {

    // Storing URI of the directory (either file:/// or storage://
    fun baseStorageDirectory() = preferenceStore.getString(
        Preference.appStateKey(StoragePreferenceKeys.BASE_STORAGE_DIRECTORY),
        folderProvider.path(),
    )
}
