package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.NovelReaderWebCommand
import tachiyomi.domain.reader.model.ReaderSettings

class NovelReaderWebCommandActionPolicyTest {
    @Test
    fun loadChapterCommandMapsToLoadAction() {
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.LoadChapter(
                url = "file:///chapter.xhtml",
                progress = 0.5,
            ),
            actionFor(
                NovelReaderWebCommand.LoadChapter(
                    url = "file:///chapter.xhtml",
                    progress = 0.5,
                ),
            ),
        )
    }

    @Test
    fun changeModeReloadsCurrentChapterWhenAvailable() {
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.ReloadCurrentChapter(
                url = "file:///chapter.xhtml",
                continuousMode = true,
            ),
            actionFor(
                command = NovelReaderWebCommand.ChangeMode(continuous = true),
                currentUrl = "file:///chapter.xhtml",
            ),
        )
    }

    @Test
    fun changeModeIsIgnoredWithoutCurrentChapter() {
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.Ignore,
            actionFor(
                command = NovelReaderWebCommand.ChangeMode(continuous = true),
                currentUrl = null,
            ),
        )
    }

    @Test
    fun applySettingsActionUsesSettingsPolicy() {
        val settings = ReaderSettings(verticalWriting = false)

        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.ApplySettings(
                settings = settings,
                settingsAction = NovelReaderWebSettingsPolicy.SettingsCommandAction.ReinjectReader,
            ),
            actionFor(
                command = NovelReaderWebCommand.ApplySettings(settings),
                lastAppliedSettings = ReaderSettings(verticalWriting = true),
            ),
        )
    }

    @Test
    fun applySettingsActionCanUseLiveDomUpdates() {
        val settings = ReaderSettings(fontSize = 20.0)

        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.ApplySettings(
                settings = settings,
                settingsAction = NovelReaderWebSettingsPolicy.SettingsCommandAction.ApplyLiveSettings,
            ),
            actionFor(
                command = NovelReaderWebCommand.ApplySettings(settings),
                lastAppliedSettings = ReaderSettings(fontSize = 18.0),
            ),
        )
    }

    @Test
    fun focusAndPaginationCommandsMapToDirectActions() {
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.SetFocusMode(true),
            actionFor(NovelReaderWebCommand.ChangeFocusMode(focusMode = true)),
        )
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.Paginate(forward = false),
            actionFor(NovelReaderWebCommand.Paginate(forward = false)),
        )
    }

    @Test
    fun scriptCommandsMapToEvaluateScriptActions() {
        val jump = actionFor(NovelReaderWebCommand.JumpToFragment("chapter'1"))
        val clear = actionFor(NovelReaderWebCommand.ClearSelection)
        val highlight = actionFor(NovelReaderWebCommand.HighlightSelection(charCount = 42))

        assertTrue(jump is NovelReaderWebCommandActionPolicy.CommandAction.EvaluateScript)
        assertTrue(jump.script.contains("chapter\\'1"))
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.EvaluateScript(
                NovelReaderWebScriptPolicy.clearSelectionScript(),
            ),
            clear,
        )
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.EvaluateScript(
                NovelReaderWebScriptPolicy.highlightSelectionScript(42),
            ),
            highlight,
        )
    }

    @Test
    fun selectionRectCommandMapsToRequestSelectionRectsAction() {
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.RequestSelectionRects(
                NovelReaderWebScriptPolicy.selectionRectsScript(
                    charCount = 42,
                    startOffset = 7,
                ),
            ),
            actionFor(
                NovelReaderWebCommand.GetSelectionRects(
                    charCount = 42,
                    startOffset = 7,
                ),
            ),
        )
    }

    @Test
    fun unsupportedWebCommandsMapToIgnoreAction() {
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.Ignore,
            actionFor(NovelReaderWebCommand.ApplySasayakiCues(cuesJson = "[]")),
        )
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.Ignore,
            actionFor(NovelReaderWebCommand.ClearSasayakiCue),
        )
        assertEquals(
            NovelReaderWebCommandActionPolicy.CommandAction.Ignore,
            actionFor(NovelReaderWebCommand.UpdateTextColor(hex = "#ffffff")),
        )
    }

    private fun actionFor(
        command: NovelReaderWebCommand,
        currentUrl: String? = null,
        lastAppliedSettings: ReaderSettings = ReaderSettings(),
    ): NovelReaderWebCommandActionPolicy.CommandAction {
        return NovelReaderWebCommandActionPolicy.commandAction(
            command = command,
            currentUrl = currentUrl,
            lastAppliedSettings = lastAppliedSettings,
        )
    }
}
