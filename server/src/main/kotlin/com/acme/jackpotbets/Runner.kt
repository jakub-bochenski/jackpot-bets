@file:JvmName("Runner")

package com.acme.jackpotbets

import com.acme.jackpotbets.dagger.DaggerServerComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.bridge.SLF4JBridgeHandler
import kotlin.system.exitProcess

private val log = KotlinLogging.logger { }

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        log.error { "Unexpected argument: $args" }
        exitProcess(1)
    }

    // liquibase :(
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    log.info { "Starting Jackpot Bets" }

    // Inject the options and build the dagger dependency graph
    val serverComponent = DaggerServerComponent
        .builder()
        .build()

    // Use the bootstrap to startup the individual components in the right order.
    serverComponent.boot().start()
}
