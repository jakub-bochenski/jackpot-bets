@file:Suppress("UnusedImports")

package com.acme.jackpotbets.db

import com.acme.jackpotbets.db.test.transactionalTest
import com.acme.jackpotbets.jackpot.Jackpot
import com.acme.jackpotbets.jooq.coFetchSingle
import com.acme.jackpotbets.model.jooq.enums.ContributionType.CONTRIBUTION
import com.acme.jackpotbets.model.jooq.enums.ContributionType.REWARD
import com.acme.jackpotbets.model.jooq.enums.ContributionType.UNREWARDED
import com.acme.jackpotbets.model.jooq.tables.references.CONTRIBUTION_CONFIG
import com.acme.jackpotbets.model.jooq.tables.references.JACKPOT
import com.acme.jackpotbets.model.jooq.tables.references.REWARD_CONFIG
import com.acme.jackpotbets.tx.Transactional
import org.jooq.exception.IntegrityConstraintViolationException
import org.junit.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.math.BigDecimal
import javax.inject.Inject

class JackpotDaoTest @Inject constructor() {
    private val jackpotDao = JackpotDao()

    @Test
    fun `findById returns jackpot with correct configs`() = transactionalTest {
        val expectedInitialPool = BigDecimal("2000.00")
        val expectedChance = BigDecimal("0.05")

        val jackpot = createTestJackpot(
            initialPool = expectedInitialPool,
            chance = expectedChance
        )

        val foundJackpot = jackpotDao.findById(jackpot.id)

        expectThat(foundJackpot)
            .isNotNull()
            .and {
                get { id }.isEqualTo(jackpot.id)
                get { initialPool }.isEqualTo(expectedInitialPool)
                get { currentPool }.isEqualTo(expectedInitialPool)
            }
    }

    @Test
    fun `findByBetId returns jackpot for bet`() = transactionalTest {
        val betId = 123L
        val userId = 456L
        val initialPool = BigDecimal("3000.00")
        val stakeAmount = BigDecimal("100.00")
        val contributionAmount = BigDecimal("2.00")
        val expectedCurrentPool = initialPool + contributionAmount

        val jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stakeAmount,
            contributionAmount = contributionAmount
        )

        val foundJackpot = jackpotDao.findByBetId(betId)

