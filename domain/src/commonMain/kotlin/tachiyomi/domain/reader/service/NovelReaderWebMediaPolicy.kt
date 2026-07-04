package tachiyomi.domain.reader.service

object NovelReaderWebMediaPolicy {
    const val LARGE_MEDIA_THRESHOLD_PX = 256

    enum class MediaMode(
        val styleElementId: String,
        val avoidColumnBreaks: Boolean,
    ) {
        CONTINUOUS(
            styleElementId = "reader-cont-img-style",
            avoidColumnBreaks = false,
        ),
        PAGED(
            styleElementId = "reader-block-img-style",
            avoidColumnBreaks = true,
        ),
    }

    fun blockMediaStyleScript(mode: MediaMode): String {
        return """
            var mediaStyle = document.getElementById('${mode.styleElementId}');
            if (mediaStyle) mediaStyle.remove();
            mediaStyle = document.createElement('style');
            mediaStyle.id = '${mode.styleElementId}';
            mediaStyle.textContent = ${NovelReaderWebScriptPolicy.jsStringLiteral(blockMediaCss(mode))};
            document.head.appendChild(mediaStyle);
        """.trimIndent()
    }

    fun avoidPageBreakStyleScript(enabled: Boolean): String {
        if (!enabled) return ""

        return """
            var abStyle = document.createElement('style');
            abStyle.textContent = ${NovelReaderWebScriptPolicy.jsStringLiteral(avoidPageBreakCss())};
            document.head.appendChild(abStyle);
        """.trimIndent()
    }

    fun classifyMediaAndRestoreProgressScript(
        pendingProgress: Double,
        verticalWriting: Boolean,
    ): String {
        val verticalWritingLiteral = if (verticalWriting) "true" else "false"

        return """
            var allMedia = Array.from(document.querySelectorAll('img, svg'));
            var imagePromises = allMedia.map(function(el) {
                return new Promise(function(resolve) {
                    var tag = el.tagName.toLowerCase();
                    var isGaiji = el.classList.contains('gaiji') || el.classList.contains('gaiji-line');
                    var classify = function() {
                        if (!isGaiji) {
                            var isLarge = false;
                            if (tag === 'img') {
                                isLarge = el.naturalWidth > $LARGE_MEDIA_THRESHOLD_PX || el.naturalHeight > $LARGE_MEDIA_THRESHOLD_PX;
                            } else if (tag === 'svg') {
                                var vb = el.viewBox && el.viewBox.baseVal;
                                var w = vb ? vb.width : (el.width ? el.width.baseVal.value : 0);
                                var h = vb ? vb.height : (el.height ? el.height.baseVal.value : 0);
                                isLarge = w > $LARGE_MEDIA_THRESHOLD_PX || h > $LARGE_MEDIA_THRESHOLD_PX;
                            }
                            if (isLarge) {
                                el.classList.add('block-img');
                            }
                        }
                        resolve();
                    };
                    if (tag === 'img') {
                        if (el.complete && el.naturalWidth > 0) { classify(); }
                        else { el.onload = classify; el.onerror = function() { resolve(); }; }
                    } else {
                        classify();
                    }
                });
            });
            Promise.all(imagePromises)
                .then(function() { return new Promise(function(r) { setTimeout(r, 50); }); })
                .then(function() {
                    window.hoshiReader.restoreProgress($pendingProgress, $verticalWritingLiteral);
                });
        """.trimIndent()
    }

    private fun blockMediaCss(mode: MediaMode): String {
        val breakInsideRules = if (mode.avoidColumnBreaks) {
            """
              break-inside: avoid !important;
              -webkit-column-break-inside: avoid !important;
            """.trimIndent()
        } else {
            ""
        }

        return buildString {
            appendLine("img.block-img, svg.block-img {")
            appendLine("  max-width: var(--reader-image-max-width, 95vw) !important;")
            appendLine("  max-height: var(--reader-image-max-height, 95vh) !important;")
            appendLine("  width: auto !important;")
            appendLine("  height: auto !important;")
            appendLine("  display: block !important;")
            appendLine("  margin: auto !important;")
            if (breakInsideRules.isNotEmpty()) {
                appendLine(breakInsideRules)
            }
            appendLine("  object-fit: contain !important;")
            appendLine("}")
            appendLine("img:not(.block-img), svg:not(.block-img) {")
            appendLine("  max-width: min(var(--reader-image-max-width, 95vw), 100%) !important;")
            appendLine("  width: auto !important;")
            appendLine("  height: auto !important;")
            appendLine("  min-width: 1em !important;")
            appendLine("  vertical-align: middle !important;")
            appendLine("  display: inline-block !important;")
            appendLine("}")
        }.trim()
    }

    private fun avoidPageBreakCss(): String {
        return """
            img, svg, figure, table, tr, td, th,
            p:has(> img:only-child), div:has(> img:only-child), span.img, div.img, p.img {
              break-inside: avoid !important;
              -webkit-column-break-inside: avoid !important;
              page-break-inside: avoid !important;
            }
        """.trimIndent()
    }
}
