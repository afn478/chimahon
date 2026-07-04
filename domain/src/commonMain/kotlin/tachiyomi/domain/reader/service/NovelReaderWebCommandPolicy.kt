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

    data class ChapterLoadCommand(
        val chapterUrl: String,
        val progress: Double,
        val chapterTitle: String?,
        val command: NovelReaderWebCommand.LoadChapter,
    )

    fun chapterLoadCommand(
        chapterUrl: String,
        progress: Double,
        chapterTitle: String?,
    ): ChapterLoadCommand {
        return ChapterLoadCommand(
            chapterUrl = chapterUrl,
            progress = progress,
            chapterTitle = chapterTitle,
            command = NovelReaderWebCommand.LoadChapter(
                url = chapterUrl,
                progress = progress,
            ),
        )
    }

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

    fun jumpToFragmentCommand(fragment: String?): NovelReaderWebCommand? {
        return fragment
            ?.takeIf(String::isNotEmpty)
            ?.let(NovelReaderWebCommand::JumpToFragment)
    }

    fun clearSelectionCommand(): NovelReaderWebCommand {
        return NovelReaderWebCommand.ClearSelection
    }

    fun selectionRectsCommand(
        charCount: Int,
        startOffset: Int = 0,
    ): NovelReaderWebCommand {
        return NovelReaderWebCommand.GetSelectionRects(
            charCount = charCount,
            startOffset = startOffset,
        )
    }

    fun highlightSasayakiCueCommand(
        cueId: String,
        reveal: Boolean,
        onProgress: ((Double) -> Unit)? = null,
    ): NovelReaderWebCommand {
        return NovelReaderWebCommand.HighlightSasayakiCue(
            cueId = cueId,
            reveal = reveal,
            onProgress = onProgress,
        )
    }

    fun clearSasayakiCueCommand(): NovelReaderWebCommand {
        return NovelReaderWebCommand.ClearSasayakiCue
    }

    fun paginateCommand(forward: Boolean): NovelReaderWebCommand {
        return NovelReaderWebCommand.Paginate(forward)
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
