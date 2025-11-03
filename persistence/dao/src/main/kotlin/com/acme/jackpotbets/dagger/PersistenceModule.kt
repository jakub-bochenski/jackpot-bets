package com.acme.jackpotbets.dagger

import com.acme.jackpotbets.option.PersistenceOptions
import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.r2dbc.proxy.ProxyConnectionFactory
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL
import io.r2dbc.spi.ConnectionFactoryOptions.SSL
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import io.r2dbc.spi.ConnectionFactoryOptions.builder
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import javax.inject.Singleton

private val log = KotlinLogging.logger("com.acme.jackpotbets.db.sql")

@Module
object PersistenceModule {
    @Provides
    @Singleton
    fun configuration(dbOpt: PersistenceOptions, registry: MeterRegistry): Configuration {
        val connectionFactory = ConnectionFactories.get(
            dbOpt.toConnectionFactoryOptions()
        )

        val loggingConnectionFactory = ProxyConnectionFactory.builder(connectionFactory)
            .listener(MetricsExecutionListener(registry))
            .onAfterQuery { log.debug { queryFormatter.format(it) } }
            .build()

        return DefaultConfiguration()
            .set(SQLDialect.POSTGRES)
            .set(loggingConnectionFactory)
    }

    @Provides
    @Singleton
    fun dslContext(configuration: Configuration): DSLContext = configuration.dsl()
}

private val queryFormatter = QueryExecutionInfoFormatter.showAll()

fun PersistenceOptions.toConnectionFactoryOptions(): ConnectionFactoryOptions =
    builder()
        .option(DRIVER, "pool")
        .option(PROTOCOL, "postgresql")
        .option(HOST, host())
        .option(PORT, port())
        .option(DATABASE, database())
        .option(USER, username())
        .option(PASSWORD, password())
        .option(SSL, ssl())
        .build()
