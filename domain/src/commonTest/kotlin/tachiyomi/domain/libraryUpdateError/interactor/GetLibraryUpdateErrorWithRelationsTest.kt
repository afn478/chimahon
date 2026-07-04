package tachiyomi.domain.libraryUpdateError.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import tachiyomi.domain.libraryUpdateError.model.LibraryUpdateErrorWithRelations
import tachiyomi.domain.libraryUpdateError.repository.LibraryUpdateErrorWithRelationsRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class GetLibraryUpdateErrorWithRelationsTest {
    @Test
    fun subscribeAllForwardsRepositoryUpdates() = runTest {
        val firstError = libraryUpdateError(mangaId = 1, errorId = 10)
        val secondError = libraryUpdateError(mangaId = 2, errorId = 20)
        val repository = FakeLibraryUpdateErrorWithRelationsRepository(listOf(firstError))
        val getLibraryUpdateErrorWithRelations = GetLibraryUpdateErrorWithRelations(repository)

        assertEquals(listOf(firstError), getLibraryUpdateErrorWithRelations.subscribeAll().first())

        repository.errors.value = listOf(secondError)

        assertEquals(listOf(secondError), getLibraryUpdateErrorWithRelations.subscribeAll().first())
    }
}

private fun libraryUpdateError(
    mangaId: Long,
    errorId: Long,
): LibraryUpdateErrorWithRelations {
    return LibraryUpdateErrorWithRelations(
        mangaId = mangaId,
        mangaTitle = "Manga $mangaId",
        mangaSource = 100 + mangaId,
        favorite = true,
        mangaThumbnail = "thumbnail-$mangaId",
        coverLastModified = 1_000 + mangaId,
        errorId = errorId,
        messageId = 2_000 + errorId,
        lastUpdate = 3_000 + errorId,
    )
}

private class FakeLibraryUpdateErrorWithRelationsRepository(
    initialErrors: List<LibraryUpdateErrorWithRelations>,
) : LibraryUpdateErrorWithRelationsRepository {
    val errors = MutableStateFlow(initialErrors)

    override fun subscribeAll(): Flow<List<LibraryUpdateErrorWithRelations>> {
        return errors
    }
}
