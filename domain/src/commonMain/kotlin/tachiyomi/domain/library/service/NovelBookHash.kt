package tachiyomi.domain.library.service

import okio.ByteString.Companion.encodeUtf8

object NovelBookHash {
    fun md5Hex(input: String): String {
        return input.encodeUtf8().md5().hex()
    }
}
