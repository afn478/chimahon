package tachiyomi.domain.extension.service

data class ExtensionIdentity(
    val packageName: String,
    val signatureHash: String,
)

data class InstalledExtensionGroups<T>(
    val updates: List<T>,
    val installed: List<T>,
)

object ExtensionTypePolicy {
    fun <T> partitionInstalledExtensions(
        installedExtensions: List<T>,
        showNsfwSources: Boolean,
        isNsfw: (T) -> Boolean,
        hasUpdate: (T) -> Boolean,
        requiresAttention: (T) -> Boolean,
        compareNames: (T, T) -> Int,
    ): InstalledExtensionGroups<T> {
        val sortedExtensions = installedExtensions
            .filter { showNsfwSources || !isNsfw(it) }
            .sortedWith { left, right ->
                compareAttentionStatus(left, right, requiresAttention)
                    ?: compareNames(left, right)
            }
        val (updates, installed) = sortedExtensions.partition(hasUpdate)

        return InstalledExtensionGroups(
            updates = updates,
            installed = installed,
        )
    }

    fun <T> sortUntrustedExtensions(
        untrustedExtensions: List<T>,
        compareNames: (T, T) -> Int,
    ): List<T> {
        return untrustedExtensions.sortedWith(compareNames)
    }

    fun <Available, Installed, Untrusted, Source> selectAvailableExtensions(
        availableExtensions: List<Available>,
        installedExtensions: List<Installed>,
        untrustedExtensions: List<Untrusted>,
        enabledLanguages: Set<String>,
        showNsfwSources: Boolean,
        availableIdentity: (Available) -> ExtensionIdentity,
        installedIdentity: (Installed) -> ExtensionIdentity,
        untrustedIdentity: (Untrusted) -> ExtensionIdentity,
        isNsfw: (Available) -> Boolean,
        extensionLanguage: (Available) -> String,
        sources: (Available) -> List<Source>,
        sourceLanguage: (Source) -> String,
        copyForSource: (Available, Source) -> Available,
        compareNames: (Available, Available) -> Int,
    ): List<Available> {
        val installedIdentities = installedExtensions.map(installedIdentity).toSet()
        val untrustedIdentities = untrustedExtensions.map(untrustedIdentity).toSet()

        return availableExtensions
            .filter { extension ->
                val identity = availableIdentity(extension)
                identity !in installedIdentities &&
                    identity !in untrustedIdentities &&
                    (showNsfwSources || !isNsfw(extension))
            }
            .flatMap { extension ->
                val extensionSources = sources(extension)
                if (extensionSources.isEmpty()) {
                    if (extensionLanguage(extension) in enabledLanguages) {
                        listOf(extension)
                    } else {
                        emptyList()
                    }
                } else {
                    extensionSources
                        .filter { source -> sourceLanguage(source) in enabledLanguages }
                        .map { source -> copyForSource(extension, source) }
                }
            }
            .sortedWith(compareNames)
    }

    private fun <T> compareAttentionStatus(
        left: T,
        right: T,
        requiresAttention: (T) -> Boolean,
    ): Int? {
        val leftRequiresAttention = requiresAttention(left)
        val rightRequiresAttention = requiresAttention(right)

        return when {
            leftRequiresAttention == rightRequiresAttention -> null
            leftRequiresAttention -> -1
            else -> 1
        }
    }
}
