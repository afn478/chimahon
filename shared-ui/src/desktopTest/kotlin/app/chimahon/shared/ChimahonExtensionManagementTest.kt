package app.chimahon.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChimahonExtensionManagementTest {
    @Test
    fun mergesInstalledScriptExtensionsWithNewestAvailableVersion() {
        val data = buildExtensionManagementData(
            installedExtensions = listOf(
                installedExtension(
                    id = "script.reader",
                    name = "Script Reader",
                    version = "1.2.0",
                    sourceCount = 1,
                ),
            ),
            availableExtensions = listOf(
                repoExtension(
                    id = "script.reader",
                    name = "Script Reader",
                    version = "1.10.0",
                    sourceCount = 2,
                ),
                repoExtension(
                    id = "script.reader",
                    name = "Script Reader",
                    version = "1.3.0",
                    sourceCount = 1,
                ),
            ),
            apkStatus = unsupportedApkStatus(),
            errors = emptyList(),
        )

        val extension = data.extensions.single()
        assertEquals(ChimahonExtensionPackageType.JavaScript, extension.packageType)
        assertEquals("1.2.0", extension.installedVersion)
        assertEquals("1.10.0", extension.availableVersion)
        assertEquals(1, extension.installedSourceCount)
        assertEquals(2, extension.availableSourceCount)
        assertTrue(extension.updateAvailable)
        assertEquals(listOf(extension), data.availableUpdates)
        assertFalse(data.apkExtensionsSupported)
    }

    @Test
    fun keepsScriptAndApkPackagesSeparateWhenApkSupportIsUnavailable() {
        val data = buildExtensionManagementData(
            installedExtensions = listOf(
                installedExtension(
                    id = "reader",
                    name = "Reader Script",
                    version = "2",
                    packageType = ChimahonExtensionPackageType.JavaScript,
                ),
                installedExtension(
                    id = "reader",
                    name = "Reader APK",
                    version = "4",
                    packageType = ChimahonExtensionPackageType.AndroidApk,
                ),
            ),
            availableExtensions = listOf(
                repoExtension(
                    id = "reader",
                    name = "Reader Script",
                    version = "2",
                    packageType = ChimahonExtensionPackageType.JavaScript,
                ),
                repoExtension(
                    id = "reader",
                    name = "Reader APK",
                    version = "5",
                    packageType = ChimahonExtensionPackageType.AndroidApk,
                ),
            ),
            apkStatus = unsupportedApkStatus(
                errors = listOf("APK extensions are unavailable here."),
            ),
            errors = listOf("Repository warning", "APK extensions are unavailable here."),
        )

        assertEquals(2, data.extensions.size)
        val extensionsByType = data.extensions.associateBy(ChimahonExtensionDetails::packageType)
        assertEquals(
            "2",
            extensionsByType.getValue(ChimahonExtensionPackageType.JavaScript).installedVersion,
        )
        assertEquals(
            "4",
            extensionsByType.getValue(ChimahonExtensionPackageType.AndroidApk).installedVersion,
        )
        assertEquals(
            listOf(ChimahonExtensionPackageType.AndroidApk),
            data.availableUpdates.map(ChimahonExtensionDetails::packageType),
        )
        assertEquals(
            listOf("Repository warning", "APK extensions are unavailable here."),
            data.errors,
        )
        assertEquals(0, data.registeredApkSourceCount)
    }
}

private fun installedExtension(
    id: String,
    name: String,
    version: String,
    sourceCount: Int = 1,
    packageType: ChimahonExtensionPackageType = ChimahonExtensionPackageType.JavaScript,
): ChimahonInstalledExtensionEntry {
    return ChimahonInstalledExtensionEntry(
        id = id,
        name = name,
        version = version,
        sourceCount = sourceCount,
        packageType = packageType,
    )
}

private fun repoExtension(
    id: String,
    name: String,
    version: String,
    sourceCount: Int = 1,
    packageType: ChimahonExtensionPackageType = ChimahonExtensionPackageType.JavaScript,
): ChimahonRepoExtensionEntry {
    return ChimahonRepoExtensionEntry(
        repoBaseUrl = "https://repo.example",
        id = id,
        name = name,
        version = version,
        artifactUrl = "https://repo.example/$id.js",
        packageType = packageType,
        language = "en",
        sourceCount = sourceCount,
    )
}

private fun unsupportedApkStatus(
    errors: List<String> = emptyList(),
): ChimahonApkExtensionManagerStatus {
    return ChimahonApkExtensionManagerStatus(
        isSupported = false,
        installedExtensionCount = 0,
        registeredSourceCount = 0,
        errors = errors,
    )
}
