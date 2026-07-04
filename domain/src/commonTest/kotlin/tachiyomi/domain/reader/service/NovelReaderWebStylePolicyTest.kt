package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import tachiyomi.domain.reader.model.ReaderSettings

class NovelReaderWebStylePolicyTest {
    @Test
    fun themeHexValuesPreserveReaderWebThemeOverrides() {
        assertEquals(
            "#1a1a1a",
            NovelReaderWebStylePolicy.backgroundHex(ReaderSettings(theme = "dark")),
        )
        assertEquals(
            "#ffffff",
            NovelReaderWebStylePolicy.textHex(ReaderSettings(theme = "dark")),
        )
        assertEquals(
            "#f4ecd8",
            NovelReaderWebStylePolicy.backgroundHex(ReaderSettings(theme = "sepia")),
        )
        assertEquals(
            "#5b4636",
            NovelReaderWebStylePolicy.textHex(ReaderSettings(theme = "sepia")),
        )
    }

    @Test
    fun themeHexValuesUseReaderColorsForCustomAndSystemThemes() {
        val settings = ReaderSettings(
            theme = "custom",
            backgroundColor = 0xFF123456.toInt(),
            textColor = 0xFF00A0EF.toInt(),
        )

        assertEquals("#123456", NovelReaderWebStylePolicy.backgroundHex(settings))
        assertEquals("#00A0EF", NovelReaderWebStylePolicy.textHex(settings))
    }

    @Test
    fun baseCssReflectsWritingModeAndParagraphSpacing() {
        val verticalCss = NovelReaderWebStylePolicy.baseCss(
            ReaderSettings(verticalWriting = true, paragraphSpacing = 0.25),
        )
        val horizontalCss = NovelReaderWebStylePolicy.baseCss(
            ReaderSettings(verticalWriting = false, paragraphSpacing = 1.5),
        )

        assertTrue(verticalCss.contains("writing-mode: vertical-rl !important"))
        assertTrue(horizontalCss.contains("writing-mode: horizontal-tb !important"))
        assertTrue(verticalCss.contains("margin-block-end: 0.25em !important"))
        assertTrue(horizontalCss.contains("margin-block-end: 1.5em !important"))
        assertTrue(verticalCss.contains("::highlight(hoshi-selection)"))
        assertTrue(verticalCss.contains("img.block-img, svg.block-img { position: static !important; }"))
    }

    @Test
    fun baseStyleElementScriptReplacesStableStyleElementWithEscapedCss() {
        val script = NovelReaderWebStylePolicy.baseStyleElementScript(
            css = "body::before { content: 'reader'; }\nhtml { margin: 0; }",
            styleElementId = "reader-style",
        )

        assertTrue(script.contains("var s = document.getElementById('reader-style');"))
        assertTrue(script.contains("if (s) s.remove();"))
        assertTrue(script.contains("s = document.createElement('style');"))
        assertTrue(script.contains("s.id = 'reader-style';"))
        assertTrue(script.contains("s.textContent = 'body::before { content: \\'reader\\'; }\\nhtml { margin: 0; }';"))
        assertTrue(script.contains("document.head.appendChild(s);"))
    }

    @Test
    fun readerAppearanceScriptAppliesTypographyThemeFontAndFurigana() {
        val script = NovelReaderWebStylePolicy.readerAppearanceScript(
            settings = ReaderSettings(
                fontSize = 18.0,
                lineHeight = 1.7,
                paragraphSpacing = 0.4,
                layoutAdvanced = true,
                characterSpacing = 0.08,
                justifyText = true,
                selectedFont = NovelReaderFontPolicy.SYSTEM_SANS_SERIF,
                hideFurigana = true,
            ),
            backgroundHex = "#112233",
            textHex = "#AABBCC",
        )

        assertTrue(script.contains("wrapper.style.setProperty('font-size', '18.0px', 'important');"))
        assertTrue(script.contains("wrapper.style.setProperty('line-height', '1.7', 'important');"))
        assertTrue(script.contains("margin-block-end: 0.4em !important"))
        assertTrue(script.contains("wrapper.style.setProperty('letter-spacing', '0.08em', 'important');"))
        assertTrue(script.contains("wrapper.style.setProperty('text-align', 'justify', 'important');"))
        assertTrue(script.contains("wrapper.style.setProperty('font-family', 'sans-serif', 'important');"))
        assertTrue(script.contains("b.style.setProperty('background-color', '#112233', 'important');"))
        assertTrue(script.contains("wrapper.style.setProperty('color', '#AABBCC', 'important');"))
        assertTrue(script.contains("rt { display: none !important; }"))
    }

