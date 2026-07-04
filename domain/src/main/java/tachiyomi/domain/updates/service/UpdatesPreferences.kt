package tachiyomi.domain.updates.service

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum

class UpdatesPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun filterDownloaded() = preferenceStore.getEnum(
        UpdatesHistoryPreferenceKeys.FILTER_UPDATES_DOWNLOADED,
        TriState.DISABLED,
    )

    fun filterUnread() = preferenceStore.getEnum(
        UpdatesHistoryPreferenceKeys.FILTER_UPDATES_UNREAD,
        TriState.DISABLED,
    )

    fun filterStarted() = preferenceStore.getEnum(
        UpdatesHistoryPreferenceKeys.FILTER_UPDATES_STARTED,
        TriState.DISABLED,
    )

    fun filterBookmarked() = preferenceStore.getEnum(
        UpdatesHistoryPreferenceKeys.FILTER_UPDATES_BOOKMARKED,
        TriState.DISABLED,
    )

    fun filterExcludedScanlators() = preferenceStore.getBoolean(
        UpdatesHistoryPreferenceKeys.FILTER_UPDATES_HIDE_EXCLUDED_SCANLATORS,
        UpdatesHistoryPreferenceDefaults.FILTER_UPDATES_HIDE_EXCLUDED_SCANLATORS,
    )

    // KMK -->
    fun usePanoramaCover() = preferenceStore.getBoolean(
        UpdatesHistoryPreferenceKeys.USE_PANORAMA_COVER,
        UpdatesHistoryPreferenceDefaults.USE_PANORAMA_COVER,
    )
    // KMK <--
}

// KMK -->
const val USE_PANORAMA_COVER_PREF = UpdatesHistoryPreferenceKeys.USE_PANORAMA_COVER
// KMK <--
