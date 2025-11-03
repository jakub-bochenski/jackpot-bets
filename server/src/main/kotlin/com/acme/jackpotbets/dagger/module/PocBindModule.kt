package com.acme.jackpotbets.dagger.module

import com.acme.jackpotbets.server.Service
import com.acme.jackpotbets.server.ServiceImpl
import dagger.Binds
import dagger.Module

@Module
interface PocBindModule {
    @Binds
    fun bindRESTServer(e: ServiceImpl): Service
}
