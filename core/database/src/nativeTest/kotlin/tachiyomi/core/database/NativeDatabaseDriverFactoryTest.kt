package tachiyomi.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeDatabaseDriverFactoryTest {
    @Test
    fun enablesForeignKeyConstraints() {
        val driver = NativeDatabaseDriverFactory().create(
            schema = ForeignKeySchema,
            name = "chimahon-foreign-key-test-${Random.nextLong()}.db",
        )

        try {
            assertEquals(1L, driver.foreignKeyConstraintsEnabled())
            driver.execute(null, "INSERT INTO parent(id) VALUES (99)", 0, null).value
            driver.execute(null, "INSERT INTO child(id, parent_id) VALUES (1, 99)", 0, null).value
        } finally {
            driver.close()
        }
    }
}

private object ForeignKeySchema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        driver.execute(
            identifier = null,
            sql = "CREATE TABLE parent(id INTEGER NOT NULL PRIMARY KEY)",
            parameters = 0,
            binders = null,
        ).value
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE child(
                    id INTEGER NOT NULL PRIMARY KEY,
                    parent_id INTEGER NOT NULL REFERENCES parent(id)
                )
            """.trimIndent(),
            parameters = 0,
            binders = null,
        ).value
        return QueryResult.Value(Unit)
    }

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: app.cash.sqldelight.db.AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Value(Unit)
}

private fun SqlDriver.foreignKeyConstraintsEnabled(): Long {
    return executeQuery(
        identifier = null,
        sql = "PRAGMA foreign_keys",
        mapper = { cursor ->
            val hasRow = cursor.next().value
            QueryResult.Value(if (hasRow) cursor.getLong(0) ?: 0L else 0L)
        },
        parameters = 0,
        binders = null,
    ).value
}
