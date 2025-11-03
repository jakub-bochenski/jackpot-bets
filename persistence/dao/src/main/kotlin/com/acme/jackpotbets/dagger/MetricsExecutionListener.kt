package com.acme.jackpotbets.dagger

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.r2dbc.proxy.core.MethodExecutionInfo
import io.r2dbc.proxy.core.QueryExecutionInfo
import io.r2dbc.proxy.core.ValueStore
import io.r2dbc.proxy.listener.ProxyMethodExecutionListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinDuration

/**
 * Implementation based on
 * https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/listener-example/src/main/java/io/r2dbc/examples/MetricsExecutionListener.java
 */
internal class MetricsExecutionListener(
    private val registry: MeterRegistry,
    private val slowQueryThreshold: Duration = Duration.INFINITE
) : ProxyMethodExecutionListener {
    override fun beforeCreateOnConnectionFactory(methodExecutionInfo: MethodExecutionInfo) {
        val sample: Timer.Sample = Timer.start()
        methodExecutionInfo.valueStore.put("connectionCreate", sample)
    }

    override fun afterCreateOnConnectionFactory(methodExecutionInfo: MethodExecutionInfo) {
        val sample = methodExecutionInfo.valueStore.getValue<Timer.Sample>("connectionCreate") ?: return

        val timer: Timer = Timer.builder(metricName("connection"))
            .description("Time to acquire a connection")
            .tags("event", "create")
            .register(registry)

        sample.stop(timer)
    }

    override fun afterCommitTransactionOnConnection(methodExecutionInfo: MethodExecutionInfo) {
        val counter: Counter = Counter.builder(metricName("transaction"))
            .description("Number of transactions")
            .tags("event", "commit")
            .register(registry)
        counter.increment()
    }

    override fun afterRollbackTransactionOnConnection(methodExecutionInfo: MethodExecutionInfo) {
        incrementRollbackCounter()
    }

    override fun afterRollbackTransactionToSavepointOnConnection(methodExecutionInfo: MethodExecutionInfo) {
        incrementRollbackCounter()
    }

    private fun incrementRollbackCounter() {
        val counter: Counter = Counter.builder(metricName("transaction"))
            .description("Number of transactions")
            .tags("event", "rollback")
            .register(registry)
        counter.increment()
    }

    override fun afterExecuteOnBatch(queryExecutionInfo: QueryExecutionInfo) {
        afterExecuteQuery(queryExecutionInfo)
    }

    override fun afterExecuteOnStatement(queryExecutionInfo: QueryExecutionInfo) {
        afterExecuteQuery(queryExecutionInfo)
    }

    private fun afterExecuteQuery(queryExecutionInfo: QueryExecutionInfo) {
        val success: Counter = Counter.builder(metricName("query"))
            .description("Num of executed queries")
            .register(registry)
        success.increment()

        // slow query
        if (this.slowQueryThreshold - queryExecutionInfo.executeDuration.toKotlinDuration() < 0.milliseconds) {
            val slowQueryCounter: Counter = Counter.builder(metricName("query.slow"))
                .description("Slow query count that took more than threshold")
                .register(registry)
            slowQueryCounter.increment()
        }
    }

    private inline fun <reified T> ValueStore.getValue(key: String): T? = get(key, T::class.java)

    private fun metricName(s: String) = "r2dbc.$s"
}
