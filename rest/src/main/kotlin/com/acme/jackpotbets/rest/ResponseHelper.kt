@file:JvmName("ResponseHelper")

package com.acme.jackpotbets.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.RoutingContext

private val log = KotlinLogging.logger {}

fun RoutingContext.sendError(code: Int, e: Throwable, message: String? = e.message) {
    if (code in 400..499)
        log.debug(e) { message ?: "Bad request" }
    else log.error(e) { message ?: "Error in request processing" }

    sendMessage(code, message ?: "Unknown error")
}

fun RoutingContext.sendMessage(code: Int, msg: String?) {
    val json = JsonObject()
    json.put("message", msg)
    sendJson(code, json)
}

fun RoutingContext.sendJson(code: Int, payload: Any) {
    response().statusCode = code
    end(DatabindCodec.mapper().writeValueAsString(payload))
}

fun RoutingContext.noContent() {
    response().setStatusCode(204).end()
}
