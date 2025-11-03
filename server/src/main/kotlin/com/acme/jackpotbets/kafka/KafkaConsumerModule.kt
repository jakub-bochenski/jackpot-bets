package com.acme.jackpotbets.kafka

import com.acme.jackpotbets.option.KafkaOptions
import dagger.Module
import dagger.Provides
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kafka.client.consumer.KafkaConsumer
import java.util.Properties

@Module
object KafkaConsumerModule {

    @Provides
    fun provideKafkaConsumer(vertx: Vertx, options: KafkaOptions): KafkaConsumer<Long, JsonObject> =
        KafkaConsumer.create(
            vertx,
            Properties().apply {
                put("bootstrap.servers", options.bootstrapServers())
                put("group.id", "my_group")
                put("auto.offset.reset", "earliest")
                put("enable.auto.commit", "false")

                put("reconnect.backoff.ms", "1000")
                put("reconnect.backoff.max.ms", "10000")
                put("retry.backoff.ms", "1000")
                put("request.timeout.ms", "30000")
            },
            Long::class.javaObjectType,
            JsonObject::class.java
        )
}
