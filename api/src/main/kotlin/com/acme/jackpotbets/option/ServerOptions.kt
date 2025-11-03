package com.acme.jackpotbets.option

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.util.Optional

@ConfigMapping(prefix = "server")
interface ServerOptions {
    @WithDefault("8080")
    fun port(): Int

    fun verticleCount(): Optional<Int>

    @WithDefault("0.0.0.0")
    fun host(): String
}
