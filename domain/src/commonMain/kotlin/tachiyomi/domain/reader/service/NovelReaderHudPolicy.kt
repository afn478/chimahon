package tachiyomi.domain.reader.service

object NovelReaderHudPolicy {
    data class BottomBarState(
        val progressText: String,
        val chaptersContentDescription: String,
        val appearanceContentDescription: String,
        val statisticsContentDescription: String,
    )

    fun bottomBarState(progress: Double): BottomBarState {
        return BottomBarState(
            progressText = "${(progress * 100).toInt()}%",
            chaptersContentDescription = "Chapters",
            appearanceContentDescription = "Appearance",
            statisticsContentDescription = "Statistics",
        )
    }
}
