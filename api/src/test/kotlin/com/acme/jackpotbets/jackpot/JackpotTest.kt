package com.acme.jackpotbets.jackpot

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.math.BigDecimal

class JackpotTest {
    @Test
    fun `Fixed jackpot applies constant percentage`() {
        val jackpot = Jackpot(
            initialPool = BigDecimal("1000.00"),
            currentPool = BigDecimal("11500.00"),
            contributionConfig = ContributionConfig.Fixed(
                percentage = BigDecimal("0.10"),
            ),
            rewardConfig = aRewardConfig
        )
        val contribution = jackpot.calculateContribution(BigDecimal("100.00"))
        expectThat(contribution)
            .isEqualTo(BigDecimal("10.0000"))
    }

    @Test
    fun `Variable jackpot at initial pool uses initial percentage`() {
        val initialPool = BigDecimal("1000.00")
        val jackpot = Jackpot(
            initialPool = initialPool,
            currentPool = initialPool,
            contributionConfig = ContributionConfig.Variable(
                variableInitialPercentage = BigDecimal("0.10"),
                variableDecayRate = BigDecimal("0.02"),
                minPercentage = BigDecimal("0.05"),
            ),
            rewardConfig = aRewardConfig
        )
        val contribution = jackpot.calculateContribution(BigDecimal("100.00"))
        // Expected calculation:
        // Initial 10% - (1 * 0%) = 10%
        // So 10% of 100.00 = 10.00
        expectThat(contribution)
            .isEqualTo(BigDecimal("10.000000"))
    }

    @Test
    fun `Variable jackpot reduces percentage as pool grows`() {
        val initialPool = BigDecimal("1000.00")
        val jackpot = Jackpot(
            initialPool = initialPool,
            currentPool = initialPool * BigDecimal("3.5"), // Pool grew 2.5x initial
            contributionConfig = ContributionConfig.Variable(
                variableInitialPercentage = BigDecimal("0.10"),
                variableDecayRate = BigDecimal("0.02"),
                minPercentage = BigDecimal("0.05"),
            ),
            rewardConfig = aRewardConfig
        )
        val contribution = jackpot.calculateContribution(BigDecimal("100.00"))
        // Expected calculation:
        // Initial 10% - (2.5 * 2%) = 10% - 5% = 5%
        // So 5% of 100.00 = 5.00
        expectThat(contribution)
            .isEqualTo(BigDecimal("5.0000"))
    }

    @Test
    fun `Variable jackpot respects minimum percentage`() {
        val initialPool = BigDecimal("1000.00")
        val jackpot = Jackpot(
            initialPool = initialPool,
            currentPool = initialPool * 10.toBigDecimal(),
            contributionConfig = ContributionConfig.Variable(
                variableInitialPercentage = BigDecimal("0.10"),
                variableDecayRate = BigDecimal("0.02"),
                minPercentage = BigDecimal("0.05")
            ),
            rewardConfig = aRewardConfig
        )
        val contribution = jackpot.calculateContribution(BigDecimal("100.00"))
        // Expected calculation:
        // Initial 10% - (10 * 2%) = 10% - 20% = -10%
        // But minimum is 5%, so should use that
        expectThat(contribution)
            .isEqualTo(BigDecimal("5.0000"))
    }

    @Test
    fun `Fixed reward has constant chance`() {
        val jackpot = Jackpot(
            initialPool = BigDecimal("1000.00"),
            currentPool = BigDecimal("5000.00"),
            contributionConfig = aContributionConfig,
            rewardConfig = RewardConfig.Fixed(
                chance = BigDecimal("0.05"),
            )
        )
        expectThat(jackpot.calculateRewardChance())
            .isEqualTo(BigDecimal("0.05"))
    }

    @Test
    fun `Variable reward at initial pool uses initial chance`() {
        val initialPool = BigDecimal("1000.00")
        val jackpot = Jackpot(
            initialPool = initialPool,
            currentPool = initialPool,
            contributionConfig = aContributionConfig,
            rewardConfig = RewardConfig.Variable(
                initialChance = BigDecimal("0.01"),
                maxPoolMultiplier = BigDecimal("5.00"),
            )
        )
        expectThat(jackpot.calculateRewardChance())
            .isEqualTo(BigDecimal("0.01"))
    }

    @Test
    fun `Variable reward increases chance as pool grows`() {
        val initialPool = BigDecimal("1000.00")
        val jackpot = Jackpot(
            initialPool = initialPool,
            currentPool = initialPool * BigDecimal("3.00"),
            contributionConfig = aContributionConfig,
            rewardConfig = RewardConfig.Variable(
                initialChance = BigDecimal("0.01"),
                maxPoolMultiplier = BigDecimal("5.00"),
            )
        )
        // Expected: At 3x initial pool
        // Progress = (3.0 - 1.0) / (5.0 - 1.0) = 2/4 = 0.5
        // Chance range = 1.0 - 0.01 = 0.99
        // Increase = 0.99 * 0.5 = 0.495
        // Final chance = 0.01 + 0.495 = 0.505 = 50.5%
        expectThat(jackpot.calculateRewardChance())
            .isEqualTo(BigDecimal("0.505000"))
    }

    @Test
    fun `Variable reward caps at 100 pct chance at max pool multiplier`() {
        val initialPool = BigDecimal("1000.00")
        val maxPoolMultiplier = BigDecimal("5.00")
        val jackpot = Jackpot(
            initialPool = initialPool,
            currentPool = initialPool * maxPoolMultiplier + BigDecimal("0.01"),
            contributionConfig = aContributionConfig,
            rewardConfig = RewardConfig.Variable(
                initialChance = BigDecimal("0.01"),
                maxPoolMultiplier = maxPoolMultiplier,
            )
        )
        expectThat(jackpot.calculateRewardChance())
            .isEqualTo(BigDecimal("1.000000")) // 100% chance
    }

    @Test
    fun `tryToWin returns null when random is above chance`() {
        val jackpot = aJackpot.copy(
            rewardConfig = RewardConfig.Fixed(
                chance = BigDecimal("0.01"),
            )
        )
        expectThat(jackpot.tryToWin { BigDecimal("0.02") })
            .isNull()
    }

    @Test
    fun `tryToWin returns current pool when random is below chance`() {
        val currentPool = BigDecimal("2000.00")
        val jackpot = aJackpot.copy(
            currentPool = currentPool,
            rewardConfig = RewardConfig.Fixed(
                chance = BigDecimal("0.01"),
            )
        )
        expectThat(jackpot.tryToWin { BigDecimal("0.005") })
            .isEqualTo(currentPool)
    }

    @Test
    fun `tryToWin returns null when random equals chance`() {
        val jackpot = aJackpot.copy(
            rewardConfig = RewardConfig.Fixed(
                chance = BigDecimal("0.01"),
            )
        )
        expectThat(jackpot.tryToWin { BigDecimal("0.01") })
            .isNull()
    }
}

private val aRewardConfig = RewardConfig.Fixed(
    chance = BigDecimal("0.05"),
)

private val aContributionConfig = ContributionConfig.Fixed(
    percentage = BigDecimal("0.10"),
)

private val aJackpot = Jackpot(
    initialPool = BigDecimal("1000.00"),
    currentPool = BigDecimal("2000.00"),
    contributionConfig = aContributionConfig,
    rewardConfig = aRewardConfig,
)
