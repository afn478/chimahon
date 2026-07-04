package tachiyomi.domain.history.service

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.updates.service.UpdatesHistoryPreferenceDefaults
import tachiyomi.domain.updates.service.UpdatesHistoryPreferenceKeys

class HistoryPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun filterUnfinishedManga() = preferenceStore.getEnum(
        UpdatesHistoryPreferenceKeys.FILTER_HISTORY_UNFINISHED_MANGA,
        TriState.DISABLED,
    )

    fun filterUnfinishedChapter() = preferenceStore.getEnum(
        UpdatesHistoryPreferenceKeys.FILTER_HISTORY_UNFINISHED_CHAPTER,
        TriState.DISABLED,
    )

    fun filterNonLibraryManga() = preferenceStore.getEnum(
        UpdatesHistoryPreferenceKeys.FILTER_HISTORY_NON_LIBRARY_MANGA,
        TriState.DISABLED,
    )

    fun usePanoramaCover() = preferenceStore.getBoolean(
        UpdatesHistoryPreferenceKeys.USE_PANORAMA_COVER,
        UpdatesHistoryPreferenceDefaults.USE_PANORAMA_COVER,
    )
}
