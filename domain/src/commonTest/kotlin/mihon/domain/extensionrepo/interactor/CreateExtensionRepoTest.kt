package mihon.domain.extensionrepo.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mihon.domain.extensionrepo.exception.SaveExtensionRepoException
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import mihon.domain.extensionrepo.service.ExtensionRepoDetailsFetcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateExtensionRepoTest {
    @Test
    fun awaitInsertsFetchedRepoForValidIndexUrl() = runTest {
        val repository = FakeExtensionRepoRepository()
        val fetchedRepo = extensionRepo(baseUrl = "https://repo.example/extensions")
        val fetcher = FakeExtensionRepoDetailsFetcher(
            "https://repo.example/extensions" to fetchedRepo,
        )
        val create = CreateExtensionRepo(repository, fetcher)

        val result = create.await(" HTTPS://REPO.example/extensions/index.min.json ")

        assertEquals(CreateExtensionRepo.Result.Success, result)
        assertEquals(listOf("https://repo.example/extensions"), fetcher.requests)
        assertEquals(fetchedRepo, repository.getRepo("https://repo.example/extensions"))
    }

    @Test
    fun awaitRejectsInvalidIndexUrlBeforeFetching() = runTest {
        val repository = FakeExtensionRepoRepository()
        val fetcher = FakeExtensionRepoDetailsFetcher()
        val create = CreateExtensionRepo(repository, fetcher)

        val result = create.await("http://repo.example/extensions/index.min.json")

        assertEquals(CreateExtensionRepo.Result.InvalidUrl, result)
        assertEquals(emptyList(), fetcher.requests)
        assertEquals(emptyList(), repository.getAll())
    }

    @Test
    fun awaitReportsDuplicateFingerprintFromRepositoryConflict() = runTest {
        val existingRepo = extensionRepo(
            baseUrl = "https://old.example/extensions",
            signingKeyFingerprint = "same-fingerprint",
        )
        val newRepo = extensionRepo(
            baseUrl = "https://repo.example/extensions",
            signingKeyFingerprint = "same-fingerprint",
        )
        val repository = FakeExtensionRepoRepository(
            initialRepos = listOf(existingRepo),
            failInserts = true,
        )
        val fetcher = FakeExtensionRepoDetailsFetcher(newRepo.baseUrl to newRepo)
        val create = CreateExtensionRepo(repository, fetcher)

        val result = create.await("https://repo.example/extensions/index.min.json")

        val duplicate = assertIs<CreateExtensionRepo.Result.DuplicateFingerprint>(result)
        assertEquals(existingRepo, duplicate.oldRepo)
        assertEquals(newRepo, duplicate.newRepo)
    }
}

class UpdateExtensionRepoTest {
    @Test
    fun awaitUpdatesRepoWhenFingerprintMatches() = runTest {
        val oldRepo = extensionRepo(
            baseUrl = "https://repo.example/extensions",
            name = "Old name",
            signingKeyFingerprint = "same-fingerprint",
        )
        val newRepo = oldRepo.copy(name = "New name")
        val repository = FakeExtensionRepoRepository(initialRepos = listOf(oldRepo))
        val fetcher = FakeExtensionRepoDetailsFetcher(oldRepo.baseUrl to newRepo)
        val update = UpdateExtensionRepo(repository, fetcher)

        update.await(oldRepo)

        assertEquals(newRepo, repository.getRepo(oldRepo.baseUrl))
    }

    @Test
    fun awaitSkipsRepoWhenFingerprintChanges() = runTest {
        val oldRepo = extensionRepo(
            baseUrl = "https://repo.example/extensions",
            signingKeyFingerprint = "old-fingerprint",
        )
        val newRepo = oldRepo.copy(signingKeyFingerprint = "new-fingerprint")
        val repository = FakeExtensionRepoRepository(initialRepos = listOf(oldRepo))
        val fetcher = FakeExtensionRepoDetailsFetcher(oldRepo.baseUrl to newRepo)
        val update = UpdateExtensionRepo(repository, fetcher)

        update.await(oldRepo)

        assertEquals(oldRepo, repository.getRepo(oldRepo.baseUrl))
    }

