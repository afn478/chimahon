package com.canopus.chimareader.ui.reader

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.canopus.chimareader.data.BookStorage
import com.canopus.chimareader.data.FileNames
import com.canopus.chimareader.data.SasayakiMatch
import com.canopus.chimareader.data.SasayakiMatchData
import com.canopus.chimareader.data.SasayakiPlaybackData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import tachiyomi.domain.reader.service.NovelReaderSasayakiCueTimeline
import tachiyomi.domain.reader.service.NovelReaderSasayakiPlaybackPolicy

class SasayakiPlayer(
    private val context: Context,
    private val rootDir: File,
    private val bridge: WebViewBridge,
    private val loadChapter: (Int) -> Unit,
    private val getCurrentIndex: () -> Int,
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var player: ExoPlayer? = null

    var matchData: SasayakiMatchData? = null
        private set
    var timeline = NovelReaderSasayakiCueTimeline(null)
        private set

    var playback = SasayakiPlaybackData(lastPosition = 0.0)
    var isPlaying = false
    var duration: Double = 0.0
    var currentTime: Double = 0.0

    var delay: Double = 0.0
        set(value) {
            field = value
            savePlayback()
            updateCue(currentTime)
        }

    var currentCue: SasayakiMatch? = null
    var pendingCue: SasayakiMatch? = null
    var chapterTransition = false
    private var shouldResume = false
    private var stopPlaybackTime: Double? = null

    var hasPlayedOnce = false
    var autoScroll = true // Mapped from Settings ideally

    private var progressTrackerJob: Job? = null

    val hasAudio: Boolean get() = player != null
    val hasMatch: Boolean get() = matchData != null

    init {
        matchData = BookStorage.loadSasayakiMatchData(rootDir)
        if (matchData != null) {
            timeline = NovelReaderSasayakiCueTimeline(matchData)
            playback = BookStorage.loadSasayakiPlaybackData(rootDir) ?: SasayakiPlaybackData(lastPosition = 0.0)
            currentTime = playback.lastPosition
            delay = playback.delay
            restoreAudioIfNeeded()
        }
    }

    private fun restoreAudioIfNeeded() {
        val audioBookmark = playback.audioBookmark
        val audioExists = audioBookmark
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it).exists() }
            ?: false

        when (
            val action = NovelReaderSasayakiPlaybackPolicy.audioRestoreAction(
                audioBookmark = audioBookmark,
                audioExists = audioExists,
            )
        ) {
            NovelReaderSasayakiPlaybackPolicy.AudioRestoreAction.Ignore -> Unit
            is NovelReaderSasayakiPlaybackPolicy.AudioRestoreAction.Restore -> {
                setupPlayer(File(action.audioPath))
            }
        }
    }

    fun importAudio(file: File) {
        when (
            val action = NovelReaderSasayakiPlaybackPolicy.audioImportAction(file.absolutePath)
        ) {
            NovelReaderSasayakiPlaybackPolicy.AudioImportAction.Ignore -> Unit
            is NovelReaderSasayakiPlaybackPolicy.AudioImportAction.SaveAndLoad -> {
                teardown()
                playback.audioBookmark = action.audioPath
                savePlayback()
                setupPlayer(File(action.audioPath))
            }
        }
    }

    private fun setupPlayer(file: File) {
        player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            setMediaItem(mediaItem)
            prepare()
            seekTo((currentTime * 1000).toLong())

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    this@SasayakiPlayer.isPlaying = isPlayingNow
                    if (isPlayingNow) {
                        hasPlayedOnce = true
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                        savePlayback()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        this@SasayakiPlayer.isPlaying = false
                        stopPlaybackTime = null
                    }
                }
            })
        }
    }

    fun togglePlayback() {
        val exoPlayer = player
        when (
            NovelReaderSasayakiPlaybackPolicy.playbackToggleAction(
                hasAudio = exoPlayer != null,
                isPlaying = exoPlayer?.isPlaying == true,
            )
        ) {
            NovelReaderSasayakiPlaybackPolicy.PlaybackToggleAction.Ignore -> Unit
            NovelReaderSasayakiPlaybackPolicy.PlaybackToggleAction.Pause -> exoPlayer?.pause()
            NovelReaderSasayakiPlaybackPolicy.PlaybackToggleAction.Play -> exoPlayer?.play()
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            while (true) {
                player?.let { p ->
                    val sec = p.currentPosition / 1000.0
                    tick(sec)
                }
                delay(100) // Poll every 100ms
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
    }

    private fun tick(seconds: Double) {
        currentTime = seconds
        player?.let {
            if (it.duration > 0) duration = it.duration / 1000.0
        }

        stopPlaybackTime?.let { stopTime ->
            if (seconds >= stopTime) {
                player?.pause()
                stopPlaybackTime = null
            }
        }

        playback.lastPosition = seconds
        updateCue(seconds)
    }

    private fun updateCue(time: Double) {
        val hasAudioNow = hasAudio
        val hasMatchNow = hasMatch
        val chapterTransitionNow = chapterTransition
        val canUpdateCue = hasAudioNow && hasMatchNow && !chapterTransitionNow
        val cue = if (canUpdateCue) {
            timeline.cueAt(time - delay)
        } else {
            null
        }
        val currentCueId = currentCue?.id
        val shouldReadCurrentIndex = cue != null && cue.id != currentCueId
        val action = NovelReaderSasayakiPlaybackPolicy.cueUpdateAction(
            hasAudio = hasAudioNow,
            hasMatch = hasMatchNow,
            chapterTransition = chapterTransitionNow,
            cue = cue,
            currentCueId = currentCueId,
            currentChapterIndex = if (shouldReadCurrentIndex) getCurrentIndex() else -1,
            autoScroll = autoScroll,
            hasPlayedOnce = hasPlayedOnce,
        )

        when (action) {
            NovelReaderSasayakiPlaybackPolicy.CueUpdateAction.Ignore -> Unit
            NovelReaderSasayakiPlaybackPolicy.CueUpdateAction.ClearDisplayedCue -> clearDisplayedCue()
            is NovelReaderSasayakiPlaybackPolicy.CueUpdateAction.DisplayCue -> {
                displayCue(action.cue, reveal = action.reveal)
            }
            is NovelReaderSasayakiPlaybackPolicy.CueUpdateAction.LoadChapterForCue -> {
                currentCue = action.cue
                pendingCue = action.cue
                loadChapter(action.chapterIndex)
            }
        }
    }

    fun handleRestoreCompleted(currentIndex: Int) {
        val timelineCue = if (hasMatch && chapterTransition) {
            timeline.cueAt(currentTime - delay)
        } else {
            null
        }
        when (
            val action = NovelReaderSasayakiPlaybackPolicy.restoreCompletedAction(
                hasMatch = hasMatch,
                chapterTransition = chapterTransition,
                currentChapterIndex = currentIndex,
                pendingCue = pendingCue,
                timelineCue = timelineCue,
                shouldResume = shouldResume,
                autoScroll = autoScroll,
                hasPlayedOnce = hasPlayedOnce,
            )
        ) {
            NovelReaderSasayakiPlaybackPolicy.RestoreCompletedAction.Ignore -> Unit
            is NovelReaderSasayakiPlaybackPolicy.RestoreCompletedAction.Complete -> {
                chapterTransition = false
                shouldResume = false
                pendingCue = null

                val cue = action.cue
                if (cue != null) {
                    displayCue(cue, reveal = action.reveal)
                } else {
                    clearDisplayedCue()
                }

                if (action.resume) {
                    player?.play()
                }
            }
        }
    }

    fun prepareTransition() {
        shouldResume = player?.isPlaying == true
        chapterTransition = true
        stopPlaybackTime = null
        clearDisplayedCue()
        player?.pause()
    }

    private fun displayCue(cue: SasayakiMatch, reveal: Boolean) {
        currentCue = cue
        bridge.highlightSasayakiCue(cue.id, reveal)
    }

    private fun clearDisplayedCue() {
        if (currentCue == null) return
        currentCue = null
        bridge.clearSasayakiCue()
    }

    private fun savePlayback() {
        BookStorage.save(playback, rootDir, FileNames.sasayakiPlayback)
    }

    fun teardown() {
        stopProgressTracker()
        player?.release()
        player = null
        isPlaying = false
        stopPlaybackTime = null
    }

    fun nextCue() {
        val lookupTime = NovelReaderSasayakiPlaybackPolicy.cueNavigationLookupTime(
            currentCueStartTime = currentCue?.startTime,
            currentPlaybackTime = currentTime,
            delay = delay,
            clampToZero = false,
        )
        when (
            val action = NovelReaderSasayakiPlaybackPolicy.nextCueSeekAction(
                nextCueStartTime = timeline.nextCue(lookupTime),
                delay = delay,
            )
        ) {
            NovelReaderSasayakiPlaybackPolicy.CueNavigationAction.Ignore -> Unit
            is NovelReaderSasayakiPlaybackPolicy.CueNavigationAction.Seek -> seek(action.seconds)
        }
    }

    fun prevCue() {
        val lookupTime = NovelReaderSasayakiPlaybackPolicy.cueNavigationLookupTime(
            currentCueStartTime = currentCue?.startTime,
            currentPlaybackTime = currentTime,
            delay = delay,
            clampToZero = true,
        )
        when (
            val action = NovelReaderSasayakiPlaybackPolicy.previousCueSeekAction(
                previousCueStartTime = timeline.prevCue(lookupTime),
                delay = delay,
            )
        ) {
            NovelReaderSasayakiPlaybackPolicy.CueNavigationAction.Ignore -> Unit
            is NovelReaderSasayakiPlaybackPolicy.CueNavigationAction.Seek -> seek(action.seconds)
        }
    }

    private fun seek(seconds: Double) {
        player?.seekTo((seconds * 1000).toLong())
        tick(seconds)
    }
}
