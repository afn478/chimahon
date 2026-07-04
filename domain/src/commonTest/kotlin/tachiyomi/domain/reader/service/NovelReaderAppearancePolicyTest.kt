package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.model.ReaderThemeColors

class NovelReaderAppearancePolicyTest {
    @Test
    fun resolveThemeColorsUsesFixedThemeColors() {
        assertEquals(
            colors(backgroundColor = 0xFFFFFFFF.toInt(), textColor = 0xFF000000.toInt()),
            resolve(NovelReaderTheme.LIGHT),
        )
        assertEquals(
            colors(backgroundColor = 0xFF121212.toInt(), textColor = 0xFFE0E0E0.toInt()),
            resolve(NovelReaderTheme.DARK),
        )
        assertEquals(
            colors(backgroundColor = 0xFFF2E2C9.toInt(), textColor = 0xFF3C2C1C.toInt()),
            resolve(NovelReaderTheme.SEPIA),
        )
        assertEquals(
            colors(backgroundColor = 0xFF000000.toInt(), textColor = 0xFFE0E0E0.toInt()),
            resolve(NovelReaderTheme.PURE_BLACK),
        )
    }

    @Test
    fun resolveThemeColorsUsesCustomColors() {
        val result = resolve(
            theme = NovelReaderTheme.CUSTOM,
            customBackgroundColor = 0xFF123456.toInt(),
            customTextColor = 0xFFABCDEF.toInt(),
        )

        assertEquals(
            colors(backgroundColor = 0xFF123456.toInt(), textColor = 0xFFABCDEF.toInt()),
            result,
        )
    }

    @Test
    fun resolveThemeColorsUsesSystemLightColors() {
        assertEquals(
            colors(backgroundColor = 0xFFFFFFFF.toInt(), textColor = 0xFF000000.toInt()),
            resolve(theme = NovelReaderTheme.SYSTEM, systemDark = false, systemLightSepia = false),
        )
        assertEquals(
            colors(backgroundColor = 0xFFF2E2C9.toInt(), textColor = 0xFF3C2C1C.toInt()),
            resolve(theme = NovelReaderTheme.SYSTEM, systemDark = false, systemLightSepia = true),
        )
    }

    @Test
    fun resolveThemeColorsUsesSystemDarkColors() {
        assertEquals(
            colors(backgroundColor = 0xFF121212.toInt(), textColor = 0xFFE0E0E0.toInt()),
            resolve(theme = NovelReaderTheme.SYSTEM, systemDark = true, systemLightSepia = false),
        )
        assertEquals(
            colors(backgroundColor = 0xFF1C140C.toInt(), textColor = 0xFFF2E2C9.toInt()),
            resolve(theme = NovelReaderTheme.SYSTEM, systemDark = true, systemLightSepia = true),
        )
    }

    @Test
    fun relativeLuminanceUsesRgbChannelsAndIgnoresAlpha() {
        assertEquals(0.0, NovelReaderAppearancePolicy.relativeLuminance(0xFF000000.toInt()))
        assertEquals(1.0, NovelReaderAppearancePolicy.relativeLuminance(0xFFFFFFFF.toInt()))
        assertEquals(
            NovelReaderAppearancePolicy.relativeLuminance(0x00FFFFFF),
            NovelReaderAppearancePolicy.relativeLuminance(0xFFFFFFFF.toInt()),
        )
    }

    @Test
    fun systemBarIconStyleUsesReaderBackgroundLuminance() {
        assertTrue(NovelReaderAppearancePolicy.shouldUseDarkSystemBarIcons(0xFFFFFFFF.toInt()))
        assertTrue(NovelReaderAppearancePolicy.shouldUseDarkSystemBarIcons(0xFFF2E2C9.toInt()))
        assertTrue(NovelReaderAppearancePolicy.shouldUseDarkSystemBarIcons(0xFFBCBCBC.toInt()))
        assertFalse(NovelReaderAppearancePolicy.shouldUseDarkSystemBarIcons(0xFFBBBBBB.toInt()))
        assertFalse(NovelReaderAppearancePolicy.shouldUseDarkSystemBarIcons(0xFF121212.toInt()))
        assertFalse(NovelReaderAppearancePolicy.shouldUseDarkSystemBarIcons(0xFF000000.toInt()))
    }

    @Test
    fun colorHexFormatsRgbChannelsWithLeadingHashAndIgnoresAlpha() {
        assertEquals("#000000", NovelReaderAppearancePolicy.colorHex(0xFF000000.toInt()))
        assertEquals("#ABCDEF", NovelReaderAppearancePolicy.colorHex(0x12ABCDEF))
        assertEquals("#001020", NovelReaderAppearancePolicy.colorHex(0xFF001020.toInt()))
    }

    @Test
    fun parseColorInputAcceptsHexForms() {
        assertEquals(0xFFAABBCC.toInt(), NovelReaderAppearancePolicy.parseColorInput("#abc"))
        assertEquals(0xFFABCDEF.toInt(), NovelReaderAppearancePolicy.parseColorInput("ABCDEF"))
        assertEquals(0x80112233.toInt(), NovelReaderAppearancePolicy.parseColorInput("#80112233"))
        assertEquals(0xFF010203.toInt(), NovelReaderAppearancePolicy.parseColorInput("  #010203  "))
    }

    @Test
    fun parseColorInputAcceptsRgbForms() {
        assertEquals(0xFF010203.toInt(), NovelReaderAppearancePolicy.parseColorInput("rgb(1, 2, 3)"))
        assertEquals(0xFF040506.toInt(), NovelReaderAppearancePolicy.parseColorInput("rgba(4, 5, 6, 0.4)"))
        assertEquals(0xFF0C2238.toInt(), NovelReaderAppearancePolicy.parseColorInput("12, 34, 56"))
    }

    @Test
    fun parseColorInputRejectsMalformedColors() {
        assertEquals(null, NovelReaderAppearancePolicy.parseColorInput(""))
        assertEquals(null, NovelReaderAppearancePolicy.parseColorInput("#12"))
        assertEquals(null, NovelReaderAppearancePolicy.parseColorInput("#xyz"))
        assertEquals(null, NovelReaderAppearancePolicy.parseColorInput("rgb(1, 2)"))
        assertEquals(null, NovelReaderAppearancePolicy.parseColorInput("rgb(1, 2, 300)"))
    }

    private fun resolve(
        theme: NovelReaderTheme,
        systemDark: Boolean = false,
        systemLightSepia: Boolean = false,
        customBackgroundColor: Int = 0,
        customTextColor: Int = 0,
    ): ReaderThemeColors {
        return NovelReaderAppearancePolicy.resolveThemeColors(
            theme = theme,
            systemDark = systemDark,
            systemLightSepia = systemLightSepia,
            customBackgroundColor = customBackgroundColor,
            customTextColor = customTextColor,
        )
    }

    private fun colors(
        backgroundColor: Int,
        textColor: Int,
    ): ReaderThemeColors {
        return ReaderThemeColors(
            backgroundColor = backgroundColor,
            textColor = textColor,
        )
    }
}
