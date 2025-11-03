package com.acme.jackpotbets.server

import com.acme.jackpotbets.exception.DomainException
import com.acme.jackpotbets.rest.BetRestHandler
import com.acme.jackpotbets.rest.RewardRestHandler
import com.acme.jackpotbets.rest.sendError
import com.acme.jackpotbets.rest.sendJson
import com.acme.jackpotbets.rest.sendMessage
import com.acme.jackpotbets.validation.ValidationHandler
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.http.HttpMethod.DELETE
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.PATCH
import io.vertx.core.http.HttpMethod.POST
import io.vertx.core.http.HttpMethod.PUT
import io.vertx.core.http.HttpServer
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.HttpException
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.validation.BadRequestException
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import org.jooq.exception.IntegrityConstraintViolationException
import javax.inject.Inject
import javax.inject.Provider

private val log = KotlinLogging.logger { }

class ServerVerticle @Inject constructor(
    httpServerProvider: Provider<HttpServer>,
    betRestHandlerProvider: Provider<BetRestHandler>,
    private val rewardRestHandler: RewardRestHandler,
    private val validationHandler: ValidationHandler,
) : CoroutineVerticle(), CoroutineRouterSupport {

    private val httpServer: HttpServer = httpServerProvider.get()
    private val betRestHandler: BetRestHandler = betRestHandlerProvider.get()

    override suspend fun start() {
        val router = Router.router(vertx)

        with(router.route()) {
            handler(ResponseContentTypeHandler.create())
            handler(
                CorsHandler.create()
                    .allowCredentials(true)
                    .allowedMethods(setOf(GET, POST, PUT, PATCH, DELETE))
            )
            handler(BodyHandler.create())
            handler(validationHandler::initValidator)
            failureHandler(::handleFailure)
        }

        router.get("/health")
            .handler { it.sendJson(200, JsonObject.of("status", "ok")) }

        router.post("/bets")
            .consumes(MIME_TYPE)
            .produces(MIME_TYPE)
            .coHandler { betRestHandler.postBet(it) }

        router.put("/reward/:betId")
            .produces(MIME_TYPE)
            .coHandler { rewardRestHandler.reward(it) }

        httpServer.requestHandler(router)

        httpServer
            .listen()
            .onComplete { log.info { "✅ Server started." } }
            .coAwait()
    }

    override suspend fun stop() {
        betRestHandler.close()
        httpServer.close().coAwait()
    }
}

private fun handleFailure(rc: RoutingContext) {
    if (rc.failed()) {
        when (val e = rc.failure()) {
            is DomainException -> rc.sendError(e.statusCode, e)
            is DecodeException, is MismatchedInputException -> rc.sendError(400, e)
            is HttpException -> rc.sendError(e.statusCode, e)
            is BadRequestException -> rc.sendError(400, e.cause ?: e)
            is IntegrityConstraintViolationException -> rc.sendError(400, e.cause ?: e)
            is JsonProcessingException -> rc.sendError(400, e)
            else -> if (rc.statusCode() == 404)
                rc.sendMessage(404, "Path not found")
            else
                rc.sendError(500, e, "Unhandled exception")
        }
        return
    }

    rc.next()
}

private const val MIME_TYPE = "application/json"
