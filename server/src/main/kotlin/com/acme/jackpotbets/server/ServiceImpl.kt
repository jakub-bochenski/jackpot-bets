package com.acme.jackpotbets.server

import com.acme.jackpotbets.option.ServerOptions
import com.acme.jackpotbets.utils.vertxRunBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import javax.inject.Inject
import javax.inject.Provider

private val log = KotlinLogging.logger { }

class ServiceImpl @Inject constructor(
    private val vertx: Vertx,
    private val verticleProvider: Provider<ServerVerticle>,
    private val options: ServerOptions
) : Service {
    override fun start() {
        val nVerticles = options.verticleCount()
            .orElse(Runtime.getRuntime().availableProcessors() * 2)

        log.info { "Deploying $nVerticles server verticles" }

        val serverVerticles = generateSequence { verticleProvider.get() }
            .take(nVerticles)
            .map { vertx.deployVerticle(it) }
            .map { future ->
                future
                    .onSuccess { log.info { "Server verticle $it deployed" } }
                    .onFailure { e -> log.error(e) { "Server verticle failed to deploy" } }
            }.toList()

        vertxRunBlocking {
            Future
                .join(serverVerticles)
                .onFailure {
                    log.error { "Failed to deploy all server verticles. Aborting." }
                    stop()
                }.coAwait()
        }
    }

    override fun stop() {
        vertxRunBlocking {
            vertx.close().coAwait()
        }
    }
}
