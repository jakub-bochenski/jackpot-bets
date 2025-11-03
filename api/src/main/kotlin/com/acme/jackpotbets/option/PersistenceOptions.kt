package com.acme.jackpotbets.option

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

@ConfigMapping(prefix = "database")
interface PersistenceOptions {
    @WithDefault("localhost")
    fun host(): String

    @WithDefault("5432")
    fun port(): Int

    @WithDefault("jackpot_bets")
    fun database(): String

    @WithDefault("sa")
    fun password(): String

    @WithDefault("sa")
    fun username(): String

    @WithDefault("qa")
    fun liquibaseContexts(): String

    @WithDefault("false")
    fun ssl(): Boolean
}
