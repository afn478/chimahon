package app.chimahon.shared

import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ChimahonReaderPageActionsTest {
    @Test
    fun writeReaderPageBytesAtomicallyReplacesFinalFileAndRemovesTemporaryFile() = runBlocking {
        val root = Files.createTempDirectory("chimahon-reader-page-actions").toString().toPath()
        val target = root / "reader-pages" / "Manga - Chapter - 1.jpg"

        try {
            FileSystem.SYSTEM.createDirectories(target.parent!!)
            FileSystem.SYSTEM.write(target) {
                write(byteArrayOf(1, 1, 1))
            }

            writeReaderPageBytesAtomically(target, byteArrayOf(2, 3, 4))

            val bytes = FileSystem.SYSTEM.read(target) {
                readByteArray()
            }
            val temporaryFiles = FileSystem.SYSTEM
                .list(target.parent!!)
                .filter { path -> path.name.startsWith("${target.name}.tmp-") }

            assertContentEquals(byteArrayOf(2, 3, 4), bytes)
            assertEquals(emptyList(), temporaryFiles)
        } finally {
            FileSystem.SYSTEM.deleteRecursively(root, mustExist = false)
        }
    }
}
