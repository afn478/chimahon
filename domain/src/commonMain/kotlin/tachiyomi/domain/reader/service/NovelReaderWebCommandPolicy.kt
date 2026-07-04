package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderWebCommand
import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderWebCommandPolicy {
    data class CommandTriggerState(
        val hasObservedInitialValue: Boolean = false,
    )

    data class CommandTriggerResult(
        val state: CommandTriggerState,
        val command: NovelReaderWebCommand?,
    )

    fun initialLoadCommand(
        chapterUrl: String?,
        progress: Double,
        hasPendingCommands: Boolean,
    ): NovelReaderWebCommand? {
        if (hasPendingCommands) return null
        return chapterUrl?.let { NovelReaderWebCommand.LoadChapter(it, progress) }
    }

    fun reloadChapterCommand(
        chapterUrl: String?,
        progress: Double,
    ): NovelReaderWebCommand? {
        return chapterUrl?.let { NovelReaderWebCommand.LoadChapter(it, progress) }
    }

    fun focusModeCommand(focusMode: Boolean): NovelReaderWebCommand {
        return NovelReaderWebCommand.ChangeFocusMode(focusMode)
    }

    fun settingsCommand(settings: ReaderSettings): NovelReaderWebCommand {
        return NovelReaderWebCommand.ApplySettings(settings)
    }

    fun continuousModeChangedCommand(
        state: CommandTriggerState,
        chapterUrl: String?,
        progress: Double,
    ): CommandTriggerResult {
        val nextState = state.copy(hasObservedInitialValue = true)
        val command = if (state.hasObservedInitialValue) {
            reloadChapterCommand(
                chapterUrl = chapterUrl,
                progress = progress,
            )
        } else {
            null
        }

        return CommandTriggerResult(
            state = nextState,
            command = command,
        )
    }

    fun settingsChangedCommand(
        state: CommandTriggerState,
        settings: ReaderSettings,
    ): CommandTriggerResult {
        val nextState = state.copy(hasObservedInitialValue = true)
        val command = if (state.hasObservedInitialValue) {
            settingsCommand(settings)
        } else {
            null
        }

        return CommandTriggerResult(
            state = nextState,
            command = command,
        )
    }

    fun collapseConsecutiveLoads(
        commands: Iterable<NovelReaderWebCommand>,
    ): List<NovelReaderWebCommand> {
        return collapseConsecutiveLoadCommands(
            commands = commands,
            isLoadCommand = { it is NovelReaderWebCommand.LoadChapter },
        )
    }

    fun <T : Any> collapseConsecutiveLoadCommands(
        commands: Iterable<T>,
        isLoadCommand: (T) -> Boolean,
    ): List<T> {
        val collapsed = mutableListOf<T>()
        var pendingLoadCommand: T? = null

        commands.forEach { command ->
            if (isLoadCommand(command)) {
                pendingLoadCommand = command
            } else {
                pendingLoadCommand?.let(collapsed::add)
                pendingLoadCommand = null
                collapsed += command
            }
        }

        pendingLoadCommand?.let(collapsed::add)
        return collapsed
    }
}
