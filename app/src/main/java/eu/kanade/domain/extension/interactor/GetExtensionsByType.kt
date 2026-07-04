package eu.kanade.domain.extension.interactor

import eu.kanade.domain.extension.model.Extensions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.extension.service.ExtensionIdentity
import tachiyomi.domain.extension.service.ExtensionTypePolicy

class GetExtensionsByType(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<Extensions> {
        val showNsfwSources = preferences.showNsfwSource().get()

        return combine(
            preferences.enabledLanguages().changes(),
            extensionManager.installedExtensionsFlow,
            extensionManager.untrustedExtensionsFlow,
            extensionManager.availableExtensionsFlow,
        ) { enabledLanguages, _installed, _untrusted, _available ->
            val (updates, installed) = ExtensionTypePolicy.partitionInstalledExtensions(
                installedExtensions = _installed,
                showNsfwSources = showNsfwSources,
                isNsfw = Extension.Installed::isNsfw,
                hasUpdate = Extension.Installed::hasUpdate,
                requiresAttention = { it.isObsolete || it.isRedundant },
                compareNames = ::compareExtensionNames,
            )

            val untrusted = ExtensionTypePolicy.sortUntrustedExtensions(
                untrustedExtensions = _untrusted,
                compareNames = ::compareExtensionNames,
            )

            val available = ExtensionTypePolicy.selectAvailableExtensions(
                availableExtensions = _available,
                installedExtensions = _installed,
                untrustedExtensions = _untrusted,
                enabledLanguages = enabledLanguages,
                showNsfwSources = showNsfwSources,
                availableIdentity = { it.identity() },
                installedIdentity = { it.identity() },
                untrustedIdentity = { it.identity() },
                isNsfw = Extension.Available::isNsfw,
                extensionLanguage = Extension.Available::lang,
                sources = Extension.Available::sources,
                sourceLanguage = Extension.Available.Source::lang,
                copyForSource = { ext, source ->
                    ext.copy(
                        name = source.name,
                        lang = source.lang,
                        pkgName = "${ext.pkgName}-${source.id}",
                        sources = listOf(source),
                    )
                },
                compareNames = ::compareExtensionNames,
            )

            Extensions(updates, installed, available, untrusted)
        }
    }

    private companion object {
        fun Extension.identity(): ExtensionIdentity {
            return ExtensionIdentity(
                packageName = pkgName,
                signatureHash = signatureHash,
            )
        }

        fun compareExtensionNames(left: Extension, right: Extension): Int {
            return String.CASE_INSENSITIVE_ORDER.compare(left.name, right.name)
        }
    }
}
