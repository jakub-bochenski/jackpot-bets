package com.acme.jackpotbets.tx

import org.jooq.DSLContext
import org.jooq.kotlin.coroutines.transactionCoroutine
import javax.inject.Inject

fun interface Transactional {
    fun dsl(): DSLContext
}

interface TransactionOperation {
    suspend operator fun <T> invoke(block: suspend Transactional.() -> T): T
}

class TransactionOperationImpl @Inject constructor(private val dslContext: DSLContext) : TransactionOperation {
    override suspend fun <T> invoke(block: suspend Transactional.() -> T): T =
        dslContext.transactionCoroutine { ctx ->
            Transactional { ctx.dsl() }.block()
        }
}
