package tachiyomi.domain.extension.service

object ExtensionLanguagePolicy {
    fun <T> selectLanguages(
        extensions: List<T>,
        enabledLanguages: Set<String>,
        extensionLanguage: (T) -> String,
        sourceLanguages: (T) -> List<String>,
        compareLanguages: (String, String) -> Int,
    ): List<String> {
        return extensions
            .flatMap { extension ->
                sourceLanguages(extension).ifEmpty {
                    listOf(extensionLanguage(extension))
                }
            }
            .distinct()
            .sortedWith { left, right ->
                compareEnabledLanguageStatus(left, right, enabledLanguages)
                    ?: compareLanguages(left, right)
            }
    }

    private fun compareEnabledLanguageStatus(
        left: String,
        right: String,
        enabledLanguages: Set<String>,
    ): Int? {
        val leftEnabled = left in enabledLanguages
        val rightEnabled = right in enabledLanguages

        return when {
            leftEnabled == rightEnabled -> null
            leftEnabled -> -1
            else -> 1
        }
    }
}
