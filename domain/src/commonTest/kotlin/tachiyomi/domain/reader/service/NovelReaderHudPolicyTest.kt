package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderHudPolicyTest {
    @Test
    fun bottomBarStateFormatsProgressAndActions() {
        assertEquals(
            NovelReaderHudPolicy.BottomBarState(
                progressText = "42%",
                chaptersContentDescription = "Chapters",
                appearanceContentDescription = "Appearance",
                statisticsContentDescription = "Statistics",
            ),
            NovelReaderHudPolicy.bottomBarState(progress = 0.427),
        )
    }
}
