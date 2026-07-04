package tachiyomi.domain.reader.service

object NovelEpubContentPolicy {
    const val CONTENT_WRAPPER_ID = "hoshi-content-wrapper"

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val markupExtensions = setOf("html", "xhtml", "htm")

    fun isImageExtension(extension: String): Boolean {
        return extension.lowercase() in imageExtensions
    }

    fun isMarkupExtension(extension: String): Boolean {
        return extension.lowercase() in markupExtensions
    }

    fun isStylesheetExtension(extension: String): Boolean {
        return extension.equals("css", ignoreCase = true)
    }

    fun wrapBodyContent(html: String): String {
        if (html.contains(CONTENT_WRAPPER_ID)) return html

        val bodyTagStart = html.indexOf("<body", ignoreCase = true)
        if (bodyTagStart < 0) return html

        val bodyTagEnd = html.indexOf('>', bodyTagStart)
        if (bodyTagEnd < 0) return html

        val bodyClose = html.lastIndexOf("</body", ignoreCase = true)
        if (bodyClose < 0 || bodyClose <= bodyTagEnd) return html

        return StringBuilder(html.length + 70)
            .append(html, 0, bodyTagEnd + 1)
            .append("<div id=\"$CONTENT_WRAPPER_ID\">")
            .append(html, bodyTagEnd + 1, bodyClose)
            .append("</div>")
            .append(html, bodyClose, html.length)
            .toString()
    }

    fun cleanCss(css: String): String {
        if (css.isBlank()) return css

        return css
            .replace(Regex("""(?is)@page\s*\{[^}]*\}\s*"""), "")
            .replace(Regex("""[ \t]*-epub-[\w-]+\s*:[^;]+;[ \t]*\n?"""), "")
            .replace(Regex("""(?i)[ \t]*(?:-webkit-)?writing-mode\s*:[^;]+;[ \t]*\n?"""), "")
            .replace(Regex("""(?i)[ \t]*column-[\w-]+\s*:[^;]+;[ \t]*\n?"""), "")
            .replace(Regex("""(?i)[ \t]*overflow(?:-[xy])?\s*:[^;]+;[ \t]*\n?"""), "")
            .let(::removeHtmlBodyRules)
            .let(::stripReaderHostileTextRules)
    }

    private fun removeHtmlBodyRules(css: String): String {
        val blockRegex = Regex("""([^{}@]+)\{([^{}]*)\}""")
        val targetSelectorRegex = Regex(
            """(?<![.\w#-])(html|body)(?![.\w-])""",
            RegexOption.IGNORE_CASE,
        )

        return blockRegex.replace(css) { match ->
            val selector = match.groupValues[1]
            if (targetSelectorRegex.containsMatchIn(selector)) {
                ""
            } else {
                match.value
            }
        }
    }

    private fun stripReaderHostileTextRules(css: String): String {
        return css.replace(
            Regex("""(?i)[ \t]*(?:line-height|text-indent)\s*:[^;]+;[ \t]*\n?|[ \t]*text-align\s*:\s*justify\s*;[ \t]*\n?"""),
            "",
        )
    }
}
