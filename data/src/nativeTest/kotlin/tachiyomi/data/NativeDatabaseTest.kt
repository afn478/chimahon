package tachiyomi.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import tachiyomi.core.database.NativeDatabaseDriverFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeDatabaseTest {
    @Test
    fun usesTheProductionSchemaThroughTheNativeHandler() = runBlocking {
        val driver = NativeDatabaseDriverFactory().create(
            schema = Database.Schema,
            name = "chimahon-native-database-test-${Random.nextLong()}.db",
        )

        try {
            val database = Database(
                driver = driver,
                mangasAdapter = Mangas.Adapter(
                    genreAdapter = StringListColumnAdapter,
                ),
            )
            val handler = NativeDatabaseHandler(database, driver)

            assertTrue(handler.awaitList { mangasQueries.getAllManga() }.isEmpty())

            handler.await(inTransaction = true) {
                mangasQueries.insert(
                    source = 1,
                    url = "/native",
                    artist = null,
                    author = null,
                    description = null,
                    genre = listOf("KMP", "Native"),
                    title = "Native manga",
                    status = 0,
                    thumbnailUrl = null,
                    favorite = true,
                    lastUpdate = null,
                    nextUpdate = null,
                    initialized = false,
                    viewerFlags = 0,
                    chapterFlags = 0,
                    coverLastModified = 0,
                    dateAdded = 0,
                    updateStrategy = 0,
                    calculateInterval = 0,
                    version = 0,
                    notes = "",
                )
            }

            val inserted = handler.awaitOne {
                mangasQueries.getMangaByUrlAndSource("/native", 1)
            }

            assertEquals("Native manga", inserted.title)
            assertEquals(listOf("KMP", "Native"), inserted.genre)
            assertEquals(
                listOf(inserted),
                handler.subscribeToList { mangasQueries.getAllManga() }.first(),
            )
        } finally {
            driver.close()
        }
    }
}
