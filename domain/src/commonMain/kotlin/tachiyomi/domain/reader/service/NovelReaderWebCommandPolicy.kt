package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderWebCommand
import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderWebCommandPolicy {
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
