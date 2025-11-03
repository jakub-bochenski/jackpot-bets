package com.acme.jackpotbets.option

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

@ConfigMapping(prefix = "kafka")
interface KafkaOptions {
    fun bootstrapServers(): String

    @WithDefault("all")
    fun acks(): String
}
