package mihon.domain.extensionrepo.service

import mihon.domain.extensionrepo.model.ExtensionRepo

interface ExtensionRepoDetailsFetcher {
    suspend fun fetchRepoDetails(repo: String): ExtensionRepo?
}