    @Test
    fun readerAppearanceScriptCanIncludeBodyFontSizeForSettingsReapply() {
        val script = NovelReaderWebStylePolicy.readerAppearanceScript(
            settings = ReaderSettings(
                fontSize = 16.0,
                layoutAdvanced = false,
                justifyText = false,
            ),
            backgroundHex = "#000000",
            textHex = "#FFFFFF",
            includeBodyFontSize = true,
        )

        assertTrue(script.contains("wrapper.style.setProperty('font-size', '16.0px', 'important');"))
        assertTrue(script.contains("b.style.setProperty('font-size', '16.0px', 'important');"))
        assertTrue(script.contains("wrapper.style.setProperty('text-align', 'left', 'important');"))
        assertTrue(!script.contains("letter-spacing"))
    }

    @Test
    fun paragraphSpacingScriptUpdatesStableStyleElement() {
        val script = NovelReaderWebStylePolicy.paragraphSpacingScript(
            ReaderSettings(paragraphSpacing = 0.75),
        )

        assertTrue(script.contains("hoshi-paragraph-spacing-style"))
        assertTrue(script.contains("margin-block-end: 0.75em !important"))
    }

    @Test
    fun fontScriptUsesDefaultFontFamilies() {
        assertEquals(
            "wrapper.style.setProperty('font-family', 'serif', 'important');",
            NovelReaderWebStylePolicy.fontScript(
                settings = ReaderSettings(selectedFont = NovelReaderFontPolicy.SYSTEM_SERIF),
                wrapperVar = "wrapper",
            ),
        )
        assertEquals(
            "wrapper.style.setProperty('font-family', 'sans-serif', 'important');",
            NovelReaderWebStylePolicy.fontScript(
                settings = ReaderSettings(selectedFont = NovelReaderFontPolicy.SYSTEM_SANS_SERIF),
                wrapperVar = "wrapper",
            ),
        )
    }

    @Test
    fun fontScriptEscapesCustomFontFamilyAndFontUrl() {
        assertEquals(
            "wrapper.style.setProperty('font-family', 'A\\\\B\\'C', 'important');",
            NovelReaderWebStylePolicy.fontScript(
                settings = ReaderSettings(selectedFont = "A\\B'C"),
                wrapperVar = "wrapper",
            ),
        )

        val script = NovelReaderWebStylePolicy.fontScript(
            settings = ReaderSettings(fontUrl = "file:///fonts/A Font.ttf"),
            wrapperVar = "wrapper",
        )
        assertTrue(script.contains("fontFace.textContent = '@font-face"))
        assertTrue(script.contains("src: url(\\'file:///fonts/A Font.ttf\\')"))
        assertTrue(script.contains("document.fonts.ready.then"))
        assertTrue(script.contains("wrapper.style.setProperty('font-family', 'HoshiCustomFont', 'important')"))
    }

    @Test
    fun themeScriptUpdatesBodyWrapperAndDocumentBackground() {
        val script = NovelReaderWebStylePolicy.themeScript(
            backgroundHex = "#123456",
            textHex = "#ABCDEF",
        )

        assertTrue(script.contains("b.style.setProperty('background-color', '#123456', 'important')"))
        assertTrue(script.contains("wrapper.style.setProperty('color', '#ABCDEF', 'important')"))
        assertTrue(script.contains("document.documentElement.style.setProperty('background-color', '#123456', 'important')"))
    }

    @Test
    fun furiganaScriptHidesOrRemovesFuriganaStyle() {
        val hideScript = NovelReaderWebStylePolicy.furiganaScript(
            ReaderSettings(hideFurigana = true),
        )
        val showScript = NovelReaderWebStylePolicy.furiganaScript(
            ReaderSettings(hideFurigana = false),
        )

        assertTrue(hideScript.contains("hoshi-furigana-style"))
        assertTrue(hideScript.contains("rt { display: none !important; }"))
        assertTrue(showScript.contains("if (furiganaStyle) furiganaStyle.remove();"))
    }
}
