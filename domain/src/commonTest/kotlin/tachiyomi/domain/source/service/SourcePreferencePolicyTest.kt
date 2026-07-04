package tachiyomi.domain.source.service

import kotlin.test.Test
import kotlin.test.assertEquals

class SourcePreferencePolicyTest {
    @Test
    fun toggleLanguageAddsMissingLanguageAndRemovesEnabledLanguage() {
        val enabled = setOf("all", "en")

        assertEquals(
            setOf("all", "en", "ja"),
            SourcePreferencePolicy.toggleLanguage(enabled, "ja"),
        )
        assertEquals(
            setOf("all"),
            SourcePreferencePolicy.toggleLanguage(enabled, "en"),
        )
    }

    @Test
    fun setSourceEnabledUpdatesDisabledSourceIds() {
        val disabled = setOf("1", "2")

        assertEquals(
            setOf("2"),
            SourcePreferencePolicy.setSourceEnabled(
                disabledSourceIds = disabled,
                sourceId = 1,
                enabled = true,
            ),
        )
        assertEquals(
            setOf("1", "2", "3"),
            SourcePreferencePolicy.setSourceEnabled(
                disabledSourceIds = disabled,
                sourceId = 3,
                enabled = false,
            ),
        )
    }

    @Test
    fun setSourceIdsEnabledUpdatesDisabledSourceIdsInBulk() {
        val disabled = setOf("1", "2", "4")

        assertEquals(
            setOf("4"),
            SourcePreferencePolicy.setSourceIdsEnabled(
                disabledSourceIds = disabled,
                sourceIds = listOf(1, 2),
                enabled = true,
            ),
        )
        assertEquals(
            setOf("1", "2", "3", "4"),
            SourcePreferencePolicy.setSourceIdsEnabled(
                disabledSourceIds = disabled,
                sourceIds = listOf(3, 4),
                enabled = false,
            ),
        )
    }

    @Test
    fun togglePinnedSourceUsesPersistedSourceIdStrings() {
        val pinned = setOf("1")

        assertEquals(setOf("1", "2"), SourcePreferencePolicy.togglePinnedSource(pinned, 2))
        assertEquals(emptySet(), SourcePreferencePolicy.togglePinnedSource(pinned, 1))
    }

    @Test
    fun toggleDataSaverExcludedSourceUsesPersistedSourceIdStrings() {
        val excluded = setOf("1")

        assertEquals(setOf("1", "2"), SourcePreferencePolicy.toggleDataSaverExcludedSource(excluded, 2))
        assertEquals(emptySet(), SourcePreferencePolicy.toggleDataSaverExcludedSource(excluded, 1))
    }
}
