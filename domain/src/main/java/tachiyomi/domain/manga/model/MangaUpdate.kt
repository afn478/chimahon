package tachiyomi.domain.manga.model

fun Manga.toMangaUpdate(): MangaUpdate {
    return MangaUpdate(
        id = id,
        source = source,
        favorite = favorite,
        lastUpdate = lastUpdate,
        nextUpdate = nextUpdate,
        fetchInterval = fetchInterval,
        dateAdded = dateAdded,
        viewerFlags = viewerFlags,
        chapterFlags = chapterFlags,
        coverLastModified = coverLastModified,
        url = url,
        // SY -->
        title = ogTitle,
        artist = ogArtist,
        author = ogAuthor,
        thumbnailUrl = ogThumbnailUrl,
        description = ogDescription,
        genre = ogGenre,
        status = ogStatus,
        // SY <--
        updateStrategy = updateStrategy,
        initialized = initialized,
        version = version,
        notes = notes,
    )
}
