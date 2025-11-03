package com.acme.jackpotbets.rest

import com.acme.jackpotbets.option.ApplicationOptions
import com.acme.jackpotbets.validation.bodyAsPojo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import io.vertx.kotlin.coroutines.coAwait
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import javax.inject.Inject

data class PlacedBet(
    val betId: Long,
    val userId: Long,
    val jackpotId: Long,
    @Positive
    val amount: BigDecimal,
)

class BetRestHandler @Inject constructor(
    val producer: KafkaProducer<Long, JsonObject>,
    val applicationOptions: ApplicationOptions,
) {

    suspend fun postBet(rc: RoutingContext) {
        val payload = rc.bodyAsPojo<PlacedBet>()

        producer
            .write(
                KafkaProducerRecord.create(
                    applicationOptions.kafkaTopic(),
                    payload.jackpotId, // partition by jackpotId to allow horizontal scaling of consumers
                    JsonObject.mapFrom(payload)
                ).also {
                    log.info { "Posting message with ${it.key()} to ${it.topic()}" }
                }
            )
            .coAwait()

        rc.sendJson(202, payload)
    }

    suspend fun close() {
        producer.close().coAwait()
    }
}

private val log = KotlinLogging.logger { }
