package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.NovelReaderWebCommand
import tachiyomi.domain.reader.model.ReaderSettings

class NovelReaderWebCommandPolicyTest {
    @Test
    fun initialLoadCommandLoadsCurrentChapterWhenQueueIsEmpty() {
        assertEquals(
            NovelReaderWebCommand.LoadChapter(
                url = "file:///chapter.xhtml",
                progress = 0.35,
            ),
            NovelReaderWebCommandPolicy.initialLoadCommand(
                chapterUrl = "file:///chapter.xhtml",
                progress = 0.35,
                hasPendingCommands = false,
            ),
        )
    }

    @Test
    fun initialLoadCommandWaitsForExistingPendingCommands() {
        assertEquals(
            null,
            NovelReaderWebCommandPolicy.initialLoadCommand(
                chapterUrl = "file:///chapter.xhtml",
                progress = 0.35,
                hasPendingCommands = true,
            ),
        )
    }

    @Test
    fun initialLoadCommandIgnoresMissingChapterUrl() {
        assertEquals(
            null,
            NovelReaderWebCommandPolicy.initialLoadCommand(
                chapterUrl = null,
                progress = 0.35,
                hasPendingCommands = false,
            ),
        )
    }

    @Test
    fun reloadChapterCommandReloadsCurrentChapterProgress() {
        assertEquals(
            NovelReaderWebCommand.LoadChapter(
                url = "file:///chapter.xhtml",
                progress = 0.65,
            ),
            NovelReaderWebCommandPolicy.reloadChapterCommand(
                chapterUrl = "file:///chapter.xhtml",
                progress = 0.65,
            ),
        )
    }

    @Test
    fun reloadChapterCommandIgnoresMissingChapterUrl() {
        assertEquals(
            null,
            NovelReaderWebCommandPolicy.reloadChapterCommand(
                chapterUrl = null,
                progress = 0.65,
            ),
        )
    }

    @Test
    fun focusModeCommandBuildsFocusCommand() {
        assertEquals(
            NovelReaderWebCommand.ChangeFocusMode(focusMode = true),
            NovelReaderWebCommandPolicy.focusModeCommand(focusMode = true),
        )
    }

    @Test
    fun settingsCommandBuildsApplySettingsCommand() {
        val settings = ReaderSettings(fontSize = 24.0)

        assertEquals(
            NovelReaderWebCommand.ApplySettings(settings),
            NovelReaderWebCommandPolicy.settingsCommand(settings),
        )
    }

    @Test
    fun continuousModeChangedCommandConsumesInitialValueWithoutCommand() {
        assertEquals(
            NovelReaderWebCommandPolicy.CommandTriggerResult(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                command = null,
            ),
            NovelReaderWebCommandPolicy.continuousModeChangedCommand(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(),
                chapterUrl = "file:///chapter.xhtml",
                progress = 0.4,
            ),
        )
    }

    @Test
    fun continuousModeChangedCommandReloadsAfterInitialValue() {
        assertEquals(
            NovelReaderWebCommandPolicy.CommandTriggerResult(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                command = NovelReaderWebCommand.LoadChapter(
                    url = "file:///chapter.xhtml",
                    progress = 0.4,
                ),
            ),
            NovelReaderWebCommandPolicy.continuousModeChangedCommand(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                chapterUrl = "file:///chapter.xhtml",
                progress = 0.4,
            ),
        )
    }

    @Test
    fun continuousModeChangedCommandIgnoresMissingChapterAfterInitialValue() {
        assertEquals(
            NovelReaderWebCommandPolicy.CommandTriggerResult(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                command = null,
            ),
            NovelReaderWebCommandPolicy.continuousModeChangedCommand(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                chapterUrl = null,
                progress = 0.4,
            ),
        )
    }

    @Test
    fun settingsChangedCommandConsumesInitialValueWithoutCommand() {
        val settings = ReaderSettings(fontSize = 20.0)

        assertEquals(
            NovelReaderWebCommandPolicy.CommandTriggerResult(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                command = null,
            ),
            NovelReaderWebCommandPolicy.settingsChangedCommand(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(),
                settings = settings,
            ),
        )
    }

    @Test
    fun settingsChangedCommandAppliesAfterInitialValue() {
        val settings = ReaderSettings(fontSize = 20.0)

        assertEquals(
            NovelReaderWebCommandPolicy.CommandTriggerResult(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                command = NovelReaderWebCommand.ApplySettings(settings),
            ),
            NovelReaderWebCommandPolicy.settingsChangedCommand(
                state = NovelReaderWebCommandPolicy.CommandTriggerState(
                    hasObservedInitialValue = true,
                ),
                settings = settings,
            ),
        )
    }

    @Test
    fun collapseConsecutiveLoadsKeepsLastSharedLoadCommandInEachRun() {
        val commands = listOf(
            NovelReaderWebCommand.LoadChapter("chapter-1", progress = 0.0),
            NovelReaderWebCommand.LoadChapter("chapter-2", progress = 0.25),
            NovelReaderWebCommand.Paginate(forward = true),
            NovelReaderWebCommand.LoadChapter("chapter-3", progress = 0.5),
        )

        assertEquals(
            listOf(
                NovelReaderWebCommand.LoadChapter("chapter-2", progress = 0.25),
                NovelReaderWebCommand.Paginate(forward = true),
                NovelReaderWebCommand.LoadChapter("chapter-3", progress = 0.5),
            ),
            NovelReaderWebCommandPolicy.collapseConsecutiveLoads(commands),
        )
    }

    @Test
    fun collapseConsecutiveLoadCommandsKeepsLastLoadInEachRun() {
        val commands = listOf(
            loadCommand("chapter-1"),
            loadCommand("chapter-2"),
            otherCommand("apply-settings"),
            loadCommand("chapter-3"),
            loadCommand("chapter-4"),
            otherCommand("paginate"),
            loadCommand("chapter-5"),
        )

        assertEquals(
            listOf(
                loadCommand("chapter-2"),
                otherCommand("apply-settings"),
                loadCommand("chapter-4"),
                otherCommand("paginate"),
                loadCommand("chapter-5"),
            ),
            NovelReaderWebCommandPolicy.collapseConsecutiveLoadCommands(
                commands = commands,
                isLoadCommand = Command::loadsChapter,
            ),
        )
    }

    @Test
    fun collapseConsecutiveLoadCommandsPreservesNonConsecutiveLoads() {
        val commands = listOf(
            loadCommand("chapter-1"),
            otherCommand("apply-settings"),
            loadCommand("chapter-2"),
            otherCommand("paginate"),
        )

        assertEquals(
            commands,
            NovelReaderWebCommandPolicy.collapseConsecutiveLoadCommands(
                commands = commands,
                isLoadCommand = Command::loadsChapter,
            ),
        )
    }

    @Test
    fun collapseConsecutiveLoadCommandsHandlesEmptyQueue() {
        assertEquals(
            emptyList(),
            NovelReaderWebCommandPolicy.collapseConsecutiveLoadCommands(
                commands = emptyList<Command>(),
                isLoadCommand = Command::loadsChapter,
            ),
        )
    }

    private data class Command(
        val name: String,
        val loadsChapter: Boolean,
    )

    private fun loadCommand(name: String): Command {
        return Command(name = name, loadsChapter = true)
    }

    private fun otherCommand(name: String): Command {
        return Command(name = name, loadsChapter = false)
    }
}
