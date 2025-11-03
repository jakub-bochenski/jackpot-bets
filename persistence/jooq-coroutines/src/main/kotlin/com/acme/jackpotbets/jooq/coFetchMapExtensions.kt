package com.acme.jackpotbets.jooq

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.jooq.Field
import org.jooq.Record
import org.jooq.ResultQuery

suspend fun <R : Record, K> ResultQuery<R>.coFetchMap(
    key: Field<K>
): Map<K, R> = asFlow().map { it[key] to it }.toList().toMap()

suspend fun <K, V> ResultQuery<*>.coFetchMap(
    key: Field<K>,
    value: Field<V>
): Map<K, V> = asFlow().map { it[key] to it[value] }.toList().toMap()

suspend fun <R : Record, K> ResultQuery<R>.coFetchMap(
    keyMapper: suspend (R) -> K
): Map<K, R> = asFlow().map { keyMapper(it) to it }.toList().toMap()

suspend fun <R : Record, K, V> ResultQuery<R>.coFetchMap(
    keyMapper: suspend (R) -> K,
    valueMapper: suspend (R) -> V
): Map<K, V> = asFlow().map { keyMapper(it) to valueMapper(it) }.toList().toMap()

suspend fun <R : Record, K, V> ResultQuery<R>.coFetchMap(
    key: Field<K>,
    valueMapper: suspend (R) -> V
): Map<K, V> = asFlow().map { it[key] to valueMapper(it) }.toList().toMap()
