package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
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
