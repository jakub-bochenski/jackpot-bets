package com.acme.jackpotbets.jooq

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.jooq.Field
import org.jooq.Record
import org.jooq.ResultQuery

suspend fun <R : Record, K> ResultQuery<R>.coFetchGroups(
    key: Field<K>
): Map<K, List<R>> = asFlow().map { it[key] to it }.toList()
    .groupBy({ it.first }) { it.second }

suspend fun <K, V> ResultQuery<*>.coFetchGroups(
    key: Field<K>,
    value: Field<V>
): Map<K, List<V>> = asFlow().map { it[key] to it[value] }.toList()
    .groupBy({ it.first }) { it.second }

suspend fun <R : Record, K> ResultQuery<R>.coFetchGroups(
    keyMapper: suspend (R) -> K
): Map<K, List<R>> = asFlow().map { keyMapper(it) to it }.toList()
    .groupBy({ it.first }) { it.second }

suspend fun <R : Record, K, V> ResultQuery<R>.coFetchGroups(
    keyMapper: suspend (R) -> K,
    valueMapper: suspend (R) -> V
): Map<K, List<V>> = asFlow().map { keyMapper(it) to valueMapper(it) }.toList()
    .groupBy({ it.first }) { it.second }

suspend fun <R : Record, K, V> ResultQuery<R>.coFetchGroups(
    key: Field<K>,
    valueMapper: suspend (R) -> V
): Map<K, List<V>> = asFlow().map { it[key] to valueMapper(it) }.toList()
    .groupBy({ it.first }) { it.second }
