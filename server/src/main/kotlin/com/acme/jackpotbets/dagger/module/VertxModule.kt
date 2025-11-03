package com.acme.jackpotbets.dagger.module

import com.acme.jackpotbets.option.MetricsOptions
import com.acme.jackpotbets.option.ServerOptions
import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.client.WebClient
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions
import io.vertx.micrometer.backends.BackendRegistries
import javax.inject.Singleton

private val log = KotlinLogging.logger { }

@Module
object VertxModule {

    @Provides
    fun vertxOptions(metricsOpt: MetricsOptions): VertxOptions = VertxOptions().apply {
        preferNativeTransport = true
        metricsOpt.takeIf { it.enabled() }?.let { options -> metricsOptions = options.toVertxOptions() }
    }

    private fun MetricsOptions.toVertxOptions() = MicrometerMetricsOptions().apply {
        isEnabled = true
        isJvmMetricsEnabled = true
        prometheusOptions = VertxPrometheusOptions().apply {
            isEnabled = true
            setStartEmbeddedServer(true)
            embeddedServerOptions = HttpServerOptions().apply {
                port = port()
            }
            embeddedServerEndpoint = path()
        }
    }

    @Provides
    fun meterRegistry(): MeterRegistry = BackendRegistries.getDefaultNow() ?: SimpleMeterRegistry()

    @Provides
    @Singleton
    fun vertx(options: VertxOptions): Vertx = Vertx
        .vertx(options)
        .also { vertx ->
            if (vertx.isNativeTransportEnabled)
                log.info { "Native transport has been enabled." }
            else
                log.warn { "Native transport has not been enabled. Maybe you are not running this on x86_64 Linux?" }
        }

    @Provides
    fun eventBus(vertx: Vertx): EventBus = vertx.eventBus()

    /**
     * We need a new instance for each verticle
     */
    @Provides
    fun httpServer(vertx: Vertx, serverOpt: ServerOptions): HttpServer {
        val options = HttpServerOptions().apply {
            host = serverOpt.host()
            port = serverOpt.port()
            isCompressionSupported = true
            isHandle100ContinueAutomatically = true
            isTcpFastOpen = true
            isTcpNoDelay = true
            isTcpQuickAck = true
        }
        return vertx.createHttpServer(options)
    }

    @Provides
    @Singleton
    fun httpClient(vertx: Vertx): WebClient = WebClient.create(vertx)
}
