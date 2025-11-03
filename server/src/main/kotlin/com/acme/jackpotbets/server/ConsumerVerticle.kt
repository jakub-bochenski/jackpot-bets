package com.acme.jackpotbets.server

import com.acme.jackpotbets.bet.BetService
import com.acme.jackpotbets.option.ApplicationOptions
import com.acme.jackpotbets.rest.PlacedBet
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.json.JsonObject
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import javax.inject.Inject
import javax.inject.Provider

class ConsumerVerticle @Inject constructor(
    kafkaConsumerProvider: Provider<KafkaConsumer<Long, JsonObject>>,
    val betService: BetService,
    val applicationOptions: ApplicationOptions,
) : CoroutineVerticle(), CoroutineKafkaSupport {

    private val consumer: KafkaConsumer<Long, JsonObject> = kafkaConsumerProvider.get()

    override suspend fun start() {
        consumer.coHandler { record ->
            log.info {
                "Consumed record with key=${record.key()} and value=${record.value()}, " +
                    "partition=${record.partition()}, offset=${record.offset()}"
            }

            try {
                betService.placeBet(
                    record
                        .value()
                        .mapTo(PlacedBet::class.java)
                )
            } catch (ex: Exception) {
                log.error(ex) { "Failed to process record with key=${record.key()} and value=${record.value()}" }
                // there should be a retry/DLQ mechanism here -- for simplicity, we just log the error
            }

            consumer.commit().coAwait()
        }

        consumer.subscribe(applicationOptions.kafkaTopic()).coAwait()
    }

    override suspend fun stop() {
        consumer.close().coAwait()
    }
}

private val log = KotlinLogging.logger { }
