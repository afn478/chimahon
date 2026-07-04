package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.ReaderSettings

class NovelReaderWebSettingsPolicyTest {
    @Test
    fun settingsCommandActionReinjectsWhenPageBreakPolicyChanges() {
        assertEquals(
            NovelReaderWebSettingsPolicy.SettingsCommandAction.ReinjectReader,
            NovelReaderWebSettingsPolicy.settingsCommandAction(
                oldSettings = ReaderSettings(avoidPageBreak = true),
                newSettings = ReaderSettings(avoidPageBreak = false),
            ),
        )
    }

    @Test
    fun settingsCommandActionReinjectsWhenWritingModeChanges() {
        assertEquals(
            NovelReaderWebSettingsPolicy.SettingsCommandAction.ReinjectReader,
            NovelReaderWebSettingsPolicy.settingsCommandAction(
                oldSettings = ReaderSettings(verticalWriting = true),
                newSettings = ReaderSettings(verticalWriting = false),
            ),
        )
    }

    @Test
    fun settingsCommandActionAppliesLiveForAppearanceOnlyChanges() {
        assertEquals(
            NovelReaderWebSettingsPolicy.SettingsCommandAction.ApplyLiveSettings,
            NovelReaderWebSettingsPolicy.settingsCommandAction(
                oldSettings = ReaderSettings(fontSize = 18.0),
                newSettings = ReaderSettings(fontSize = 20.0),
            ),
        )
    }

    @Test
    fun liveSettingsActionReloadsOnlyWhenContinuousModeChanges() {
        assertEquals(
            NovelReaderWebSettingsPolicy.LiveSettingsAction.ReloadCurrentChapter,
            NovelReaderWebSettingsPolicy.liveSettingsAction(
                currentContinuousMode = false,
                nextSettings = ReaderSettings(continuousMode = true),
            ),
        )
        assertEquals(
            NovelReaderWebSettingsPolicy.LiveSettingsAction.ApplyDomUpdates,
            NovelReaderWebSettingsPolicy.liveSettingsAction(
                currentContinuousMode = true,
                nextSettings = ReaderSettings(continuousMode = true),
            ),
        )
    }
}
