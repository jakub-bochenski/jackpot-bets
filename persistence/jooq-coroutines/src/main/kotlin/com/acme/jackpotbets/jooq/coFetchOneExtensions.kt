package com.acme.jackpotbets.jooq

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.Field
import org.jooq.Record
import org.jooq.ResultQuery
import org.jooq.exception.NoDataFoundException
import org.jooq.exception.TooManyRowsException
import org.reactivestreams.Publisher

suspend fun <R : Record> ResultQuery<R>.coFetchOne(): R? = awaitFirstOrNull()

suspend fun <T> ResultQuery<*>.coFetchOne(field: Field<T>): T? = awaitFirstOrNull()?.get(field)

suspend fun <R : Record, T : Any> ResultQuery<R>.coFetchOne(
    mapper: suspend (R) -> T
): T? = awaitFirstOrNull()?.let { mapper(it) }

suspend inline fun <reified T : Any> ResultQuery<*>.coFetchOneInto(): T? = awaitFirstOrNull()?.into(T::class.java)

suspend fun <R : Record> ResultQuery<R>.coFetchSingle(): R = checkedAwaitSingle()

suspend fun <T> ResultQuery<*>.coFetchSingle(field: Field<T>): T = checkedAwaitSingle().get(field)

suspend fun <R : Record, T : Any> ResultQuery<R>.coFetchSingle(
    mapper: suspend (R) -> T
): T = mapper(checkedAwaitSingle())

suspend inline fun <reified T : Any> ResultQuery<*>.coFetchSingleInto(): T = coFetchSingle().into(T::class.java)

private class ReactiveNoDataFoundException(override val cause: Throwable) : NoDataFoundException()
private class ReactiveTooManyRowsException(override val cause: Throwable) : TooManyRowsException()

private suspend fun <T> Publisher<T>.checkedAwaitSingle() = try {
    awaitSingle()
} catch (e: NoSuchElementException) {
    throw ReactiveNoDataFoundException(e)
} catch (e: IllegalArgumentException) {
    throw ReactiveTooManyRowsException(e)
}
