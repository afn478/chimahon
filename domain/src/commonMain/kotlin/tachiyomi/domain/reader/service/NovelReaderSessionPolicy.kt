package tachiyomi.domain.reader.service

import tachiyomi.domain.library.model.NovelBookMetadata
import tachiyomi.domain.reader.model.NovelEpubSpineItemType
import tachiyomi.domain.reader.model.NovelReaderBookmark

object NovelReaderSessionPolicy {
    const val DEFAULT_STATISTICS_TITLE = "Unknown"
    const val BOOKMARK_PROGRESS_EPSILON = 0.0001

    sealed interface BookOpenAction {
        data object Ignore : BookOpenAction
        data class PersistLastAccess(val metadata: NovelBookMetadata) : BookOpenAction
    }

    data class RestoredBookmarkState(
        val chapterIndex: Int,
        val progress: Double,
        val characterCount: Int,
    )

    data class TrackingLockTransition(
        val updateBeforeLock: Boolean,
        val resetBaselineAfterUnlock: Boolean,
        val locked: Boolean,
    )

    data class ChapterChangeState(
        val index: Int,
        val progress: Double,
        val baselineCharacterCount: Int,
        val updateStatisticsBeforeChange: Boolean,
    )

    fun bookOpenAction(
        book: NovelBookMetadata,
        rootAvailable: Boolean,
        nowMillis: Long,
    ): BookOpenAction {
        return if (rootAvailable) {
            BookOpenAction.PersistLastAccess(book.copy(lastAccess = nowMillis))
        } else {
            BookOpenAction.Ignore
        }
    }

    fun restoredBookmarkState(
        bookmark: NovelReaderBookmark?,
        chapterCharacterCount: (Int) -> Int,
    ): RestoredBookmarkState {
        val chapterIndex = bookmark?.chapterIndex ?: 0
        val progress = bookmark?.progress ?: 0.0
        return RestoredBookmarkState(
            chapterIndex = chapterIndex,
            progress = progress,
            characterCount = NovelReaderProgressPolicy.exploredCharacterCount(
                chapterIndex = chapterIndex,
                progress = progress,
                chapterCharacterCount = chapterCharacterCount,
            ),
        )
    }

    fun statisticsTitle(documentTitle: String?): String {
        return documentTitle ?: DEFAULT_STATISTICS_TITLE
    }

    fun isImageOnlySpineItem(type: NovelEpubSpineItemType?): Boolean {
        return type == NovelEpubSpineItemType.IMAGE_ONLY
    }

    fun shouldUpdateStatistics(
        updateTracker: Boolean,
        trackingLocked: Boolean,
        appBackgrounded: Boolean,
    ): Boolean {
        return updateTracker && shouldTickStatistics(
            trackingLocked = trackingLocked,
            appBackgrounded = appBackgrounded,
        )
    }

    fun shouldTickStatistics(
        trackingLocked: Boolean,
        appBackgrounded: Boolean,
    ): Boolean {
        return !trackingLocked && !appBackgrounded
    }

    fun trackingLockTransition(
        locked: Boolean,
        currentlyTracking: Boolean,
    ): TrackingLockTransition {
        return TrackingLockTransition(
            updateBeforeLock = locked && currentlyTracking,
            resetBaselineAfterUnlock = !locked,
            locked = locked,
        )
    }

    fun chapterChangeState(
        newIndex: Int,
        progress: Double,
        baselineCharacterCount: Int,
        trackingLocked: Boolean,
        appBackgrounded: Boolean,
    ): ChapterChangeState {
        return ChapterChangeState(
            index = newIndex,
            progress = progress,
            baselineCharacterCount = baselineCharacterCount,
            updateStatisticsBeforeChange = shouldTickStatistics(
                trackingLocked = trackingLocked,
                appBackgrounded = appBackgrounded,
            ),
        )
    }
}
