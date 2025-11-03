package com.acme.jackpotbets.jackpot

import java.math.BigDecimal
import java.time.LocalDateTime

data class Jackpot(
    val id: Long = Long.MIN_VALUE,
    val initialPool: BigDecimal,
    val currentPool: BigDecimal,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val contributionConfig: ContributionConfig,
    val rewardConfig: RewardConfig,
) {

    fun calculateContribution(stakeAmount: BigDecimal) =
        contributionConfig.calculateContribution(this, stakeAmount)

    fun calculateRewardChance() =
        rewardConfig.calculateRewardChance(this)

    /* Attempt to win the jackpot.
     * The generator function should produce a random BigDecimal in the range [0.0, 1.0).
     * If the generated value is less than the calculated reward chance, the current pool is won.
     * Otherwise, no reward is won (returns null).
     */
    fun tryToWin(generator: () -> BigDecimal): BigDecimal? =
        if (generator() < calculateRewardChance())
            currentPool
        else
            null
}
