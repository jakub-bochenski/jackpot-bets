package com.acme.jackpotbets.jackpot

import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.time.LocalDateTime

sealed class RewardConfig {
    abstract val id: Long
    abstract val createdAt: LocalDateTime

    abstract fun calculateRewardChance(jackpot: Jackpot): BigDecimal

    data class Fixed(
        override val id: Long = Long.MIN_VALUE,
        val chance: BigDecimal,
        override val createdAt: LocalDateTime = LocalDateTime.now(),
    ) : RewardConfig() {
        override fun calculateRewardChance(jackpot: Jackpot): BigDecimal = chance
    }

    data class Variable(
        override val id: Long = Long.MIN_VALUE,
        val initialChance: BigDecimal,
        val maxPoolMultiplier: BigDecimal,
        override val createdAt: LocalDateTime = LocalDateTime.now(),
    ) : RewardConfig() {
        // Calculate chance based on current pool:
        // - If currentPool <= initialPool, use initialChance
        // - As currentPool grows towards (initialPool * maxPoolMultiplier),
        //   linearly increase chance from initialChance to 1.0
        override fun calculateRewardChance(jackpot: Jackpot): BigDecimal =
            with(jackpot) {
                val poolMultiplier = currentPool / initialPool
                if (poolMultiplier <= ONE)
                    return initialChance

                val scaledProgress = (poolMultiplier - ONE) / (maxPoolMultiplier - ONE)
                val progress = minOf(scaledProgress, ONE)
                val chanceRange = ONE - initialChance

                initialChance + (chanceRange * progress)
            }
    }
}
