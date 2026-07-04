package tachiyomi.domain.reader.model

data class ReaderSettings(
    val fontSize: Double = 18.0,
    val lineHeight: Double = 1.6,
    val characterSpacing: Double = 0.0,
    val paragraphSpacing: Double = 0.0,
    val horizontalPadding: Double = 10.0,
    val verticalPadding: Double = 10.0,
    val selectedFont: String = "System Serif",
    val fontUrl: String? = null,
    val theme: String = "system",
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val textColor: Int = 0xFF000000.toInt(),
    val verticalWriting: Boolean = true,
    val justifyText: Boolean = false,
    val avoidPageBreak: Boolean = true,
    val hideFurigana: Boolean = false,
    val layoutAdvanced: Boolean = false,
    val tapZonePercent: Int = 20,
    val continuousMode: Boolean = false,
)
