package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.CustomReaderTheme
import tachiyomi.domain.reader.model.NovelReaderSettingsSnapshot
import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderSettingsPolicy {
    fun preferenceName(name: String, namespace: String?): String {
        return if (namespace.isNullOrEmpty()) name else "${namespace}_$name"
    }

    fun encodeCustomThemes(themes: List<CustomReaderTheme>): String {
        return themes.joinToString("|") { "${it.name}~${it.backgroundColor},${it.textColor}" }
    }

    fun decodeCustomThemes(value: String?): List<CustomReaderTheme> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|")
            .mapNotNull { encoded ->
                val parts = encoded.split("~", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val colorParts = parts[1].split(",")
                val backgroundColor = colorParts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val textColor = colorParts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                CustomReaderTheme(name = parts[0], backgroundColor = backgroundColor, textColor = textColor)
            }
            .distinct()
    }

    fun addCustomTheme(
        themes: List<CustomReaderTheme>,
        theme: CustomReaderTheme,
        maxThemes: Int,
    ): List<CustomReaderTheme> {
        return (themes + theme).distinct().takeLast(maxThemes)
    }

    fun deleteCustomTheme(
        themes: List<CustomReaderTheme>,
        theme: CustomReaderTheme,
    ): List<CustomReaderTheme> {
        return themes.filter { it != theme }
    }

    fun renameCustomTheme(
        themes: List<CustomReaderTheme>,
        theme: CustomReaderTheme,
        newName: String,
    ): List<CustomReaderTheme> {
        return themes.map {
            if (it == theme) it.copy(name = newName) else it
        }
    }

    fun buildReaderSettings(
        snapshot: NovelReaderSettingsSnapshot,
        systemDark: Boolean,
        fontUrl: String?,
    ): ReaderSettings {
        val colors = NovelReaderAppearancePolicy.resolveThemeColors(
            theme = snapshot.theme,
            systemDark = systemDark,
            systemLightSepia = snapshot.systemLightSepia,
            customBackgroundColor = snapshot.customBackgroundColor,
            customTextColor = snapshot.customTextColor,
        )

        return ReaderSettings(
            fontSize = snapshot.fontSize,
            lineHeight = snapshot.lineHeight,
            characterSpacing = snapshot.characterSpacing,
            paragraphSpacing = snapshot.paragraphSpacing,
            horizontalPadding = snapshot.horizontalPadding,
            verticalPadding = snapshot.verticalPadding,
            selectedFont = snapshot.selectedFont,
            fontUrl = fontUrl,
            theme = snapshot.theme.name.lowercase(),
            backgroundColor = colors.backgroundColor,
            textColor = colors.textColor,
            verticalWriting = snapshot.verticalWriting,
            justifyText = snapshot.justifyText,
            avoidPageBreak = snapshot.avoidPageBreak,
            hideFurigana = snapshot.hideFurigana,
            layoutAdvanced = snapshot.layoutAdvanced,
            tapZonePercent = snapshot.tapZonePercent,
            continuousMode = snapshot.continuousMode,
        )
    }
}
