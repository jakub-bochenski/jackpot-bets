package com.acme.jackpotbets.server

import io.vertx.core.impl.ContextInternal
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface CoroutineKafkaSupport : CoroutineScope {
    fun <K, V> KafkaConsumer<K, V>.coHandler(
        context: CoroutineContext = EmptyCoroutineContext,
        handler: suspend (record: KafkaConsumerRecord<K, V>) -> Unit
    ): KafkaConsumer<K, V> = handler {
        launch((ContextInternal.current()?.dispatcher() ?: EmptyCoroutineContext) + context) {
            handler(it)
            // TODO: exception handling?
        }
    }
}
