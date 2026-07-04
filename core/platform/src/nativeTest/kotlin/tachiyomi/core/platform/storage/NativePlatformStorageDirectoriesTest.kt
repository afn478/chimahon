package tachiyomi.core.platform.storage

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class NativePlatformStorageDirectoriesTest {
    @Test
    fun fileUriEncodesPathsForNativeHosts() {
        val directories = NativePlatformStorageDirectories(
            appName = "Chimahon",
            homeDir = "/Users/tester",
            tempDir = "/tmp",
        )

        assertEquals(
            "file:///tmp/Chimahon%20files/%23cover%201%3F.jpg",
            directories.fileUri("/tmp/Chimahon files/#cover 1?.jpg".toPath()),
        )
    }
}