        expectThat(foundJackpot)
            .isNotNull()
            .and {
                get { id }.isEqualTo(jackpot.id)
                get { initialPool }.isEqualTo(initialPool)
                get { currentPool }.isEqualTo(expectedCurrentPool)
            }
    }

    @Test
    fun `findByBetId returns null for non-existent bet`() = transactionalTest {
        expectThat(jackpotDao.findByBetId(999L)).isNull()
    }

    @Test
    fun `insertContribution adds to pool`() = transactionalTest {
        val initialPool = BigDecimal("4000.00")
        val betId = 123L
        val userId = 456L
        val stakeAmount = BigDecimal("100.00")
        val contributionAmount = BigDecimal("2.00")
        val expectedCurrentPool = initialPool + contributionAmount

        val jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stakeAmount,
            contributionAmount = contributionAmount
        )

        expectThat(jackpotDao.findById(jackpot.id))
            .isNotNull()
            .get { currentPool }
            .isEqualTo(expectedCurrentPool)
    }

    @Test
    fun `insertContribution is idempotent`() = transactionalTest {
        val initialPool = BigDecimal("5000.00")
        val betId = 123L
        val userId = 456L
        val stakeAmount = BigDecimal("100.00")
        val contributionAmount = BigDecimal("2.00")

        val jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stakeAmount,
            contributionAmount = contributionAmount
        )

        expectCatching {
            jackpotDao.insertContribution(
                betId = betId,
                userId = userId,
                jackpot = jackpot,
                stakeAmount = stakeAmount,
                contributionAmount = contributionAmount
            )
        }.isFailure()
            .isA<IntegrityConstraintViolationException>()
    }

    @Test
    fun `insertReward resets pool to initial value`() = transactionalTest {
        val initialPool = BigDecimal("6000.00")
        val betId = 123L
        val userId = 456L
        val stakeAmount = BigDecimal("100.00")
        val contributionAmount = BigDecimal("2.00")
        val rewardAmount = BigDecimal("500.00")

        val jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stakeAmount,
            contributionAmount = contributionAmount
        )

        expectThat(jackpotDao.findById(jackpot.id))
            .isNotNull()
            .get { currentPool }
            .isEqualTo(initialPool + contributionAmount)

        jackpotDao.insertReward(
            betId = betId,
            jackpot = jackpot,
            rewardAmount = rewardAmount
        )

        expectThat(jackpotDao.findById(jackpot.id))
            .isNotNull()
            .get { currentPool }
            .isEqualTo(initialPool)
    }

    @Test
    fun `insertUnrewarded keeps current pool value`() = transactionalTest {
        val initialPool = BigDecimal("7000.00")
        val betId = 123L
        val userId = 456L
        val stakeAmount = BigDecimal("100.00")
        val contributionAmount = BigDecimal("2.00")
        val expectedCurrentPool = initialPool + contributionAmount

        var jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stakeAmount,
            contributionAmount = contributionAmount
        )

        jackpot = jackpotDao.findById(jackpot.id)!! // Refresh pool value

        jackpotDao.insertUnrewarded(
            betId = betId,
            jackpot = jackpot
        )

        expectThat(jackpotDao.findById(jackpot.id))
            .isNotNull()
            .get { currentPool }
            .isEqualTo(expectedCurrentPool)
    }

    @Test
    fun `reward status is unique per bet`() = transactionalTest {
        val initialPool = BigDecimal("8000.00")
        val betId = 123L
        val userId = 456L
        val stakeAmount = BigDecimal("100.00")
        val contributionAmount = BigDecimal("2.00")
        val rewardAmount = BigDecimal("500.00")

        val jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stakeAmount,
            contributionAmount = contributionAmount
        )
        jackpotDao.insertUnrewarded(
            betId = betId,
            jackpot = jackpot
        )

        expectCatching {
            jackpotDao.insertReward(
                betId = betId,
                jackpot = jackpot,
                rewardAmount = rewardAmount
            )
        }.isFailure()
            .isA<IntegrityConstraintViolationException>()
    }

    @Test
    fun `findContribution returns contribution by type`() = transactionalTest {
        // given
        val betId = 123L
        val userId = 456L
        val initialPool = BigDecimal("4000.00")
        val stakeAmount = BigDecimal("100.00")
        val contributionAmount = BigDecimal("2.00")

        val jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        // when
        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stakeAmount,
            contributionAmount = contributionAmount
        )

        // then
        expectThat(jackpotDao.findContribution(betId, CONTRIBUTION))
            .isNotNull()
            .and {
                get { type }.isEqualTo(CONTRIBUTION)
                get { stakeAmount }.isEqualTo(stakeAmount)
                get { contributionAmount }.isEqualTo(contributionAmount)
            }
    }

    @Test
    fun `findContribution returns null for non-existent contribution`() = transactionalTest {
        expectThat(jackpotDao.findContribution(999L, CONTRIBUTION)).isNull()
    }

    @Test
    fun `findContribution returns reward entry`() = transactionalTest {
        // given
        val betId = 123L
        val userId = 456L
        val initialPool = BigDecimal("4000.00")
        val stake = BigDecimal("100.00")
        val contribution = BigDecimal("2.00")
        val rewardAmount = BigDecimal("500.00")

        var jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        // Insert contribution first
        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stake,
            contributionAmount = contribution
        )

        jackpot = jackpotDao.findById(jackpot.id)!! // Refresh pool value

        // Insert reward
        jackpotDao.insertReward(
            betId = betId,
            jackpot = jackpot,
            rewardAmount = rewardAmount
        )

        // then
        expectThat(jackpotDao.findContribution(betId, REWARD))
            .isNotNull()
            .and {
                get { type }.isEqualTo(REWARD)
                get { stakeAmount }.isEqualTo(BigDecimal("0.00"))
                get { contributionAmount }.isEqualTo(-rewardAmount)
                get { jackpotAmountAfter }.isEqualTo(initialPool) // Reset to initial
            }
    }

    @Test
    fun `findContribution returns unrewarded entry`() = transactionalTest {
        // given
        val betId = 123L
        val userId = 456L
        val initialPool = BigDecimal("4000.00")
        val stake = BigDecimal("100.00")
        val contribution = BigDecimal("2.00")
        val expectedCurrentPool = initialPool + contribution

        var jackpot = createTestJackpot(
            initialPool = initialPool,
            chance = BigDecimal("0.01")
        )

        // Insert contribution first
        jackpotDao.insertContribution(
            betId = betId,
            userId = userId,
            jackpot = jackpot,
            stakeAmount = stake,
            contributionAmount = contribution
        )

        jackpot = jackpotDao.findById(jackpot.id)!! // Refresh pool value

        // Insert unrewarded
        jackpotDao.insertUnrewarded(
            betId = betId,
            jackpot = jackpot
        )

        // then
        expectThat(jackpotDao.findContribution(betId, UNREWARDED))
            .isNotNull()
            .and {
                get { type }.isEqualTo(UNREWARDED)
                get { stakeAmount }.isEqualTo(BigDecimal("0.00"))
                get { contributionAmount }.isEqualTo(BigDecimal("0.00"))
                get { jackpotAmountAfter }.isEqualTo(expectedCurrentPool) // Keeps current pool
            }
    }

    @Test
    fun `insertReward fails when no contribution exists`() = transactionalTest {
        val jackpot = createTestJackpot(
            initialPool = BigDecimal("1000.00"),
            chance = BigDecimal("0.01")
        )

        expectCatching {
            jackpotDao.insertReward(
                betId = 999L,
                jackpot = jackpot,
                rewardAmount = BigDecimal("500.00")
            )
        }
            .isFailure()
            .get { message }.isEqualTo("No contribution found for bet_id=999")
    }

    @Test
    fun `insertUnrewarded fails when no contribution exists`() = transactionalTest {
        val jackpot = createTestJackpot(
            initialPool = BigDecimal("1000.00"),
            chance = BigDecimal("0.01")
        )

        expectCatching {
            jackpotDao.insertUnrewarded(
                betId = 999L,
                jackpot = jackpot
            )
        }
            .isFailure()
            .get { message }.isEqualTo("No contribution found for bet_id=999")
    }

    context(tx: Transactional)
    private suspend fun createTestJackpot(
        initialPool: BigDecimal = BigDecimal("1000.00"),
        chance: BigDecimal = BigDecimal("0.01")
    ): Jackpot {
        val contributionConfigId = tx.dsl()
            .insertInto(
                CONTRIBUTION_CONFIG,
                CONTRIBUTION_CONFIG.TYPE,
                CONTRIBUTION_CONFIG.PERCENTAGE
            )
            .values("FIXED", BigDecimal("0.02"))
            .returningResult(CONTRIBUTION_CONFIG.ID)
            .coFetchSingle()
            .get(CONTRIBUTION_CONFIG.ID)!!

        val rewardConfigId = tx.dsl()
            .insertInto(
                REWARD_CONFIG,
                REWARD_CONFIG.TYPE,
                REWARD_CONFIG.CHANCE
            )
            .values("FIXED", chance)
            .returningResult(REWARD_CONFIG.ID)
            .coFetchSingle()
            .get(REWARD_CONFIG.ID)!!

        val jackpotId = tx.dsl()
            .insertInto(
                JACKPOT,
                JACKPOT.CONTRIBUTION_CONFIG_ID,
                JACKPOT.REWARD_CONFIG_ID,
                JACKPOT.INITIAL_POOL
            )
            .values(contributionConfigId, rewardConfigId, initialPool)
            .returningResult(JACKPOT.ID)
            .coFetchSingle()
            .get(JACKPOT.ID)!!

        return jackpotDao.findById(jackpotId)!!
    }
}
