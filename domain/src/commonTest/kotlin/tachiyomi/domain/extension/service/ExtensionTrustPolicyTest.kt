package tachiyomi.domain.extension.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionTrustPolicyTest {
    @Test
    fun isTrustedAcceptsRepositoryFingerprints() {
        val trusted = ExtensionTrustPolicy.isTrusted(
            packageName = "pkg",
            versionCode = 1,
            signatureHashes = listOf("repo-signature"),
            trustedRepositoryFingerprints = setOf("repo-signature"),
            trustedExtensionKeys = emptySet(),
        )

        assertTrue(trusted)
    }

    @Test
    fun isTrustedAcceptsBuiltInFingerprints() {
        val trusted = ExtensionTrustPolicy.isTrusted(
            packageName = "pkg",
            versionCode = 1,
            signatureHashes = listOf("built-in-signature"),
            trustedRepositoryFingerprints = emptySet(),
            trustedExtensionKeys = emptySet(),
            builtInTrustedFingerprints = setOf("built-in-signature"),
        )

        assertTrue(trusted)
    }

    @Test
    fun isTrustedAcceptsStoredTrustedExtensionKey() {
        val trusted = ExtensionTrustPolicy.isTrusted(
            packageName = "pkg",
            versionCode = 3,
            signatureHashes = listOf("old-signature", "new-signature"),
            trustedRepositoryFingerprints = emptySet(),
            trustedExtensionKeys = setOf("pkg:3:new-signature"),
        )

        assertTrue(trusted)
    }

    @Test
    fun isTrustedRejectsUnknownSignaturesAndEmptySignatureLists() {
        assertFalse(
            ExtensionTrustPolicy.isTrusted(
                packageName = "pkg",
                versionCode = 1,
                signatureHashes = listOf("unknown"),
                trustedRepositoryFingerprints = setOf("repo-signature"),
                trustedExtensionKeys = setOf("pkg:1:trusted"),
            ),
        )
        assertFalse(
            ExtensionTrustPolicy.isTrusted(
                packageName = "pkg",
                versionCode = 1,
                signatureHashes = emptyList(),
                trustedRepositoryFingerprints = setOf("repo-signature"),
                trustedExtensionKeys = setOf("pkg:1:trusted"),
            ),
        )
    }

    @Test
    fun trustReplacesPreviousPackageEntriesAndKeepsOtherPackages() {
        val trustedExtensionKeys = ExtensionTrustPolicy.trust(
            trustedExtensionKeys = setOf(
                "pkg:1:old",
                "other:1:signature",
            ),
            packageName = "pkg",
            versionCode = 2,
            signatureHash = "new",
        )

        assertEquals(
            setOf(
                "pkg:2:new",
                "other:1:signature",
            ),
            trustedExtensionKeys,
        )
    }

    @Test
    fun trustedExtensionKeyUsesPersistedColonSeparatedFormat() {
        assertEquals(
            "pkg.name:42:signature",
            ExtensionTrustPolicy.trustedExtensionKey(
                packageName = "pkg.name",
                versionCode = 42,
                signatureHash = "signature",
            ),
        )
    }
}