    @Test
    fun awaitAllowsLegacyNoFingerprintRepoToUpdateFingerprint() = runTest {
        val oldRepo = extensionRepo(
            baseUrl = "https://repo.example/extensions",
            signingKeyFingerprint = "NOFINGERPRINT-old",
        )
        val newRepo = oldRepo.copy(signingKeyFingerprint = "new-fingerprint")
        val repository = FakeExtensionRepoRepository(initialRepos = listOf(oldRepo))
        val fetcher = FakeExtensionRepoDetailsFetcher(oldRepo.baseUrl to newRepo)
        val update = UpdateExtensionRepo(repository, fetcher)

        update.await(oldRepo)

        assertEquals(newRepo, repository.getRepo(oldRepo.baseUrl))
    }
}

private fun extensionRepo(
    baseUrl: String,
    name: String = "Repo",
    shortName: String? = "Repo",
    website: String = "$baseUrl/",
    signingKeyFingerprint: String = "fingerprint",
): ExtensionRepo {
    return ExtensionRepo(
        baseUrl = baseUrl,
        name = name,
        shortName = shortName,
        website = website,
        signingKeyFingerprint = signingKeyFingerprint,
    )
}

private class FakeExtensionRepoDetailsFetcher(
    vararg repos: Pair<String, ExtensionRepo>,
) : ExtensionRepoDetailsFetcher {
    private val reposByBaseUrl = repos.toMap()
    val requests = mutableListOf<String>()

    override suspend fun fetchRepoDetails(repo: String): ExtensionRepo? {
        requests += repo
        return reposByBaseUrl[repo]
    }
}

private class FakeExtensionRepoRepository(
    initialRepos: List<ExtensionRepo> = emptyList(),
    private val failInserts: Boolean = false,
) : ExtensionRepoRepository {
    private val repos = MutableStateFlow(initialRepos.associateBy(ExtensionRepo::baseUrl))

    override fun subscribeAll(): Flow<List<ExtensionRepo>> {
        return repos.map { it.values.toList() }
    }

    override suspend fun getAll(): List<ExtensionRepo> {
        return repos.value.values.toList()
    }

    override suspend fun getRepo(baseUrl: String): ExtensionRepo? {
        return repos.value[baseUrl]
    }

    override suspend fun getRepoBySigningKeyFingerprint(fingerprint: String): ExtensionRepo? {
        return repos.value.values.firstOrNull { it.signingKeyFingerprint == fingerprint }
    }

    override fun getCount(): Flow<Int> {
        return repos.map { it.size }
    }

    override suspend fun insertRepo(
        baseUrl: String,
        name: String,
        shortName: String?,
        website: String,
        signingKeyFingerprint: String,
    ) {
        if (failInserts) {
            throw SaveExtensionRepoException(IllegalStateException("insert failed"))
        }
        require(baseUrl !in repos.value)
        val repo = ExtensionRepo(
            baseUrl = baseUrl,
            name = name,
            shortName = shortName,
            website = website,
            signingKeyFingerprint = signingKeyFingerprint,
        )
        repos.value = repos.value + (baseUrl to repo)
    }

    override suspend fun upsertRepo(
        baseUrl: String,
        name: String,
        shortName: String?,
        website: String,
        signingKeyFingerprint: String,
    ) {
        val repo = ExtensionRepo(
            baseUrl = baseUrl,
            name = name,
            shortName = shortName,
            website = website,
            signingKeyFingerprint = signingKeyFingerprint,
        )
        repos.value = repos.value + (baseUrl to repo)
    }

    override suspend fun replaceRepo(newRepo: ExtensionRepo) {
        repos.value = mapOf(newRepo.baseUrl to newRepo)
    }

    override suspend fun deleteRepo(baseUrl: String) {
        repos.value = repos.value - baseUrl
    }
}
