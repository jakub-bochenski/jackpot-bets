package com.acme.jackpotbets.option

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

@ConfigMapping(prefix = "app")
interface ApplicationOptions {
    @WithDefault("jackpot-bets")
    fun kafkaTopic(): String
}
