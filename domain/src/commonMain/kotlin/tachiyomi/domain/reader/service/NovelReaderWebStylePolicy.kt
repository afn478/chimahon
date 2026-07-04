package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderWebStylePolicy {
    const val BASE_STYLE_ELEMENT_ID = "hoshi-style"

    fun backgroundHex(settings: ReaderSettings): String {
        return when (settings.theme) {
            "dark" -> "#1a1a1a"
            "sepia" -> "#f4ecd8"
            "light" -> "#ffffff"
            else -> rgbHex(settings.backgroundColor)
        }
    }

    fun textHex(settings: ReaderSettings): String {
        return when (settings.theme) {
            "dark" -> "#ffffff"
            "sepia" -> "#5b4636"
            "light" -> "#000000"
            else -> rgbHex(settings.textColor)
        }
    }

    fun baseCss(settings: ReaderSettings): String {
        return buildString {
            if (settings.verticalWriting) {
                appendLine("html, body { writing-mode: vertical-rl !important; }")
            } else {
                appendLine("html, body { writing-mode: horizontal-tb !important; }")
            }

            appendLine("html, body { margin: 0 !important; padding: 0 !important; }")
            appendLine("::highlight(hoshi-selection) { background-color: rgba(130, 150, 200, 0.4); color: inherit; }")
            appendLine("p { margin-block-start: 0 !important; margin-block-end: ${settings.paragraphSpacing}em !important; }")
            appendLine("body * { font-family: inherit !important; }")
            appendLine("img.block-img, svg.block-img { position: static !important; }")
        }
    }

    fun baseStyleElementScript(
        css: String,
        styleElementId: String = BASE_STYLE_ELEMENT_ID,
    ): String {
        val styleElementIdLiteral = NovelReaderWebScriptPolicy.jsStringLiteral(styleElementId)

        return """
            var s = document.getElementById($styleElementIdLiteral);
            if (s) s.remove();
            s = document.createElement('style');
            s.id = $styleElementIdLiteral;
            s.textContent = ${NovelReaderWebScriptPolicy.jsStringLiteral(css)};
            document.head.appendChild(s);
        """.trimIndent()
    }

    fun readerAppearanceScript(
        settings: ReaderSettings,
        backgroundHex: String,
        textHex: String,
        bodyVar: String = "b",
        wrapperVar: String = "wrapper",
        includeBodyFontSize: Boolean = false,
    ): String {
        return buildString {
            appendLine("$wrapperVar.style.setProperty('font-size', '${settings.fontSize}px', 'important');")
            if (includeBodyFontSize) {
                appendLine("$bodyVar.style.setProperty('font-size', '${settings.fontSize}px', 'important');")
                appendLine()
            }
            appendLine("$wrapperVar.style.setProperty('line-height', '${settings.lineHeight}', 'important');")
            appendLine(paragraphSpacingScript(settings))
            if (settings.layoutAdvanced) {
                appendLine("$wrapperVar.style.setProperty('letter-spacing', '${settings.characterSpacing}em', 'important');")
            }
            appendLine(
                "$wrapperVar.style.setProperty('text-align', ${if (settings.justifyText) "'justify'" else "'left'"}, 'important');",
            )
            appendLine()
            appendLine(fontScript(settings, wrapperVar))
            appendLine(themeScript(backgroundHex, textHex, bodyVar, wrapperVar))
            appendLine(furiganaScript(settings))
        }.trim()
    }

    fun paragraphSpacingScript(settings: ReaderSettings): String {
        return """
            var paragraphStyle = document.getElementById('hoshi-paragraph-spacing-style');
            if (!paragraphStyle) {
                paragraphStyle = document.createElement('style');
                paragraphStyle.id = 'hoshi-paragraph-spacing-style';
                document.head.appendChild(paragraphStyle);
            }
            paragraphStyle.textContent = 'p { margin-block-start: 0 !important; margin-block-end: ${settings.paragraphSpacing}em !important; }';
        """.trimIndent()
    }

    fun fontScript(
        settings: ReaderSettings,
        wrapperVar: String,
    ): String {
        val fontUrl = settings.fontUrl
        return if (!fontUrl.isNullOrBlank()) {
            val escapedFontUrl = NovelReaderWebScriptPolicy.jsSingleQuotedContent(fontUrl)
            val fontFaceCss = "@font-face { font-family: 'HoshiCustomFont'; src: url('$escapedFontUrl'); }"
            """
                var fontFace = document.createElement('style');
                fontFace.textContent = ${NovelReaderWebScriptPolicy.jsStringLiteral(fontFaceCss)};
                document.head.appendChild(fontFace);
                document.fonts.ready.then(function() {
                    $wrapperVar.style.setProperty('font-family', 'HoshiCustomFont', 'important');
                });
            """.trimIndent()
        } else {
            val fontFamily = NovelReaderFontPolicy.cssFontFamily(settings.selectedFont)
            "$wrapperVar.style.setProperty('font-family', ${NovelReaderWebScriptPolicy.jsStringLiteral(fontFamily)}, 'important');"
        }
    }

    fun themeScript(
        backgroundHex: String,
        textHex: String,
        bodyVar: String = "b",
        wrapperVar: String = "wrapper",
    ): String {
        return """
            $bodyVar.style.setProperty('background-color', '$backgroundHex', 'important');
            $wrapperVar.style.setProperty('color', '$textHex', 'important');
            document.documentElement.style.setProperty('background-color', '$backgroundHex', 'important');
        """.trimIndent()
    }

    fun furiganaScript(settings: ReaderSettings): String {
        return if (settings.hideFurigana) {
            """
                var furiganaStyle = document.getElementById('hoshi-furigana-style');
                if (!furiganaStyle) {
                    furiganaStyle = document.createElement('style');
                    furiganaStyle.id = 'hoshi-furigana-style';
                    furiganaStyle.textContent = 'rt { display: none !important; }';
                    document.head.appendChild(furiganaStyle);
                }
            """.trimIndent()
        } else {
            """
                var furiganaStyle = document.getElementById('hoshi-furigana-style');
                if (furiganaStyle) furiganaStyle.remove();
            """.trimIndent()
        }
    }

    private fun rgbHex(color: Int): String {
        return "#" + (color and 0xFFFFFF)
            .toString(radix = 16)
            .uppercase()
            .padStart(length = 6, padChar = '0')
    }
}
