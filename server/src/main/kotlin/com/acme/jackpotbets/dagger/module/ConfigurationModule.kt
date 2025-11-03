package com.acme.jackpotbets.dagger.module

import com.acme.jackpotbets.option.ApplicationOptions
import com.acme.jackpotbets.option.KafkaOptions
import com.acme.jackpotbets.option.MetricsOptions
import com.acme.jackpotbets.option.PersistenceOptions
import com.acme.jackpotbets.option.ServerOptions
import dagger.Module
import dagger.Provides
import io.smallrye.config.SmallRyeConfig
import io.smallrye.config.SmallRyeConfigBuilder

@Module
class ConfigurationModule {
    private val config: SmallRyeConfig = SmallRyeConfigBuilder()
        .addDefaultInterceptors()
        .addDefaultSources()
        .withMapping(ServerOptions::class.java)
        .withMapping(PersistenceOptions::class.java)
        .withMapping(ApplicationOptions::class.java)
        .withMapping(MetricsOptions::class.java)
        .withMapping(KafkaOptions::class.java)
        .build()

    @Provides
    fun serverOptions(): ServerOptions = config.getConfigMapping(ServerOptions::class.java)

    @Provides
    fun databaseOptions(): PersistenceOptions = config.getConfigMapping(PersistenceOptions::class.java)

    @Provides
    fun applicationOptions(): ApplicationOptions = config.getConfigMapping(ApplicationOptions::class.java)

    @Provides
    fun metricsOptions(): MetricsOptions = config.getConfigMapping(MetricsOptions::class.java)

    @Provides
    fun kafkaOptions(): KafkaOptions = config.getConfigMapping(KafkaOptions::class.java)
}
