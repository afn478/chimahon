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

    @Test
    fun applySettingsActionCapturesProgressBeforeReloadWhenModeChangesWithCurrentUrl() {
        assertEquals(
            NovelReaderWebSettingsPolicy.ApplySettingsAction.CaptureProgressThenReload(
                url = "file:///chapter.xhtml",
                continuousMode = true,
                progressScript = NovelReaderWebScriptPolicy.calculateProgressScript(),
            ),
            NovelReaderWebSettingsPolicy.applySettingsAction(
                currentContinuousMode = false,
                nextSettings = ReaderSettings(continuousMode = true),
                currentUrl = "file:///chapter.xhtml",
            ),
        )
    }

    @Test
    fun applySettingsActionOnlySetsContinuousModeWhenReloadHasNoCurrentUrl() {
        assertEquals(
            NovelReaderWebSettingsPolicy.ApplySettingsAction.SetContinuousMode(
                continuousMode = false,
            ),
            NovelReaderWebSettingsPolicy.applySettingsAction(
                currentContinuousMode = true,
                nextSettings = ReaderSettings(continuousMode = false),
                currentUrl = null,
            ),
        )
    }

    @Test
    fun applySettingsActionBuildsDomUpdatePayloadWhenModeIsUnchanged() {
        val settings = ReaderSettings(
            continuousMode = true,
            backgroundColor = 0x00123456,
        )

        assertEquals(
            NovelReaderWebSettingsPolicy.ApplySettingsAction.ApplyDomUpdates(
                settings = settings,
                backgroundColor = settings.backgroundColor,
                script = NovelReaderWebInjectionPolicy.liveSettingsScript(settings),
            ),
            NovelReaderWebSettingsPolicy.applySettingsAction(
                currentContinuousMode = true,
                nextSettings = settings,
                currentUrl = "file:///chapter.xhtml",
            ),
        )
    }

    @Test
    fun reloadProgressParsesJavascriptProgressOrFallsBackToStart() {
        assertEquals(0.45, NovelReaderWebSettingsPolicy.reloadProgress("\"0.45\""))
        assertEquals(0.0, NovelReaderWebSettingsPolicy.reloadProgress("\"not-progress\""))
        assertEquals(0.0, NovelReaderWebSettingsPolicy.reloadProgress(null))
    }
}
