package tachiyomi.domain.reader.service

import kotlin.math.pow
import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.model.ReaderThemeColors

object NovelReaderAppearancePolicy {
    private const val LIGHT_SYSTEM_BAR_LUMINANCE_THRESHOLD = 0.5

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

    fun shouldUseDarkSystemBarIcons(backgroundColor: Int): Boolean {
        return relativeLuminance(backgroundColor) > LIGHT_SYSTEM_BAR_LUMINANCE_THRESHOLD
    }

    fun relativeLuminance(argbColor: Int): Double {
        val red = srgbToLinear((argbColor ushr 16) and 0xFF)
        val green = srgbToLinear((argbColor ushr 8) and 0xFF)
        val blue = srgbToLinear(argbColor and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun srgbToLinear(channel: Int): Double {
        val component = channel / 255.0
        return if (component <= 0.04045) {
            component / 12.92
        } else {
            ((component + 0.055) / 1.055).pow(2.4)
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
