package tachiyomi.domain.reader.model

sealed interface NovelReaderWebCommand {
    data class LoadChapter(val url: String, val progress: Double) : NovelReaderWebCommand
    data class JumpToFragment(val fragment: String) : NovelReaderWebCommand
    data class ApplySasayakiCues(val cuesJson: String) : NovelReaderWebCommand
    data class HighlightSasayakiCue(
        val cueId: String,
        val reveal: Boolean,
        val onProgress: ((Double) -> Unit)? = null,
    ) : NovelReaderWebCommand
    data object ClearSasayakiCue : NovelReaderWebCommand
    data class UpdateTextColor(val hex: String?) : NovelReaderWebCommand
    data class ChangeMode(val continuous: Boolean) : NovelReaderWebCommand
    data class ApplySettings(val settings: ReaderSettings) : NovelReaderWebCommand
    data class ChangeFocusMode(val focusMode: Boolean) : NovelReaderWebCommand
    data class Paginate(val forward: Boolean) : NovelReaderWebCommand
    data object ClearSelection : NovelReaderWebCommand
    data class HighlightSelection(val charCount: Int) : NovelReaderWebCommand
    data class GetSelectionRects(val charCount: Int, val startOffset: Int = 0) : NovelReaderWebCommand
}
