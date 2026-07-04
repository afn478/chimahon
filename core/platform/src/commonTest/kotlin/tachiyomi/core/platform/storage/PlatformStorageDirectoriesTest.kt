package tachiyomi.core.platform.storage

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformStorageDirectoriesTest {
    @Test
    fun fileUrisEncodeReservedCharactersInPosixPaths() {
        val path = "/Users/tester/Chimahon files/#cover 1?.jpg".toPath()

        assertEquals(
            "file:///Users/tester/Chimahon%20files/%23cover%201%3F.jpg",
            encodedFileUri(path),
        )
    }

    @Test
    fun fileUrisNormalizeWindowsDriveSeparators() {
        val path = "C:\\Users\\tester\\Chimahon files\\cover.jpg".toPath()

        assertEquals(
            "file:///C:/Users/tester/Chimahon%20files/cover.jpg",
            encodedFileUri(path),
        )
    }

    @Test
    fun fileUrisHandleNormalizedDoubleSlashPaths() {
        val path = "//server/share/Chimahon files/cover.jpg".toPath()

        assertEquals(
            "file:///server/share/Chimahon%20files/cover.jpg",
            encodedFileUri(path),
        )
    }

    @Test
    fun pathSegmentsReplaceCharactersUnsafeOnCommonFilesystems() {
        assertEquals("Chi_ma_hon_", sanitizePathSegment("Chi:ma/hon?"))
        assertEquals("app", sanitizePathSegment("..."))
    }
}
