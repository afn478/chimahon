package tachiyomi.domain.reader.service

import kotlin.math.pow
import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.model.ReaderThemeColors

object NovelReaderAppearancePolicy {
    private const val LIGHT_SYSTEM_BAR_LUMINANCE_THRESHOLD = 0.5
    private val rgbColorRegex = Regex("""rgba?\((.*)\)""", RegexOption.IGNORE_CASE)
    private val rgbChannelRegex = Regex("""\d{1,3}""")

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

    fun colorHex(color: Int): String {
        return "#" + (color and 0xFFFFFF)
            .toString(radix = 16)
            .uppercase()
            .padStart(length = 6, padChar = '0')
    }

    fun parseColorInput(value: String): Int? {
        val input = value.trim()
        if (input.isEmpty()) return null
        return parseHexColor(input) ?: parseRgbColor(input)
    }

    fun relativeLuminance(argbColor: Int): Double {
        val red = srgbToLinear((argbColor ushr 16) and 0xFF)
        val green = srgbToLinear((argbColor ushr 8) and 0xFF)
        val blue = srgbToLinear(argbColor and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun parseHexColor(input: String): Int? {
        val rawHex = input.removePrefix("#")
        val hex = when (rawHex.length) {
            3 -> rawHex.map { "$it$it" }.joinToString("")
            6, 8 -> rawHex
            else -> return null
        }
        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        val parsed = hex.toLongOrNull(radix = 16) ?: return null
        return when (hex.length) {
            6 -> (0xFF000000 or parsed).toInt()
            8 -> parsed.toInt()
            else -> null
        }
    }

    private fun parseRgbColor(input: String): Int? {
        val body = rgbColorRegex
            .matchEntire(input)
            ?.groupValues
            ?.get(1)
            ?: input
        val channels = rgbChannelRegex
            .findAll(body)
            .mapNotNull { it.value.toIntOrNull() }
            .take(3)
            .toList()
        if (channels.size != 3 || channels.any { it !in 0..255 }) return null
        val (red, green, blue) = channels
        return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
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
