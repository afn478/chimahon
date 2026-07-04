package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import tachiyomi.domain.reader.model.CustomReaderTheme
import tachiyomi.domain.reader.model.NovelReaderSettingsSnapshot
import tachiyomi.domain.reader.model.NovelReaderTheme
import tachiyomi.domain.reader.model.ReaderSettings

class NovelReaderSettingsPolicyTest {
    @Test
    fun preferenceNameUsesPlainKeyWithoutNamespace() {
        assertEquals(
            "theme",
            NovelReaderSettingsPolicy.preferenceName(name = "theme", namespace = null),
        )
        assertEquals(
            "theme",
            NovelReaderSettingsPolicy.preferenceName(name = "theme", namespace = ""),
        )
    }

    @Test
    fun preferenceNamePrefixesNamespacedKeys() {
        val result = NovelReaderSettingsPolicy.preferenceName(
            name = "theme",
            namespace = "book_42",
        )

        assertEquals("book_42_theme", result)
    }

    @Test
    fun customThemesRoundTripThroughPreferenceString() {
        val themes = listOf(
            customTheme(name = "Sepia", backgroundColor = -864567, textColor = -16777216),
            customTheme(name = "Night", backgroundColor = -16777216, textColor = -1),
        )

        val encoded = NovelReaderSettingsPolicy.encodeCustomThemes(themes)
        val decoded = NovelReaderSettingsPolicy.decodeCustomThemes(encoded)

        assertEquals("Sepia~-864567,-16777216|Night~-16777216,-1", encoded)
        assertEquals(themes, decoded)
    }

    @Test
    fun decodeCustomThemesDropsMalformedEntriesAndDuplicates() {
        val result = NovelReaderSettingsPolicy.decodeCustomThemes(
            "Sepia~-864567,-16777216|missing-colors~12|bad-number~a,1|Sepia~-864567,-16777216",
        )

        assertEquals(
            listOf(customTheme(name = "Sepia", backgroundColor = -864567, textColor = -16777216)),
            result,
        )
    }

    @Test
    fun addCustomThemeDeduplicatesAndKeepsMostRecentThemesWithinLimit() {
        val existing = listOf(
            customTheme("one"),
            customTheme("two"),
            customTheme("three"),
        )

        val result = NovelReaderSettingsPolicy.addCustomTheme(
            themes = existing,
            theme = customTheme("four"),
            maxThemes = 3,
        )

        assertEquals(listOf("two", "three", "four"), result.map { it.name })
    }

    @Test
    fun addCustomThemeKeepsExistingDuplicatePosition() {
        val existing = listOf(
            customTheme("one"),
            customTheme("two"),
        )

        val result = NovelReaderSettingsPolicy.addCustomTheme(
            themes = existing,
            theme = customTheme("one"),
            maxThemes = 3,
        )

        assertEquals(listOf("one", "two"), result.map { it.name })
    }

    @Test
    fun deleteCustomThemeRemovesMatchingTheme() {
        val target = customTheme("two")
        val result = NovelReaderSettingsPolicy.deleteCustomTheme(
            themes = listOf(customTheme("one"), target, customTheme("three")),
            theme = target,
        )

        assertEquals(listOf("one", "three"), result.map { it.name })
    }

    @Test
    fun renameCustomThemeOnlyRenamesExactThemeMatch() {
        val target = customTheme("two", backgroundColor = 1, textColor = 2)
        val sameNameDifferentColors = customTheme("two", backgroundColor = 3, textColor = 4)

        val result = NovelReaderSettingsPolicy.renameCustomTheme(
            themes = listOf(target, sameNameDifferentColors),
            theme = target,
            newName = "renamed",
        )

        assertEquals(
            listOf("renamed", "two"),
            result.map { it.name },
        )
    }

    @Test
    fun buildReaderSettingsCombinesSnapshotAppearanceAndPlatformInputs() {
        val result = NovelReaderSettingsPolicy.buildReaderSettings(
            snapshot = settingsSnapshot(
                theme = NovelReaderTheme.SYSTEM,
                systemLightSepia = true,
                customBackgroundColor = 0xFF123456.toInt(),
                customTextColor = 0xFFABCDEF.toInt(),
            ),
            systemDark = true,
            fontUrl = "file:///fonts/mincho.ttf",
        )

        assertEquals(
            ReaderSettings(
                fontSize = 20.0,
                lineHeight = 1.8,
                characterSpacing = 0.05,
                paragraphSpacing = 0.25,
                horizontalPadding = 12.0,
                verticalPadding = 14.0,
                selectedFont = "Mincho",
                fontUrl = "file:///fonts/mincho.ttf",
                theme = "system",
                backgroundColor = 0xFF1C140C.toInt(),
                textColor = 0xFFF2E2C9.toInt(),
                verticalWriting = false,
                justifyText = true,
                avoidPageBreak = false,
                hideFurigana = true,
                layoutAdvanced = true,
                tapZonePercent = 24,
                continuousMode = true,
            ),
            result,
        )
    }

    @Test
    fun buildReaderSettingsUsesCustomThemeColors() {
        val result = NovelReaderSettingsPolicy.buildReaderSettings(
            snapshot = settingsSnapshot(
                theme = NovelReaderTheme.CUSTOM,
                customBackgroundColor = 0xFF123456.toInt(),
                customTextColor = 0xFFABCDEF.toInt(),
            ),
            systemDark = false,
            fontUrl = null,
        )

        assertEquals("custom", result.theme)
        assertEquals(0xFF123456.toInt(), result.backgroundColor)
        assertEquals(0xFFABCDEF.toInt(), result.textColor)
        assertEquals(null, result.fontUrl)
    }

    private fun customTheme(
        name: String,
        backgroundColor: Int = 1,
        textColor: Int = 2,
    ): CustomReaderTheme {
        return CustomReaderTheme(
            name = name,
            backgroundColor = backgroundColor,
            textColor = textColor,
        )
    }

    private fun settingsSnapshot(
        theme: NovelReaderTheme,
        systemLightSepia: Boolean = false,
        customBackgroundColor: Int = 0,
        customTextColor: Int = 0,
    ): NovelReaderSettingsSnapshot {
        return NovelReaderSettingsSnapshot(
            theme = theme,
            fontSize = 20.0,
            lineHeight = 1.8,
            characterSpacing = 0.05,
            paragraphSpacing = 0.25,
            horizontalPadding = 12.0,
            verticalPadding = 14.0,
            selectedFont = "Mincho",
            customBackgroundColor = customBackgroundColor,
            customTextColor = customTextColor,
            verticalWriting = false,
            justifyText = true,
            avoidPageBreak = false,
            hideFurigana = true,
            layoutAdvanced = true,
            tapZonePercent = 24,
            continuousMode = true,
            systemLightSepia = systemLightSepia,
        )
    }
}
