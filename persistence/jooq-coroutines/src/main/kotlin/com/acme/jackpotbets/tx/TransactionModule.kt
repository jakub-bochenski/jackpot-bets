package com.acme.jackpotbets.tx

import dagger.Binds
import dagger.Module

@Module
interface TransactionModule {
    @Binds
    fun bindTransactionOperation(tx: TransactionOperationImpl): TransactionOperation
}
