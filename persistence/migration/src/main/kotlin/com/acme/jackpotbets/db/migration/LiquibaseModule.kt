package com.acme.jackpotbets.db.migration

import com.acme.jackpotbets.option.PersistenceOptions
import dagger.Module
import dagger.Provides
import liquibase.Liquibase
import liquibase.Scope
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.ui.LoggerUIService
import java.sql.Connection
import java.sql.DriverManager
import javax.inject.Singleton

@Module
class LiquibaseModule {
    @Provides
    @Singleton
    fun provideLiquibase(db: PersistenceOptions) = liquibaseFrom(
        DriverManager.getConnection(
            "jdbc:postgresql://${db.host()}:${db.port()}/${db.database()}",
            db.username(),
            db.password()
        )
    )
}

/**
 * Workaround for https://github.com/liquibase/liquibase/issues/3651
 */
fun runLiquibase(block: () -> Unit) =
    Scope.child(
        mapOf(Scope.Attr.ui.name to LoggerUIService()),
        block
    )

fun liquibaseFrom(connection: Connection) = Liquibase(
    "db/changelog.xml",
    ClassLoaderResourceAccessor(),
    DatabaseFactory
        .getInstance()
        .findCorrectDatabaseImplementation(
            JdbcConnection(connection)
        )
)
