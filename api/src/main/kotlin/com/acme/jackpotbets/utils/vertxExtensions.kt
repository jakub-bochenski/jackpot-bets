package com.acme.jackpotbets.utils

import io.vertx.core.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * A Vertx-safe version of [runBlocking] which prevents running on event loop thread.
 *
 * When called on event loop thread, throws [IllegalStateException].
 *
 * https://vertx.io/docs/vertx-lang-kotlin-coroutines/kotlin/#_coroutine_builders
 */
fun <T> vertxRunBlocking(block: suspend CoroutineScope.() -> T): T {
    check(!Context.isOnEventLoopThread()) { "Cannot execute on event loop thread" }
    @Suppress("ForbiddenMethodCall") // it is safe to call runBlocking when not on event loop
    return runBlocking { block() }
}
