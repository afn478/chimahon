package tachiyomi.domain.reader.model

data class NovelReaderSettingsSnapshot(
    val theme: NovelReaderTheme,
    val fontSize: Double,
    val lineHeight: Double,
    val characterSpacing: Double,
    val paragraphSpacing: Double,
    val horizontalPadding: Double,
    val verticalPadding: Double,
    val selectedFont: String,
    val customBackgroundColor: Int,
    val customTextColor: Int,
    val verticalWriting: Boolean,
    val justifyText: Boolean,
    val avoidPageBreak: Boolean,
    val hideFurigana: Boolean,
    val layoutAdvanced: Boolean,
    val tapZonePercent: Int,
    val continuousMode: Boolean,
    val systemLightSepia: Boolean,
)
