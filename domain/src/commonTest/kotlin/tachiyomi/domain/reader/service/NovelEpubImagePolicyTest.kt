package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelEpubImagePolicyTest {
    @Test
    fun thresholdsRemainStableForImporterCompatibility() {
        assertEquals(2048, NovelEpubImagePolicy.MAX_DIMENSION)
        assertEquals(4_000_000L, NovelEpubImagePolicy.MAX_PIXELS)
    }

    @Test
    fun shouldDownsampleRejectsInvalidOrSmallDimensions() {
        assertFalse(NovelEpubImagePolicy.shouldDownsample(width = 0, height = 1200))
        assertFalse(NovelEpubImagePolicy.shouldDownsample(width = 1200, height = 0))
        assertFalse(NovelEpubImagePolicy.shouldDownsample(width = 1024, height = 2048))
        assertFalse(NovelEpubImagePolicy.shouldDownsample(width = 2000, height = 2000))
    }

    @Test
    fun shouldDownsampleMatchesDimensionAndPixelLimits() {
        assertTrue(NovelEpubImagePolicy.shouldDownsample(width = 2049, height = 1000))
        assertTrue(NovelEpubImagePolicy.shouldDownsample(width = 1000, height = 2049))
        assertTrue(NovelEpubImagePolicy.shouldDownsample(width = 2001, height = 2000))
    }

    @Test
    fun sampleSizeUsesSmallestPowerOfTwoThatFits() {
        assertEquals(1, NovelEpubImagePolicy.sampleSize(width = 1024, height = 2048))
        assertEquals(2, NovelEpubImagePolicy.sampleSize(width = 4096, height = 1000))
        assertEquals(2, NovelEpubImagePolicy.sampleSize(width = 2200, height = 2200))
        assertEquals(4, NovelEpubImagePolicy.sampleSize(width = 5000, height = 5000))
    }

    @Test
    fun mimeMightHaveAlphaMatchesAlphaCapableImporterFormats() {
        assertTrue(NovelEpubImagePolicy.mimeMightHaveAlpha("image/png"))
        assertTrue(NovelEpubImagePolicy.mimeMightHaveAlpha("IMAGE/GIF"))
        assertTrue(NovelEpubImagePolicy.mimeMightHaveAlpha("image/webp"))
        assertFalse(NovelEpubImagePolicy.mimeMightHaveAlpha("image/jpeg"))
        assertFalse(NovelEpubImagePolicy.mimeMightHaveAlpha(null))
    }
}
