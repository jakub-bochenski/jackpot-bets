package com.acme.jackpotbets.option

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

@ConfigMapping(prefix = "metrics")
interface MetricsOptions {
    @WithDefault("false")
    fun enabled(): Boolean

    @WithDefault("8888")
    fun port(): Int

    @WithDefault("/metrics")
    fun path(): String
}
