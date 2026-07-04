package tachiyomi.domain.extension.service

object ExtensionTrustPolicy {
    fun isTrusted(
        packageName: String,
        versionCode: Long,
        signatureHashes: List<String>,
        trustedRepositoryFingerprints: Set<String>,
        trustedExtensionKeys: Set<String>,
        builtInTrustedFingerprints: Set<String> = emptySet(),
    ): Boolean {
        val trustedFingerprints = trustedRepositoryFingerprints + builtInTrustedFingerprints
        if (trustedFingerprints.any { it in signatureHashes }) {
            return true
        }

        val signatureHash = signatureHashes.lastOrNull() ?: return false
        return trustedExtensionKey(
            packageName = packageName,
            versionCode = versionCode,
            signatureHash = signatureHash,
        ) in trustedExtensionKeys
    }

    fun trust(
        trustedExtensionKeys: Set<String>,
        packageName: String,
        versionCode: Long,
        signatureHash: String,
    ): Set<String> {
        val newTrustedExtensionKey = trustedExtensionKey(
            packageName = packageName,
            versionCode = versionCode,
            signatureHash = signatureHash,
        )

        return trustedExtensionKeys
            .filterNot { it.startsWith("$packageName:") }
            .toSet() + newTrustedExtensionKey
    }

    fun trustedExtensionKey(
        packageName: String,
        versionCode: Long,
        signatureHash: String,
    ): String {
        return "$packageName:$versionCode:$signatureHash"
    }
}
