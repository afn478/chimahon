package tachiyomi.domain.library.service

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelBookHashTest {
    @Test
    fun md5HexMatchesStableVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", NovelBookHash.md5Hex(""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", NovelBookHash.md5Hex("abc"))
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", NovelBookHash.md5Hex("hello world"))
    }
}
