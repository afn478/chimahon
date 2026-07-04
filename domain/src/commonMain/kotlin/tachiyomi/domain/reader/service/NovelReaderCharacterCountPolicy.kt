package tachiyomi.domain.reader.service

object NovelReaderCharacterCountPolicy {
    private val bodyRegex = Regex("(?s)<body.*?</body>")
    private val rubyTextRegex = Regex("(?s)<rt>.*?</rt>")
    private val scriptOrStyleRegex = Regex("(?s)<(script|style)[^>]*>.*?</\\1>")
    private val tagRegex = Regex("<[^>]+>")

    fun countChapterCharacters(content: String?): Int {
        if (content == null) return 0

        val readableText = readableText(content)
        var count = 0
        readableText.forEachCodePoint { codePoint ->
            if (isCountedCharacter(codePoint)) {
                count += 1
            }
        }
        return count
    }

    private fun readableText(content: String): String {
        var text = bodyRegex.find(content)?.value ?: content

        text = text.replace(rubyTextRegex, "")
        text = text.replace(scriptOrStyleRegex, "")
        text = text.replace(tagRegex, "")
        text = text.replace("&nbsp;", " ")
        text = text.replace("&amp;", "&")
        text = text.replace("&lt;", "<")
        text = text.replace("&gt;", ">")

        return text
    }

    private inline fun String.forEachCodePoint(action: (Int) -> Unit) {
        var index = 0
        while (index < length) {
            val current = this[index]
            val next = getOrNull(index + 1)
            if (current.isHighSurrogate() && next?.isLowSurrogate() == true) {
                action(toCodePoint(current, next))
                index += 2
            } else {
                action(current.code)
                index += 1
            }
        }
    }

    private fun toCodePoint(high: Char, low: Char): Int {
        return ((high.code - HIGH_SURROGATE_START) shl 10) +
            (low.code - LOW_SURROGATE_START) +
            SUPPLEMENTARY_PLANE_START
    }

    private fun isCountedCharacter(codePoint: Int): Boolean {
        return isAsciiLetterOrDigit(codePoint) ||
            codePoint == '○'.code ||
            codePoint == '◯'.code ||
            codePoint in '々'.code..'〇'.code ||
            codePoint == '〻'.code ||
            codePoint in 'ぁ'.code..'ゖ'.code ||
            codePoint in 'ゝ'.code..'ゞ'.code ||
            codePoint in 'ァ'.code..'ヺ'.code ||
            codePoint == 'ー'.code ||
            codePoint in '０'.code..'９'.code ||
            codePoint in 'Ａ'.code..'Ｚ'.code ||
            codePoint in 'ａ'.code..'ｚ'.code ||
            codePoint in 'ｦ'.code..'ﾝ'.code ||
            isHan(codePoint) ||
            isHangul(codePoint)
    }

    private fun isAsciiLetterOrDigit(codePoint: Int): Boolean {
        return codePoint in '0'.code..'9'.code ||
            codePoint in 'A'.code..'Z'.code ||
            codePoint in 'a'.code..'z'.code
    }

    private fun isHan(codePoint: Int): Boolean {
        return codePoint in 0x3400..0x4DBF ||
            codePoint in 0x4E00..0x9FFF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x20000..0x2A6DF ||
            codePoint in 0x2A700..0x2B73F ||
            codePoint in 0x2B740..0x2B81F ||
            codePoint in 0x2B820..0x2CEAF ||
            codePoint in 0x2CEB0..0x2EBEF ||
            codePoint in 0x30000..0x3134F ||
            codePoint in 0x31350..0x323AF
    }

    private fun isHangul(codePoint: Int): Boolean {
        return codePoint in 0x1100..0x11FF ||
            codePoint in 0x3130..0x318F ||
            codePoint in 0xA960..0xA97F ||
            codePoint in 0xAC00..0xD7AF ||
            codePoint in 0xD7B0..0xD7FF
    }

    private const val HIGH_SURROGATE_START = 0xD800
    private const val LOW_SURROGATE_START = 0xDC00
    private const val SUPPLEMENTARY_PLANE_START = 0x10000
}
