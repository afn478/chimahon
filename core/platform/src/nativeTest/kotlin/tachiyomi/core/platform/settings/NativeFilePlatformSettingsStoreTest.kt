package tachiyomi.core.platform.settings

import kotlinx.coroutines.runBlocking
import okio.FileSystem
import tachiyomi.core.platform.storage.NativePlatformStorageDirectories
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NativeFilePlatformSettingsStoreTest {
    @Test
    fun settingsSurviveReloadWithEscapedValues() = runBlocking {
        val root = temporarySettingsDirectory()
        try {
            val settingsFile = root / "settings.tsv"
            val store = FilePlatformSettingsStore(settingsFile)

            store.writeString("reader_mode", "RightToLeft")
            store.writeString("escaped\tkey", "line one\nline two\\tail")
            store.writeBoolean("incognito", true)
            store.writeInt("font_size", 18)

            val reloaded = FilePlatformSettingsStore(settingsFile)

            assertEquals("RightToLeft", reloaded.readString("reader_mode"))
            assertEquals("line one\nline two\\tail", reloaded.readString("escaped\tkey"))
            assertTrue(reloaded.readBoolean("incognito"))
            assertEquals(18, reloaded.readInt("font_size"))
            assertFalse(FileSystem.SYSTEM.exists(settingsFile.parent!! / "${settingsFile.name}.tmp"))
        } finally {
            FileSystem.SYSTEM.deleteRecursively(root, mustExist = false)
        }
    }

    @Test
    fun missingInvalidRemovedAndClearedValuesPersist() = runBlocking {
        val root = temporarySettingsDirectory()
        try {
            val settingsFile = root / "settings.tsv"
            val store = FilePlatformSettingsStore(settingsFile)

            store.writeString("incognito", "maybe")
            store.writeString("font_size", "large")
            store.writeString("one", "1")
            store.writeString("two", "2")
            store.remove("one")

            val reloaded = FilePlatformSettingsStore(settingsFile)
            assertFalse(reloaded.readBoolean("incognito"))
            assertEquals(22, reloaded.readInt("font_size", 22))
            assertNull(reloaded.readString("missing"))
            assertEquals(
                mapOf(
                    "font_size" to "large",
                    "incognito" to "maybe",
                    "two" to "2",
                ),
                reloaded.snapshot(),
            )

            store.clear()

            assertEquals(emptyMap<String, String>(), FilePlatformSettingsStore(settingsFile).snapshot())
        } finally {
            FileSystem.SYSTEM.deleteRecursively(root, mustExist = false)
        }
    }

    private fun temporarySettingsDirectory() =
        NativePlatformStorageDirectories(
            appName = "Chimahon",
            homeDir = "/tmp",
            tempDir = "/tmp",
        ).temporaryDir / "settings-store-${Random.nextLong().toString().replace("-", "n")}"
}
