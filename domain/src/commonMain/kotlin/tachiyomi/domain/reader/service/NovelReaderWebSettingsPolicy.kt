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
}
