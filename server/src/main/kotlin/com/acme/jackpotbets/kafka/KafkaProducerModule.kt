package com.acme.jackpotbets.kafka

import com.acme.jackpotbets.option.KafkaOptions
import dagger.Module
import dagger.Provides
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kafka.client.producer.KafkaProducer
import java.util.Properties

@Module
object KafkaProducerModule {

    @Provides
    fun provideKafkaProducer(vertx: Vertx, options: KafkaOptions): KafkaProducer<Long, JsonObject> =
        KafkaProducer.create(
            vertx,
            Properties().apply {
                put("bootstrap.servers", options.bootstrapServers())
                put("acks", options.acks())
            },
            Long::class.javaObjectType,
            JsonObject::class.java,
        )
}
