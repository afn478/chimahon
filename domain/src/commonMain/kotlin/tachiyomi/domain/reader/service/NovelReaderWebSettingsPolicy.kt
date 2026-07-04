package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderWebSettingsPolicy {
    sealed interface SettingsCommandAction {
        data object ReinjectReader : SettingsCommandAction
        data object ApplyLiveSettings : SettingsCommandAction
    }

    sealed interface LiveSettingsAction {
        data object ReloadCurrentChapter : LiveSettingsAction
        data object ApplyDomUpdates : LiveSettingsAction
    }

    sealed interface ApplySettingsAction {
        data class CaptureProgressThenReload(
            val url: String,
            val continuousMode: Boolean,
            val progressScript: String,
        ) : ApplySettingsAction

        data class SetContinuousMode(val continuousMode: Boolean) : ApplySettingsAction

        data class ApplyDomUpdates(
            val settings: ReaderSettings,
            val backgroundColor: Int,
            val script: String,
        ) : ApplySettingsAction
    }

    fun settingsCommandAction(
        oldSettings: ReaderSettings,
        newSettings: ReaderSettings,
    ): SettingsCommandAction {
        return if (
            oldSettings.avoidPageBreak != newSettings.avoidPageBreak ||
            oldSettings.verticalWriting != newSettings.verticalWriting
        ) {
            SettingsCommandAction.ReinjectReader
        } else {
            SettingsCommandAction.ApplyLiveSettings
        }
    }

    fun liveSettingsAction(
        currentContinuousMode: Boolean,
        nextSettings: ReaderSettings,
    ): LiveSettingsAction {
        return if (nextSettings.continuousMode != currentContinuousMode) {
            LiveSettingsAction.ReloadCurrentChapter
        } else {
            LiveSettingsAction.ApplyDomUpdates
        }
    }

    fun applySettingsAction(
        currentContinuousMode: Boolean,
        nextSettings: ReaderSettings,
        currentUrl: String?,
    ): ApplySettingsAction {
        return when (liveSettingsAction(currentContinuousMode, nextSettings)) {
            LiveSettingsAction.ReloadCurrentChapter -> {
                currentUrl?.let { url ->
                    ApplySettingsAction.CaptureProgressThenReload(
                        url = url,
                        continuousMode = nextSettings.continuousMode,
                        progressScript = NovelReaderWebScriptPolicy.calculateProgressScript(),
                    )
                } ?: ApplySettingsAction.SetContinuousMode(nextSettings.continuousMode)
            }
            LiveSettingsAction.ApplyDomUpdates -> {
                ApplySettingsAction.ApplyDomUpdates(
                    settings = nextSettings,
                    backgroundColor = nextSettings.backgroundColor,
                    script = NovelReaderWebInjectionPolicy.liveSettingsScript(nextSettings),
                )
            }
        }
    }

    fun reloadProgress(result: String?): Double {
        return NovelReaderWebResultPolicy.progressResult(result) ?: 0.0
    }
}
