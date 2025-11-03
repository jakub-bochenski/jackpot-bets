@file:Suppress("NoUnusedImports")

package com.acme.jackpotbets.bet

import com.acme.jackpotbets.db.JackpotDao
import com.acme.jackpotbets.jackpot.ContributionConfig
import com.acme.jackpotbets.jackpot.Jackpot
import com.acme.jackpotbets.jackpot.RewardConfig
import com.acme.jackpotbets.model.jooq.enums.ContributionType.REWARD
import com.acme.jackpotbets.model.jooq.tables.references.JACKPOT_CONTRIBUTION
import com.acme.jackpotbets.rest.PlacedBet
import com.acme.jackpotbets.tx.TransactionOperation
import com.acme.jackpotbets.tx.Transactional
import com.acme.jackpotbets.utils.vertxRunBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.jooq.exception.IntegrityConstraintViolationException
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.math.BigDecimal

class BetServiceTest {
    @Test
    fun `placeBet inserts contribution for existing jackpot`() = withTestContext {
        val betAmount = BigDecimal("100.00")
        val placedBet = PlacedBet(
            betId = 123L,
            userId = 456L,
            jackpotId = 1L,
            amount = betAmount
        )

        coEvery { jackpotDao.findById(1L) } returns jackpot
        coEvery {
            jackpotDao.insertContribution(any(), any(), any(), any(), any())
        } returns Unit

        losingBetService.placeBet(placedBet)

        coVerify {
            jackpotDao.insertContribution(
                betId = 123L,
                userId = 456L,
                jackpot = jackpot,
                stakeAmount = betAmount,
                contributionAmount = BigDecimal("10.0000") // 10% of 100.00
            )
        }
    }

    @Test
    fun `placeBet silently skips when contribution already exists`() = withTestContext {
        val placedBet = PlacedBet(
            betId = 123L,
            userId = 456L,
            jackpotId = 1L,
            amount = BigDecimal("100.00")
        )

        coEvery { jackpotDao.findById(1L) } returns jackpot
        coEvery {
            jackpotDao.insertContribution(any(), any(), any(), any(), any())
        } throws IntegrityConstraintViolationException("Duplicate bet_id", null)

        losingBetService.placeBet(placedBet)

        coVerify(exactly = 1) {
            jackpotDao.insertContribution(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `rewardBet inserts reward when bet wins`() = withTestContext {
        // given
        coEvery { jackpotDao.findByBetId(123L) } returns jackpot
        coEvery { jackpotDao.insertReward(any(), any(), any()) } returns Unit

        // when
        val result = winningBetService.rewardBet(123L)

        // then
        expectThat(result).isEqualTo(jackpot.currentPool)
        coVerify {
            jackpotDao.insertReward(
                betId = 123L,
                jackpot = jackpot,
                rewardAmount = jackpot.currentPool
            )
        }
    }

    @Test
    fun `rewardBet inserts unrewarded when bet loses`() = withTestContext {
        // given
        coEvery { jackpotDao.findByBetId(123L) } returns jackpot
        coEvery { jackpotDao.insertUnrewarded(any(), any()) } returns Unit

        // when
        val result = losingBetService.rewardBet(123L)

        // then
        expectThat(result).isNull()
        coVerify {
            jackpotDao.insertUnrewarded(
                betId = 123L,
                jackpot = jackpot
            )
        }
    }

    @Test
    fun `rewardBet returns existing reward amount when already rewarded`() = withTestContext {
        // given
        val existingRewardAmount = BigDecimal("500.00")
        coEvery { jackpotDao.findByBetId(123L) } returns jackpot
        coEvery { jackpotDao.insertReward(any(), any(), any()) } throws
            IntegrityConstraintViolationException("Duplicate bet_id for reward status", null)
        coEvery { jackpotDao.findContribution(123L, REWARD) } returns JACKPOT_CONTRIBUTION.newRecord().apply {
            contributionAmount = -existingRewardAmount
        }

        // when
        val result = winningBetService.rewardBet(123L)

        // then
        expectThat(result).isEqualTo(existingRewardAmount)
    }

    @Test
    fun `rewardBet returns null when already unrewarded`() = withTestContext {
        // given
        coEvery { jackpotDao.findByBetId(123L) } returns jackpot
        coEvery { jackpotDao.insertReward(any(), any(), any()) } throws
            IntegrityConstraintViolationException("Duplicate bet_id for reward status", null)
        coEvery { jackpotDao.findContribution(123L, REWARD) } returns null

        // when
        val result = winningBetService.rewardBet(123L)

        // then
        expectThat(result).isNull()
    }

    private val jackpotDao = mockk<JackpotDao>()
    private val dslContext = mockk<org.jooq.DSLContext>()
    private val testContext = Transactional { dslContext }
    private val tx = object : TransactionOperation {
        override suspend fun <T> invoke(block: suspend Transactional.() -> T): T =
            block(testContext)
    }

    private val losingBetService = BetService(
        jackpotDao = jackpotDao,
        tx = tx,
        generator = { BigDecimal("0.02") } // default to losing number
    )

    private val winningBetService = BetService(
        jackpotDao = jackpotDao,
        tx = tx,
        generator = { BigDecimal("0.005") } // 0.5% < 1% chance = win
    )

    private val jackpot = Jackpot(
        initialPool = BigDecimal("1000.00"),
        currentPool = BigDecimal("2000.00"),
        contributionConfig = ContributionConfig.Fixed(
            percentage = BigDecimal("0.10"),
        ),
        rewardConfig = RewardConfig.Fixed(
            chance = BigDecimal("0.01"),
        )
    )

    private fun withTestContext(block: suspend Transactional.() -> Unit) =
        vertxRunBlocking {
            block(testContext)
        }
}
