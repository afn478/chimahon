package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderWebCommand

object NovelReaderWebCommandPolicy {
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
