package tachiyomi.domain.reader.service

object NovelReaderFontPolicy {
    const val FONTS_DIRECTORY = "fonts"
    const val SYSTEM_SERIF = "System Serif"
    const val SYSTEM_SANS_SERIF = "System Sans-Serif"
    const val IMPORTED_FONT_PREFIX = "imported_font_"
    const val DEFAULT_IMPORTED_FONT_EXTENSION = "ttf"

    val defaultFonts = listOf(SYSTEM_SERIF, SYSTEM_SANS_SERIF)

    fun isDefaultFont(fontName: String): Boolean {
        return fontName in defaultFonts
    }

    fun isCustomFont(
        fontName: String,
        importedFontNames: Collection<String>,
    ): Boolean {
        return !isDefaultFont(fontName) && fontName in importedFontNames
    }

    fun importedFontFileName(importTimeMillis: Long): String {
        return "$IMPORTED_FONT_PREFIX$importTimeMillis.$DEFAULT_IMPORTED_FONT_EXTENSION"
    }

    fun fontDisplayName(fileName: String): String {
        return fileName.substringBeforeLast(".", missingDelimiterValue = fileName)
    }

    fun importedFontNames(fileNames: Iterable<String>): List<String> {
        return fileNames.map(::fontDisplayName)
    }

    fun customFontNames(fileNames: Iterable<String>): List<String> {
        return importedFontNames(fileNames).filterNot(::isDefaultFont)
    }

    fun matchesImportedFontFile(
        fileName: String,
        fontName: String,
    ): Boolean {
        return !isDefaultFont(fontName) && fontDisplayName(fileName) == fontName
    }

    fun fontFileUri(absolutePath: String): String {
        return NovelReaderFileUrlPolicy.fileUrlForAbsolutePath(absolutePath)
    }

    fun cssFontFamily(selectedFont: String): String {
        return when (selectedFont) {
            SYSTEM_SERIF -> "serif"
            SYSTEM_SANS_SERIF -> "sans-serif"
            else -> selectedFont
        }
    }
}
