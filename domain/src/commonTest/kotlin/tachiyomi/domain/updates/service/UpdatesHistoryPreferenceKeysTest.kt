package tachiyomi.domain.updates.service

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdatesHistoryPreferenceKeysTest {
    @Test
    fun updatesPreferenceKeysRemainStable() {
        assertEquals(
            "pref_filter_updates_downloaded",
            UpdatesHistoryPreferenceKeys.FILTER_UPDATES_DOWNLOADED,
        )
        assertEquals(
            "pref_filter_updates_unread",
            UpdatesHistoryPreferenceKeys.FILTER_UPDATES_UNREAD,
        )
        assertEquals(
            "pref_filter_updates_started",
            UpdatesHistoryPreferenceKeys.FILTER_UPDATES_STARTED,
        )
        assertEquals(
            "pref_filter_updates_bookmarked",
            UpdatesHistoryPreferenceKeys.FILTER_UPDATES_BOOKMARKED,
        )
        assertEquals(
            "pref_filter_updates_hide_excluded_scanlators",
            UpdatesHistoryPreferenceKeys.FILTER_UPDATES_HIDE_EXCLUDED_SCANLATORS,
        )
    }

    @Test
    fun historyPreferenceKeysRemainStable() {
        assertEquals(
            "pref_filter_history_unfinished_manga",
            UpdatesHistoryPreferenceKeys.FILTER_HISTORY_UNFINISHED_MANGA,
        )
        assertEquals(
            "pref_filter_history_unfinished_chapter",
            UpdatesHistoryPreferenceKeys.FILTER_HISTORY_UNFINISHED_CHAPTER,
        )
        assertEquals(
            "pref_filter_history_non_library_manga",
            UpdatesHistoryPreferenceKeys.FILTER_HISTORY_NON_LIBRARY_MANGA,
        )
        assertEquals(
            "pref_updates_history_screen_use_panorama_cover",
            UpdatesHistoryPreferenceKeys.USE_PANORAMA_COVER,
        )
    }

    @Test
    fun booleanPreferenceDefaultsRemainStable() {
        assertEquals(
            false,
            UpdatesHistoryPreferenceDefaults.FILTER_UPDATES_HIDE_EXCLUDED_SCANLATORS,
        )
        assertEquals(false, UpdatesHistoryPreferenceDefaults.USE_PANORAMA_COVER)
    }
}
