package eu.kanade.domain.extension.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import mihon.domain.extensionrepo.interactor.CreateExtensionRepo
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.extension.service.ExtensionTrustPolicy

class TrustExtension(
    private val extensionRepoRepository: ExtensionRepoRepository,
    private val preferences: SourcePreferences,
) {

    suspend fun isTrusted(pkgInfo: PackageInfo, fingerprints: List<String>): Boolean {
        // KMK -->
        if (isDebugBuildType) return true
        // KMK <--
        val trustedFingerprints = extensionRepoRepository.getAll().map { it.signingKeyFingerprint }.toHashSet()
        return ExtensionTrustPolicy.isTrusted(
            packageName = pkgInfo.packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
            signatureHashes = fingerprints,
            trustedRepositoryFingerprints = trustedFingerprints,
            trustedExtensionKeys = preferences.trustedExtensions().get(),
            builtInTrustedFingerprints = setOf(CreateExtensionRepo.KOMIKKU_SIGNATURE),
        )
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions().getAndSet { exts ->
            ExtensionTrustPolicy.trust(
                trustedExtensionKeys = exts,
                packageName = pkgName,
                versionCode = versionCode,
                signatureHash = signatureHash,
            )
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions().delete()
    }
}
