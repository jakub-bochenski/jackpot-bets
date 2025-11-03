@file:Suppress("SpacingAroundColon", "NoUnusedImports", "Wrapping")

package com.acme.jackpotbets.jackpot

import java.math.BigDecimal
import java.time.LocalDateTime

sealed class ContributionConfig {
    abstract val id: Long
    abstract val createdAt: LocalDateTime

    abstract fun calculateContribution(jackpot: Jackpot, stakeAmount: BigDecimal): BigDecimal

    data class Fixed(
        override val id: Long = Long.MIN_VALUE,
        override val createdAt: LocalDateTime = LocalDateTime.now(),
        val percentage: BigDecimal,
    ) : ContributionConfig() {
        override fun calculateContribution(jackpot: Jackpot, stakeAmount: BigDecimal) =
            stakeAmount * percentage
    }

    data class Variable(
        override val id: Long = Long.MIN_VALUE,
        override val createdAt: LocalDateTime = LocalDateTime.now(),
        val variableInitialPercentage: BigDecimal,
        val variableDecayRate: BigDecimal,
        val minPercentage: BigDecimal,
    ) : ContributionConfig() {
        // Calculate effective percentage:
        // - Start with initial percentage (e.g. 10%)
        // - Subtract decay based on how much pool has grown:
        //   * currentPool/initialPool - 1 gives relative pool growth (e.g. 2.5x means pool grew 250%)
        //   * above is coerced to minimum of 0 to avoid negative growth
        //   * multiply by decay rate to determine percentage point reduction
        // - Never go below minimum percentage
        override fun calculateContribution(jackpot: Jackpot, stakeAmount: BigDecimal) =
            with(jackpot) {
                stakeAmount * maxOf(
                    minPercentage,
                    variableInitialPercentage - (
                        maxOf(currentPool / initialPool - 1.toBigDecimal(), 0.toBigDecimal()) *
                            variableDecayRate
                        )
                )
            }
    }
}
