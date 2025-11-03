package com.acme.jackpotbets.jooq

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import org.jooq.Field
import org.jooq.Record
import org.jooq.ResultQuery
import kotlin.reflect.KClass

suspend fun <R : Record> ResultQuery<R>.coFetchSet(): Set<R> = asFlow().toSet()

suspend fun <R : Record, T> ResultQuery<R>.coFetchSet(
    mapper: suspend (R) -> T
): Set<T> = asFlow().map(mapper).toSet()

suspend fun <T> ResultQuery<*>.coFetchSet(
    field: Field<T>
): Set<T> = asFlow().map { it[field] }.toSet()

suspend fun <T : Any> ResultQuery<*>.coFetchSet(
    field: Field<*>,
    type: KClass<T>
): Set<T> = asFlow().map { it[field, type.java] }.toSet()
