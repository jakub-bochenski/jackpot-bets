package com.acme.jackpotbets.jooq

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.jooq.Field
import org.jooq.Record
import org.jooq.ResultQuery
import kotlin.reflect.KClass

suspend fun <R : Record> ResultQuery<R>.coFetch(): List<R> = asFlow().toList()

suspend fun <R : Record, T> ResultQuery<R>.coFetch(
    mapper: suspend (R) -> T
): List<T> = asFlow().map(mapper).toList()

suspend fun <T> ResultQuery<*>.coFetch(field: Field<T>): List<T> = asFlow().map { it[field] }.toList()

suspend fun <T : Any> ResultQuery<*>.coFetch(
    field: Field<*>,
    type: KClass<T>
): List<T> = asFlow().map { it[field, type.java] }.toList()

suspend inline fun <reified T : Any> ResultQuery<*>.coFetchInto(): List<T> = coFetch { it.into(T::class.java) }
