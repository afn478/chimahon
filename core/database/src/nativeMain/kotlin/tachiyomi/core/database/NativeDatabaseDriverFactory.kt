package tachiyomi.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

class NativeDatabaseDriverFactory : DatabaseDriverFactory {
    override fun create(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        name: String,
    ): SqlDriver {
        return NativeSqliteDriver(
            schema = schema,
            name = name,
            onConfiguration = { configuration ->
                configuration.copy(
                    extendedConfig = DatabaseConfiguration.Extended(
                        foreignKeyConstraints = true,
                    ),
                )
            },
        )
    }
}
