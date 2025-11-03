package com.acme.jackpotbets.db.test

import com.acme.jackpotbets.db.migration.liquibaseFrom
import com.acme.jackpotbets.db.migration.runLiquibase
import com.acme.jackpotbets.tx.TransactionOperation
import com.acme.jackpotbets.tx.TransactionOperationImpl
import com.acme.jackpotbets.tx.Transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import io.r2dbc.proxy.ProxyConnectionFactory
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import io.r2dbc.spi.ConnectionFactoryOptions.builder
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.test.runTest
import org.jooq.DSLContext
import org.jooq.RowCountQuery
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

/**
 * Execute test against live DB.
 *
 * Within test block there is available [TransactionOperation] which allows
 * accessing [DSLContext] within a transaction.
 */
fun persistenceTest(block: suspend (TransactionOperation) -> Unit) {
    runTest {
        block(TransactionOperationImpl(dslContext))
    }
}

/**
 * Execute test against live DB within a transaction.
 *
 * DB transaction is started before test method and rolled back after the test method.
 */
fun transactionalTest(block: suspend Transactional.() -> Unit) {
    runTest {
        val tx = TransactionOperationImpl(dslContext)
        tx {
            block()
            // https://github.com/jOOQ/jOOQ/issues/18014
            (dsl().rollback() as RowCountQuery).awaitFirstOrNull()
        }
    }
}

private val dslContext: DSLContext by lazy {
    with(postgreSQLContainer) {
        builder()
            .option(DRIVER, "postgresql")
            .option(PROTOCOL, "postgresql")
            .option(HOST, host)
            .option(PORT, firstMappedPort)
            .option(DATABASE, databaseName)
            .option(USER, username)
            .option(PASSWORD, password)
            .build()
    }
        .run(ConnectionFactories::get)
        .run(ProxyConnectionFactory::builder)
        .onAfterQuery { log.debug { queryFormatter.format(it) } }
        .build()
        .run {
            DefaultConfiguration()
                .set(POSTGRES)
                .set(this)
        }
        .run(DSL::using)
}

private val queryFormatter = QueryExecutionInfoFormatter.showAll()
private val log = KotlinLogging.logger("com.acme.jackpotbets.db.sql")

private val postgreSQLContainer: PostgreSQLContainer<Nothing> by lazy {
    PostgreSQLContainer<Nothing>(postgresImage).apply {
        withDatabaseName("integration-test")
        withUsername("integration-test")
        withPassword("integration-test")
        start()

        DriverManager.getConnection(
            jdbcUrl,
            username,
            password
        ).use { connection ->
            runLiquibase {
                liquibaseFrom(connection)
                    .update("integration-test")
            }
        }
    }
}

private val postgresImage = System
    .getProperty("postgres.docker.image", "postgres:16-alpine")
    .run(DockerImageName::parse)
    .asCompatibleSubstituteFor("postgres")
