package tachiyomi.domain.extension.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionTypePolicyTest {
    @Test
    fun partitionInstalledExtensionsFiltersNsfwSortsAttentionFirstAndPartitionsUpdates() {
        val groups = ExtensionTypePolicy.partitionInstalledExtensions(
            installedExtensions = listOf(
                installed(name = "Zulu"),
                installed(name = "Alpha", hasUpdate = true),
                installed(name = "Beta", hasUpdate = true, requiresAttention = true),
                installed(name = "Hidden", isNsfw = true),
                installed(name = "Gamma", requiresAttention = true),
            ),
            showNsfwSources = false,
            isNsfw = InstalledExtension::isNsfw,
            hasUpdate = InstalledExtension::hasUpdate,
            requiresAttention = InstalledExtension::requiresAttention,
            compareNames = ::compareInstalledNames,
        )

        assertEquals(listOf("Beta", "Alpha"), groups.updates.map(InstalledExtension::name))
        assertEquals(listOf("Gamma", "Zulu"), groups.installed.map(InstalledExtension::name))
    }

    @Test
    fun partitionInstalledExtensionsKeepsNsfwExtensionsWhenEnabled() {
        val groups = ExtensionTypePolicy.partitionInstalledExtensions(
            installedExtensions = listOf(
                installed(name = "Safe"),
                installed(name = "Nsfw", isNsfw = true),
            ),
            showNsfwSources = true,
            isNsfw = InstalledExtension::isNsfw,
            hasUpdate = InstalledExtension::hasUpdate,
            requiresAttention = InstalledExtension::requiresAttention,
            compareNames = ::compareInstalledNames,
        )

        assertEquals(listOf("Nsfw", "Safe"), groups.installed.map(InstalledExtension::name))
    }

    @Test
    fun sortUntrustedExtensionsUsesInjectedNameComparator() {
        val sorted = ExtensionTypePolicy.sortUntrustedExtensions(
            untrustedExtensions = listOf(
                untrusted(name = "Zulu"),
                untrusted(name = "alpha"),
            ),
            compareNames = ::compareUntrustedNames,
        )

        assertEquals(listOf("alpha", "Zulu"), sorted.map(UntrustedExtension::name))
    }

    @Test
    fun selectAvailableExtensionsExcludesInstalledUntrustedAndNsfwExtensions() {
        val selected = ExtensionTypePolicy.selectAvailableExtensions(
            availableExtensions = listOf(
                available(name = "Available", packageName = "available"),
                available(name = "Installed", packageName = "installed"),
                available(name = "Untrusted", packageName = "untrusted"),
                available(name = "Nsfw", packageName = "nsfw", isNsfw = true),
            ),
            installedExtensions = listOf(installed(name = "Installed", packageName = "installed")),
            untrustedExtensions = listOf(untrusted(name = "Untrusted", packageName = "untrusted")),
            enabledLanguages = setOf("en"),
            showNsfwSources = false,
            availableIdentity = AvailableExtension::identity,
            installedIdentity = InstalledExtension::identity,
            untrustedIdentity = UntrustedExtension::identity,
            isNsfw = AvailableExtension::isNsfw,
            extensionLanguage = AvailableExtension::language,
            sources = AvailableExtension::sources,
            sourceLanguage = AvailableSource::language,
            copyForSource = ::copyAvailableForSource,
            compareNames = ::compareAvailableNames,
        )

        assertEquals(listOf("Available"), selected.map(AvailableExtension::name))
    }

    @Test
    fun selectAvailableExtensionsExpandsSourcesByEnabledLanguagesAndSortsNames() {
        val selected = ExtensionTypePolicy.selectAvailableExtensions(
            availableExtensions = listOf(
                available(
                    name = "Multi",
                    packageName = "multi",
                    language = "all",
                    sources = listOf(
                        source(id = 1, name = "Zulu", language = "ja"),
                        source(id = 2, name = "Alpha", language = "en"),
                    ),
                ),
                available(name = "Fallback", packageName = "fallback", language = "en"),
                available(name = "Hidden", packageName = "hidden", language = "fr"),
            ),
            installedExtensions = emptyList(),
            untrustedExtensions = emptyList(),
            enabledLanguages = setOf("en"),
            showNsfwSources = true,
            availableIdentity = AvailableExtension::identity,
            installedIdentity = InstalledExtension::identity,
            untrustedIdentity = UntrustedExtension::identity,
            isNsfw = AvailableExtension::isNsfw,
            extensionLanguage = AvailableExtension::language,
            sources = AvailableExtension::sources,
            sourceLanguage = AvailableSource::language,
            copyForSource = ::copyAvailableForSource,
            compareNames = ::compareAvailableNames,
        )

        assertEquals(listOf("Alpha", "Fallback"), selected.map(AvailableExtension::name))
        assertEquals(listOf("multi-2", "fallback"), selected.map { it.identity.packageName })
    }

    private data class InstalledExtension(
        val name: String,
        val identity: ExtensionIdentity,
        val isNsfw: Boolean,
        val hasUpdate: Boolean,
        val requiresAttention: Boolean,
    )

    private data class UntrustedExtension(
        val name: String,
        val identity: ExtensionIdentity,
    )

    private data class AvailableExtension(
        val name: String,
        val identity: ExtensionIdentity,
        val language: String,
        val isNsfw: Boolean,
        val sources: List<AvailableSource>,
    )

    private data class AvailableSource(
        val id: Long,
        val name: String,
        val language: String,
    )

    private fun installed(
        name: String,
        packageName: String = name.lowercase(),
        isNsfw: Boolean = false,
        hasUpdate: Boolean = false,
        requiresAttention: Boolean = false,
    ): InstalledExtension {
        return InstalledExtension(
            name = name,
            identity = identity(packageName),
            isNsfw = isNsfw,
            hasUpdate = hasUpdate,
            requiresAttention = requiresAttention,
        )
    }

    private fun untrusted(
        name: String,
        packageName: String = name.lowercase(),
    ): UntrustedExtension {
        return UntrustedExtension(
            name = name,
            identity = identity(packageName),
        )
    }

    private fun available(
        name: String,
        packageName: String = name.lowercase(),
        language: String = "en",
        isNsfw: Boolean = false,
        sources: List<AvailableSource> = emptyList(),
    ): AvailableExtension {
        return AvailableExtension(
            name = name,
            identity = identity(packageName),
            language = language,
            isNsfw = isNsfw,
            sources = sources,
        )
    }

    private fun source(
        id: Long,
        name: String,
        language: String,
    ): AvailableSource {
        return AvailableSource(
            id = id,
            name = name,
            language = language,
        )
    }

    private fun identity(packageName: String): ExtensionIdentity {
        return ExtensionIdentity(
            packageName = packageName,
            signatureHash = "signature",
        )
    }

    private companion object {
        fun compareInstalledNames(left: InstalledExtension, right: InstalledExtension): Int {
            return left.name.compareTo(right.name, ignoreCase = true)
        }

        fun compareUntrustedNames(left: UntrustedExtension, right: UntrustedExtension): Int {
            return left.name.compareTo(right.name, ignoreCase = true)
        }

        fun compareAvailableNames(left: AvailableExtension, right: AvailableExtension): Int {
            return left.name.compareTo(right.name, ignoreCase = true)
        }

        fun copyAvailableForSource(
            extension: AvailableExtension,
            source: AvailableSource,
        ): AvailableExtension {
            return extension.copy(
                name = source.name,
                identity = extension.identity.copy(
                    packageName = "${extension.identity.packageName}-${source.id}",
                ),
                language = source.language,
                sources = listOf(source),
            )
        }
    }
}
