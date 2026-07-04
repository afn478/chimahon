package tachiyomi.domain.manga.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import uy.kohesive.injekt.injectLazy
import java.io.Serializable
import java.time.Instant

@Immutable
data class Manga(
    val id: Long,
    val source: Long,
    val favorite: Boolean,
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val dateAdded: Long,
    val viewerFlags: Long,
    val chapterFlags: Long,
    val coverLastModified: Long,
    val url: String,
    // SY -->
    val ogTitle: String,
    val ogArtist: String?,
    val ogAuthor: String?,
    val ogThumbnailUrl: String?,
    val ogDescription: String?,
    val ogGenre: List<String>?,
    val ogStatus: Long,
    // SY <--
    val updateStrategy: UpdateStrategy,
    val initialized: Boolean,
    val lastModifiedAt: Long,
    val favoriteModifiedAt: Long?,
    val version: Long,
    val notes: String,
) : Serializable {

    // SY -->
    private val customMangaInfo = if (favorite) {
        getCustomMangaInfo.get(id)
    } else {
        null
    }

    val title: String
        get() = customMangaInfo?.title ?: ogTitle

    val author: String?
        get() = customMangaInfo?.author ?: ogAuthor

    val artist: String?
        get() = customMangaInfo?.artist ?: ogArtist

    val thumbnailUrl: String?
        get() = customMangaInfo?.thumbnailUrl ?: ogThumbnailUrl

    val description: String?
        get() = customMangaInfo?.description ?: ogDescription

    val genre: List<String>?
        get() = customMangaInfo?.genre ?: ogGenre

    val status: Long
        get() = customMangaInfo?.status ?: ogStatus
    // SY <--

    val expectedNextUpdate: Instant?
        get() = nextUpdate
            /* KMK -->
            Always predict release date even for Completed entries
            .takeIf { status != SManga.COMPLETED.toLong() }?
             KMK <-- */
            .let { Instant.ofEpochMilli(it) }

    val sorting: Long
        get() = MangaChapterFlags.sorting(chapterFlags)

    val displayMode: Long
        get() = MangaChapterFlags.displayMode(chapterFlags)

    val unreadFilterRaw: Long
        get() = MangaChapterFlags.unreadFilterRaw(chapterFlags)

    val downloadedFilterRaw: Long
        get() = MangaChapterFlags.downloadedFilterRaw(chapterFlags)

    val bookmarkedFilterRaw: Long
        get() = MangaChapterFlags.bookmarkedFilterRaw(chapterFlags)

    val unreadFilter: TriState
        get() = when (unreadFilterRaw) {
            CHAPTER_SHOW_UNREAD -> TriState.ENABLED_IS
            CHAPTER_SHOW_READ -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

    val bookmarkedFilter: TriState
        get() = when (bookmarkedFilterRaw) {
            CHAPTER_SHOW_BOOKMARKED -> TriState.ENABLED_IS
            CHAPTER_SHOW_NOT_BOOKMARKED -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

    fun sortDescending(): Boolean {
        return MangaChapterFlags.sortDescending(chapterFlags)
    }

    companion object {
        const val SHOW_ALL = MangaChapterFlags.SHOW_ALL

        const val CHAPTER_SORT_DESC = MangaChapterFlags.CHAPTER_SORT_DESC
        const val CHAPTER_SORT_ASC = MangaChapterFlags.CHAPTER_SORT_ASC
        const val CHAPTER_SORT_DIR_MASK = MangaChapterFlags.CHAPTER_SORT_DIR_MASK

        const val CHAPTER_SHOW_UNREAD = MangaChapterFlags.CHAPTER_SHOW_UNREAD
        const val CHAPTER_SHOW_READ = MangaChapterFlags.CHAPTER_SHOW_READ
        const val CHAPTER_UNREAD_MASK = MangaChapterFlags.CHAPTER_UNREAD_MASK

        const val CHAPTER_SHOW_DOWNLOADED = MangaChapterFlags.CHAPTER_SHOW_DOWNLOADED
        const val CHAPTER_SHOW_NOT_DOWNLOADED = MangaChapterFlags.CHAPTER_SHOW_NOT_DOWNLOADED
        const val CHAPTER_DOWNLOADED_MASK = MangaChapterFlags.CHAPTER_DOWNLOADED_MASK

        const val CHAPTER_SHOW_BOOKMARKED = MangaChapterFlags.CHAPTER_SHOW_BOOKMARKED
        const val CHAPTER_SHOW_NOT_BOOKMARKED = MangaChapterFlags.CHAPTER_SHOW_NOT_BOOKMARKED
        const val CHAPTER_BOOKMARKED_MASK = MangaChapterFlags.CHAPTER_BOOKMARKED_MASK

        const val CHAPTER_SORTING_SOURCE = MangaChapterFlags.CHAPTER_SORTING_SOURCE
        const val CHAPTER_SORTING_NUMBER = MangaChapterFlags.CHAPTER_SORTING_NUMBER
        const val CHAPTER_SORTING_UPLOAD_DATE = MangaChapterFlags.CHAPTER_SORTING_UPLOAD_DATE
        const val CHAPTER_SORTING_ALPHABET = MangaChapterFlags.CHAPTER_SORTING_ALPHABET
        const val CHAPTER_SORTING_MASK = MangaChapterFlags.CHAPTER_SORTING_MASK

        const val CHAPTER_DISPLAY_NAME = MangaChapterFlags.CHAPTER_DISPLAY_NAME
        const val CHAPTER_DISPLAY_NUMBER = MangaChapterFlags.CHAPTER_DISPLAY_NUMBER
        const val CHAPTER_DISPLAY_MASK = MangaChapterFlags.CHAPTER_DISPLAY_MASK

        fun create() = Manga(
            id = -1L,
            url = "",
            // Sy -->
            ogTitle = "",
            // SY <--
            source = -1L,
            favorite = false,
            lastUpdate = 0L,
            nextUpdate = 0L,
            fetchInterval = 0,
            dateAdded = 0L,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            // SY -->
            ogArtist = null,
            ogAuthor = null,
            ogThumbnailUrl = null,
            ogDescription = null,
            ogGenre = null,
            ogStatus = 0L,
            // SY <--
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
            lastModifiedAt = 0L,
            favoriteModifiedAt = null,
            version = 0L,
            notes = "",
        )

        // SY -->
        private val getCustomMangaInfo: GetCustomMangaInfo by injectLazy()
        // SY <--
    }
}
