package tachiyomi.domain.reader.service

object NovelReaderWebScriptPolicy {
    enum class PageDirection(val value: String) {
        FORWARD("forward"),
        BACKWARD("backward"),
    }

    fun jsStringLiteral(value: String): String {
        return buildString {
            append('\'')
            appendJsSingleQuotedContent(value)
            append('\'')
        }
    }

    fun jsSingleQuotedContent(value: String): String {
        return buildString {
            appendJsSingleQuotedContent(value)
        }
    }

    fun scrollToFragmentScript(fragment: String): String {
        val fragmentLiteral = jsStringLiteral(fragment)
        return """
            (function() {
                var id = $fragmentLiteral;
                var el = document.getElementById(id);
                if (!el) {
                    var named = document.getElementsByName(id);
                    if (named.length > 0) el = named[0];
                }
                if (el) el.scrollIntoView({ behavior: 'instant', block: 'start' });
            })();
        """.trimIndent()
    }

    fun clearSelectionScript(): String {
        return "if(window.hoshiReader && window.hoshiReader.clearSelection) { window.hoshiReader.clearSelection(); }"
    }

    fun highlightSelectionScript(charCount: Int): String {
        return "if(window.hoshiReader && window.hoshiReader.highlightSelection) { window.hoshiReader.highlightSelection($charCount); }"
    }

    fun selectionRectsScript(charCount: Int, startOffset: Int = 0): String {
        return "(function() { try { return window.hoshiReader.getSelectionRects($charCount, $startOffset); } catch(e) { return []; } })()"
    }

    fun handleTapScript(cssX: Float, cssY: Float): String {
        return "if (window.hoshiReader && window.hoshiReader.handleTap) { window.hoshiReader.handleTap($cssX, $cssY); }"
    }

    fun calculateProgressScript(): String {
        return "(function() { return window.hoshiReader.calculateProgress(); })()"
    }

    fun imageOnlyReaderApiScript(
        backgroundTapCallScript: String,
    ): String {
        return """
            window.hoshiReader = {
                handleTap: function(clientX, clientY) {
                    $backgroundTapCallScript
                    return false;
                },
                paginate: function(direction) {
                    return 'limit';
                },
                calculateProgress: function() {
                    return 0;
                }
            };
        """.trimIndent()
    }

    fun continuousBoundaryScript(forward: Boolean): String {
        val forwardLiteral = if (forward) "true" else "false"
        return """
            (function() {
                var el = document.scrollingElement || document.documentElement;
                var ph = window.innerHeight;
                var pw = window.innerWidth;
                var vOver = el.scrollHeight - ph > 1;
                var hOver = el.scrollWidth  - pw > 1;
                var forward = $forwardLiteral;
                if (vOver) {
                    var y = Math.round(window.scrollY);
                    var maxY = el.scrollHeight - ph;
                    if (forward) return y >= maxY - 2 ? 'limit' : 'scrolling';
                    return y <= 2 ? 'limit' : 'scrolling';
                }
                if (hOver) {
                    var x = window.scrollX;
                    var maxX = el.scrollWidth - pw;
                    var absX = Math.abs(x);
                    if (forward) return absX >= maxX - 2 ? 'limit' : 'scrolling';
                    return absX <= 2 ? 'limit' : 'scrolling';
                }
                return 'limit';
            })()
        """.trimIndent()
    }

    fun paginateScript(direction: PageDirection): String {
        return """
            (function() {
                if (!window.hoshiReader || typeof window.hoshiReader.paginate !== 'function') {
                    return "limit";
                }
                return window.hoshiReader.paginate('${direction.value}');
            })()
        """.trimIndent()
    }

    private fun StringBuilder.appendJsSingleQuotedContent(value: String) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\u2028' -> append("\\u2028")
                '\u2029' -> append("\\u2029")
                else -> append(char)
            }
        }
    }
}
