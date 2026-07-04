package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.ReaderSettings

object NovelReaderWebLayoutPolicy {
    const val PAGED_OVERRIDE_STYLE_ELEMENT_ID = "hoshi-override-style"

    enum class ImageHeightMode {
        FULL_VIEWPORT,
        PAGED_WITH_VERTICAL_OVERLAP,
    }

    fun pagedBottomOverlapPx(settings: ReaderSettings): Double {
        return if (settings.verticalWriting) settings.fontSize else 0.0
    }

    fun viewportMetricsScript(
        settings: ReaderSettings,
        imageHeightMode: ImageHeightMode,
    ): String {
        val imageHeightExpression = when (imageHeightMode) {
            ImageHeightMode.FULL_VIEWPORT -> "ih"
            ImageHeightMode.PAGED_WITH_VERTICAL_OVERLAP -> {
                "ih - ${pagedBottomOverlapLiteral(settings)}"
            }
        }

        return """
            var ih = window.innerHeight;
            var iw = window.innerWidth;

            // Compute padding in px (applied on both sides of wrapper).
            ${paddingMetricsScript(settings)}

            // Image max dimensions at 90vw.
            var imgMaxW = Math.max(1, Math.floor(iw * (100 - ${settings.horizontalPadding}) / 100));
            var imgMaxH = Math.max(1, $imageHeightExpression);
            document.documentElement.style.setProperty('--reader-image-max-width', imgMaxW + 'px');
            document.documentElement.style.setProperty('--reader-image-max-height', imgMaxH + 'px');
        """.trimIndent()
    }

    fun contentPaddingScript(
        settings: ReaderSettings,
        declareViewportMetrics: Boolean,
    ): String {
        return buildString {
            if (declareViewportMetrics) {
                appendLine("var iw = window.innerWidth;")
                appendLine("var ih = window.innerHeight;")
                appendLine(paddingMetricsScript(settings))
                appendLine()
            }

            appendLine("wrapper.style.setProperty('padding', vPad + 'px 0', 'important');")
            appendLine("wrapper.style.setProperty('-webkit-box-decoration-break', 'clone', 'important');")
            appendLine("wrapper.style.setProperty('box-decoration-break', 'clone', 'important');")
            appendLine("b.style.setProperty('padding', '0', 'important');")
            appendLine("b.style.setProperty('margin', '0', 'important');")
            appendLine()
            appendLine("var pPadStyle = document.getElementById('hoshi-p-padding-style');")
            appendLine("if (!pPadStyle) {")
            appendLine("    pPadStyle = document.createElement('style');")
            appendLine("    pPadStyle.id = 'hoshi-p-padding-style';")
            appendLine("    document.head.appendChild(pPadStyle);")
            appendLine("}")
            appendLine("pPadStyle.textContent = '#hoshi-content-wrapper > p { padding-left: ' + hPad + 'px !important; padding-right: ' + hPad + 'px !important; }';")
        }.trim()
    }

    fun imageOnlyDocumentLayoutScript(
        backgroundHex: String,
        restoreCompletedScript: String,
    ): String {
        return """
            var w = window.innerWidth;
            var h = window.innerHeight;

            document.documentElement.style.cssText =
                'margin:0!important;padding:0!important;' +
                'width:' + w + 'px!important;height:' + h + 'px!important;' +
                'overflow:hidden!important;background:$backgroundHex!important;';

            if (!document.body) {
                $restoreCompletedScript
                return;
            }

            var target = document.querySelector('img, svg');
            if (!target) {
                $restoreCompletedScript
                return;
            }

            Array.from(document.body.children).forEach(function(child) {
                if (!child.contains(target)) child.style.display = 'none';
            });

            document.body.style.cssText =
                'margin:0!important;padding:0!important;' +
                'width:' + w + 'px!important;height:' + h + 'px!important;' +
                'display:flex!important;align-items:center!important;justify-content:center!important;' +
                'background:$backgroundHex!important;touch-action:none!important;overflow:hidden!important;';

            var curr = target.parentElement;
            while (curr && curr !== document.body) {
                curr.style.cssText =
                    'display:flex!important;align-items:center!important;justify-content:center!important;' +
                    'margin:0!important;padding:0!important;border:none!important;' +
                    'width:' + w + 'px!important;height:' + h + 'px!important;overflow:hidden!important;';
                curr = curr.parentElement;
            }

            var imgStyle =
                'width:' + w + 'px!important;height:' + h + 'px!important;' +
                'max-width:' + w + 'px!important;max-height:' + h + 'px!important;' +
                'object-fit:contain!important;display:block!important;margin:auto!important;padding:0!important;';

            if (target.tagName.toLowerCase() === 'svg') {
                target.setAttribute('preserveAspectRatio', 'xMidYMid meet');
                target.style.cssText = imgStyle;
                $restoreCompletedScript
            } else {
                target.style.cssText = imgStyle;
                if (target.complete && target.naturalWidth > 0) {
                    $restoreCompletedScript
                } else {
                    target.onload = function() { $restoreCompletedScript };
                    target.onerror = function() { $restoreCompletedScript };
                }
            }
        """.trimIndent()
    }

