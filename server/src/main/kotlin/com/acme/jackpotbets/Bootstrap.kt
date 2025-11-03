package com.acme.jackpotbets

import com.acme.jackpotbets.db.migration.runLiquibase
import com.acme.jackpotbets.option.PersistenceOptions
import com.acme.jackpotbets.option.ServerOptions
import com.acme.jackpotbets.server.Service
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dagger.Lazy
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.json.jackson.DatabindCodec
import liquibase.Liquibase
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

@Singleton
class Bootstrap @Inject constructor(
    private val service: Service,
    private val liquibase: Lazy<Liquibase>,
    private val serverOptions: ServerOptions,
    private val persistenceOptions: PersistenceOptions,
) {
    fun start() {
        DatabindCodec
            .mapper()
            .registerKotlinModule()

        runLiquibase {
            liquibase.get().use {
                it.update(persistenceOptions.liquibaseContexts())
            }
        }

        service.start()

        log.info { "REST server started" }
        log.info { "Now connect to http://${serverOptions.host()}:${serverOptions.port()}/health" }
    }
}
