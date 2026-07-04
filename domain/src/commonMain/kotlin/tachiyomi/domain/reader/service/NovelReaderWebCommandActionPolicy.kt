package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderWebCommand
import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderWebCommandActionPolicy {
    sealed interface CommandAction {
        data class LoadChapter(
            val url: String,
            val progress: Double,
        ) : CommandAction

        data class ReloadCurrentChapter(
            val url: String,
            val continuousMode: Boolean,
        ) : CommandAction

        data class ApplySettings(
            val settings: ReaderSettings,
            val settingsAction: NovelReaderWebSettingsPolicy.SettingsCommandAction,
        ) : CommandAction

        data class SetFocusMode(val focusMode: Boolean) : CommandAction
        data class Paginate(val forward: Boolean) : CommandAction
        data class EvaluateScript(val script: String) : CommandAction
        data class RequestSelectionRects(val script: String) : CommandAction
        data object Ignore : CommandAction
    }

    fun commandAction(
        command: NovelReaderWebCommand,
        currentUrl: String?,
        lastAppliedSettings: ReaderSettings,
    ): CommandAction {
        return when (command) {
            is NovelReaderWebCommand.LoadChapter -> CommandAction.LoadChapter(
                url = command.url,
                progress = command.progress,
            )
            is NovelReaderWebCommand.ChangeMode -> {
                currentUrl?.let { url ->
                    CommandAction.ReloadCurrentChapter(
                        url = url,
                        continuousMode = command.continuous,
                    )
                } ?: CommandAction.Ignore
            }
            is NovelReaderWebCommand.ApplySettings -> CommandAction.ApplySettings(
                settings = command.settings,
                settingsAction = NovelReaderWebSettingsPolicy.settingsCommandAction(
                    oldSettings = lastAppliedSettings,
                    newSettings = command.settings,
                ),
            )
            is NovelReaderWebCommand.ChangeFocusMode -> CommandAction.SetFocusMode(command.focusMode)
            is NovelReaderWebCommand.Paginate -> CommandAction.Paginate(command.forward)
            is NovelReaderWebCommand.JumpToFragment -> CommandAction.EvaluateScript(
                NovelReaderWebScriptPolicy.scrollToFragmentScript(command.fragment),
            )
            NovelReaderWebCommand.ClearSelection -> CommandAction.EvaluateScript(
                NovelReaderWebScriptPolicy.clearSelectionScript(),
            )
            is NovelReaderWebCommand.HighlightSelection -> CommandAction.EvaluateScript(
                NovelReaderWebScriptPolicy.highlightSelectionScript(command.charCount),
            )
            is NovelReaderWebCommand.GetSelectionRects -> CommandAction.RequestSelectionRects(
                NovelReaderWebScriptPolicy.selectionRectsScript(
                    charCount = command.charCount,
                    startOffset = command.startOffset,
                ),
            )
            is NovelReaderWebCommand.ApplySasayakiCues,
            is NovelReaderWebCommand.HighlightSasayakiCue,
            NovelReaderWebCommand.ClearSasayakiCue,
            is NovelReaderWebCommand.UpdateTextColor,
            -> CommandAction.Ignore
        }
    }

    fun commandActions(
        commands: Iterable<NovelReaderWebCommand>,
        currentUrl: String?,
        lastAppliedSettings: ReaderSettings,
    ): List<CommandAction> {
        var nextCurrentUrl = currentUrl
        var nextLastAppliedSettings = lastAppliedSettings

        return NovelReaderWebCommandPolicy.collapseConsecutiveLoads(commands).map { command ->
            commandAction(
                command = command,
                currentUrl = nextCurrentUrl,
                lastAppliedSettings = nextLastAppliedSettings,
            ).also { action ->
                when (action) {
                    is CommandAction.LoadChapter -> nextCurrentUrl = action.url
                    is CommandAction.ReloadCurrentChapter -> nextCurrentUrl = action.url
                    is CommandAction.ApplySettings -> nextLastAppliedSettings = action.settings
                    is CommandAction.SetFocusMode,
                    is CommandAction.Paginate,
                    is CommandAction.EvaluateScript,
                    is CommandAction.RequestSelectionRects,
                    CommandAction.Ignore,
                    -> Unit
                }
            }
        }
    }
}
