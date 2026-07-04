package tachiyomi.domain.reader.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelReaderCharacterCountPolicyTest {
    @Test
    fun countChapterCharactersUsesBodyTextAndTtsuCharacterRules() {
        val content = """
            <html>
              <head><title>IgnoredTitle123</title></head>
              <body>
                <p>Hello 123 日本語 한글 カナ かな ｶﾀｶﾅ ＡＢＣ</p>
                <ruby>漢<rt>かん</rt></ruby>
                <script>DROP漢</script>
                <style>DROP漢</style>
                &amp;&lt;&gt;&nbsp;!
              </body>
            </html>
        """.trimIndent()

        assertEquals(25, NovelReaderCharacterCountPolicy.countChapterCharacters(content))
    }

    @Test
    fun countChapterCharactersFallsBackToWholeContentWhenBodyIsMissing() {
        val content = "<p>Outside body 42<rt>ignored</rt></p>"

        assertEquals(13, NovelReaderCharacterCountPolicy.countChapterCharacters(content))
    }

    @Test
    fun countChapterCharactersCountsSupplementaryPlaneHanAsOneCharacter() {
        val supplementaryHan = "\uD840\uDC00"

        assertEquals(1, NovelReaderCharacterCountPolicy.countChapterCharacters(supplementaryHan))
    }

    @Test
    fun countChapterCharactersHandlesNullContent() {
        assertEquals(0, NovelReaderCharacterCountPolicy.countChapterCharacters(null))
    }
}