    fun continuousBodyLayoutScript(
        verticalWriting: Boolean,
        bodyVar: String = "b",
    ): String {
        val verticalWritingLiteral = if (verticalWriting) "true" else "false"

        return """
            var vw = $verticalWritingLiteral;
            if (vw) {
                $bodyVar.style.setProperty('touch-action', 'pan-x', 'important');
                document.documentElement.style.setProperty('overflow-x', 'auto', 'important');
                document.documentElement.style.setProperty('overflow-y', 'hidden', 'important');
            } else {
                $bodyVar.style.setProperty('touch-action', 'pan-y', 'important');
                document.documentElement.style.setProperty('overflow-x', 'hidden', 'important');
                document.documentElement.style.setProperty('overflow-y', 'auto', 'important');
            }
            $bodyVar.style.setProperty('box-sizing', 'border-box', 'important');
            $bodyVar.style.setProperty('width', iw + 'px', 'important');
            $bodyVar.style.setProperty('min-height', ih + 'px', 'important');
            $bodyVar.style.setProperty('height', 'auto', 'important');
            document.documentElement.style.setProperty('height', 'auto', 'important');
        """.trimIndent()
    }

    fun pagedBodyLayoutScript(
        verticalWriting: Boolean,
        bodyVar: String = "b",
    ): String {
        val verticalWritingLiteral = if (verticalWriting) "true" else "false"

        return """
            document.documentElement.style.setProperty('height', ih + 'px', 'important');
            var vw = $verticalWritingLiteral;
            if (vw) {
                $bodyVar.style.setProperty('column-width', ih + 'px', 'important');
                $bodyVar.style.setProperty('min-height', ih + 'px', 'important');
            } else {
                $bodyVar.style.setProperty('column-width', iw + 'px', 'important');
                $bodyVar.style.setProperty('height', ih + 'px', 'important');
            }
            $bodyVar.style.setProperty('box-sizing', 'border-box', 'important');
            $bodyVar.style.setProperty('width', iw + 'px', 'important');
            $bodyVar.style.setProperty('column-fill', 'auto', 'important');
            $bodyVar.style.setProperty('column-gap', '0px', 'important');
            $bodyVar.style.setProperty('touch-action', 'none', 'important');
            document.documentElement.style.setProperty('overflow', 'hidden', 'important');
        """.trimIndent()
    }

    fun pagedOverrideStyleScript(
        styleElementId: String = PAGED_OVERRIDE_STYLE_ELEMENT_ID,
    ): String {
        val styleElementIdLiteral = NovelReaderWebScriptPolicy.jsStringLiteral(styleElementId)

        return """
            var overrideStyle = document.getElementById($styleElementIdLiteral);
            if (!overrideStyle) {
                overrideStyle = document.createElement('style');
                overrideStyle.id = $styleElementIdLiteral;
                document.head.appendChild(overrideStyle);
            }
            overrideStyle.textContent = [
                '[class*="pt"] { margin-top: 0 !important; }',
                '[class*="pb"] { margin-bottom: 0 !important; }',
                'span.img.fpage, span.img.fblk { padding-bottom: 0 !important; }',
                '@page { margin: 0 !important; }'
            ].join(' ');
        """.trimIndent()
    }

    private fun paddingMetricsScript(settings: ReaderSettings): String {
        return """
            var hPad = Math.round(iw * ${settings.horizontalPadding} / 100);
            var vPad = Math.round(ih * ${settings.verticalPadding} / 100);
        """.trimIndent()
    }

    private fun pagedBottomOverlapLiteral(settings: ReaderSettings): String {
        return if (settings.verticalWriting) settings.fontSize.toString() else "0"
    }
}
