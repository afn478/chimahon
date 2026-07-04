package mihon.domain.extensionrepo.interactor

import mihon.domain.extensionrepo.exception.SaveExtensionRepoException
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import mihon.domain.extensionrepo.service.ExtensionRepoDetailsFetcher

class CreateExtensionRepo(
    private val repository: ExtensionRepoRepository,
    private val detailsFetcher: ExtensionRepoDetailsFetcher,
) {
    suspend fun await(indexUrl: String): Result {
        val formattedIndexUrl = indexUrl.normalizedExtensionRepoIndexUrl()
            ?: return Result.InvalidUrl

        val baseUrl = formattedIndexUrl.removeSuffix(INDEX_JSON_SUFFIX)
        return detailsFetcher.fetchRepoDetails(baseUrl)?.let { insert(it) } ?: Result.InvalidUrl
    }

    private suspend fun insert(repo: ExtensionRepo): Result {
        return try {
            repository.insertRepo(
                repo.baseUrl,
                repo.name,
                repo.shortName,
                repo.website,
                repo.signingKeyFingerprint,
            )
            Result.Success
        } catch (_: SaveExtensionRepoException) {
            handleInsertionError(repo)
        }
    }

    /**
     * Error Handler for insert when there are trying to create new repositories
     *
     * SaveExtensionRepoException doesn't provide constraint info in exceptions.
     * First check if the conflict was on primary key. if so return RepoAlreadyExists
     * Then check if the conflict was on fingerprint. if so Return DuplicateFingerprint
     * If neither are found, there was some other Error, and return Result.Error
     *
     * @param repo Extension Repo holder for passing to DB/Error Dialog
     */
    private suspend fun handleInsertionError(repo: ExtensionRepo): Result {
        val repoExists = repository.getRepo(repo.baseUrl)
        if (repoExists != null) {
            return Result.RepoAlreadyExists
        }
        val matchingFingerprintRepo = repository.getRepoBySigningKeyFingerprint(repo.signingKeyFingerprint)
        if (matchingFingerprintRepo != null) {
            return Result.DuplicateFingerprint(matchingFingerprintRepo, repo)
        }
        return Result.Error
    }

    sealed interface Result {
        data class DuplicateFingerprint(val oldRepo: ExtensionRepo, val newRepo: ExtensionRepo) : Result
        data object InvalidUrl : Result
        data object RepoAlreadyExists : Result
        data object Success : Result
        data object Error : Result
    }

    companion object {
        const val REPO_HELP = "https://komikku-app.github.io/docs/guides/getting-started#adding-sources"

        // cuong-tran's key
        const val KOMIKKU_SIGNATURE = "cbec121aa82ebb02aaa73806992e0368a97d47b5451ed6524816d03084c45905"
        const val REPO_SIGNATURE = "9add655a78e96c4ec7a53ef89dccb557cb5d767489fac5e785d671a5a75d4da2"
    }
}

private const val HTTPS_SCHEME = "https://"
private const val INDEX_JSON_SUFFIX = "/index.min.json"

internal fun String.normalizedExtensionRepoIndexUrl(): String? {
    val candidate = trim()
    if (candidate.isBlank() || candidate.any(Char::isWhitespace)) return null
    if (!candidate.startsWith(HTTPS_SCHEME, ignoreCase = true)) return null

    val remainder = candidate.drop(HTTPS_SCHEME.length)
    val authority = remainder.substringBefore('/')
    if (authority.isBlank() || authority.contains('@')) return null

    val path = remainder.substringAfter('/', missingDelimiterValue = "")
    val normalized = "$HTTPS_SCHEME${authority.lowercase()}/$path"
    if (!normalized.endsWith(INDEX_JSON_SUFFIX)) return null

    return normalized
}
