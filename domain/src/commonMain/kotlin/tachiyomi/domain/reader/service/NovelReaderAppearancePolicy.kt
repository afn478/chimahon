package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.model.ReaderThemeColors

object NovelReaderAppearancePolicy {
    fun resolveThemeColors(
        theme: NovelReaderTheme,
        systemDark: Boolean,
        systemLightSepia: Boolean,
        customBackgroundColor: Int,
        customTextColor: Int,
    ): ReaderThemeColors {
        return when (theme) {
            NovelReaderTheme.LIGHT -> lightColors
            NovelReaderTheme.DARK -> darkColors
            NovelReaderTheme.SEPIA -> sepiaColors
            NovelReaderTheme.PURE_BLACK -> pureBlackColors
            NovelReaderTheme.CUSTOM -> ReaderThemeColors(
                backgroundColor = customBackgroundColor,
                textColor = customTextColor,
            )
            NovelReaderTheme.SYSTEM -> {
                when {
                    systemDark && systemLightSepia -> invertedSepiaColors
                    systemDark -> darkColors
                    systemLightSepia -> sepiaColors
                    else -> lightColors
                }
            }
        }
    }

    private val lightColors = ReaderThemeColors(
        backgroundColor = 0xFFFFFFFF.toInt(),
        textColor = 0xFF000000.toInt(),
    )
    private val darkColors = ReaderThemeColors(
        backgroundColor = 0xFF121212.toInt(),
        textColor = 0xFFE0E0E0.toInt(),
    )
    private val sepiaColors = ReaderThemeColors(
        backgroundColor = 0xFFF2E2C9.toInt(),
        textColor = 0xFF3C2C1C.toInt(),
    )
    private val invertedSepiaColors = ReaderThemeColors(
        backgroundColor = 0xFF1C140C.toInt(),
        textColor = 0xFFF2E2C9.toInt(),
    )
    private val pureBlackColors = ReaderThemeColors(
        backgroundColor = 0xFF000000.toInt(),
        textColor = 0xFFE0E0E0.toInt(),
    )
}
