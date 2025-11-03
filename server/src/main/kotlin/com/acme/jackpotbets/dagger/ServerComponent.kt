package com.acme.jackpotbets.dagger

import com.acme.jackpotbets.Bootstrap
import com.acme.jackpotbets.dagger.module.ConfigurationModule
import com.acme.jackpotbets.dagger.module.PocBindModule
import com.acme.jackpotbets.dagger.module.VertxModule
import com.acme.jackpotbets.db.migration.LiquibaseModule
import com.acme.jackpotbets.tx.TransactionModule
import com.acme.jackpotbets.validation.ValidationModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ConfigurationModule::class,
        VertxModule::class,
        PocBindModule::class,
        LiquibaseModule::class,
        PersistenceModule::class,
        TransactionModule::class,
        ValidationModule::class,
    ]
)
interface ServerComponent {
    @Component.Builder
    interface Builder {
        fun build(): ServerComponent
    }

    fun boot(): Bootstrap
}
