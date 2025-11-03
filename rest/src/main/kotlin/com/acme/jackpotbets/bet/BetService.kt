package com.acme.jackpotbets.bet

import com.acme.jackpotbets.db.JackpotDao
import com.acme.jackpotbets.exception.DomainException
import com.acme.jackpotbets.model.jooq.enums.ContributionType.REWARD
import com.acme.jackpotbets.rest.PlacedBet
import com.acme.jackpotbets.tx.TransactionOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.exception.IntegrityConstraintViolationException
import java.math.BigDecimal
import javax.inject.Inject

class BetService constructor(
    val jackpotDao: JackpotDao,
    val tx: TransactionOperation,
    val generator: () -> BigDecimal,
) {
    @Inject
    constructor(
        jackpotDao: JackpotDao,
        tx: TransactionOperation,
    ) : this(
        jackpotDao = jackpotDao,
        tx = tx,
        generator = { Math.random().toBigDecimal() } // use better RNG in real system
    )

    suspend fun placeBet(placedBet: PlacedBet) =
        try {
            tx {
                val jackpot = jackpotDao.findById(placedBet.jackpotId)
                    ?: error("Jackpot not found: jackpot_id=${placedBet.jackpotId}")

                val contributionAmount = jackpot.calculateContribution(placedBet.amount)

                jackpotDao.insertContribution(
                    betId = placedBet.betId,
                    userId = placedBet.userId,
                    jackpot = jackpot,
                    stakeAmount = placedBet.amount,
                    contributionAmount = contributionAmount
                )

                log.info {
                    "Contributed $contributionAmount to jackpot ${placedBet.jackpotId} " +
                        "for bet ${placedBet.betId} from user ${placedBet.userId}"
                }
            }
        } catch (ex: IntegrityConstraintViolationException) {
            // for simplicity assume any constraint violation here means the contribution already exists
            // in real system you want a specific exception for this case
            log.warn(ex) { "Contribution already exists for bet ${placedBet.betId}, skipping insertion" }
        }

    suspend fun rewardBet(betId: Long): BigDecimal? =
        try {
            tx {
                val jackpot = jackpotDao.findByBetId(betId)
                    ?: throw DomainException(409, "Jackpot not found for bet: bet_id=$betId")

                val rewardAmount = jackpot.tryToWin(generator)

                if (rewardAmount != null) {
                    log.info { "Bet $betId won reward of amount $rewardAmount" }

                    jackpotDao.insertReward(
                        betId = betId,
                        jackpot = jackpot,
                        rewardAmount = rewardAmount
                    )
                } else {
                    log.info { "Bet $betId did not win a reward" }

                    jackpotDao.insertUnrewarded(
                        betId = betId,
                        jackpot = jackpot
                    )
                }

                rewardAmount
            }
        } catch (ex: IntegrityConstraintViolationException) {
            // for simplicity assume any constraint violation here means the contribution already exists
            // in real system you want a specific exception for this case
            tx {
                val existingReward = jackpotDao.findContribution(betId, REWARD)
                log.info(ex) { "Found existing ${existingReward?.type ?: "no"} reward for bet $betId" }
                existingReward?.let { -it.contributionAmount }
            }
        }
}

private val log = KotlinLogging.logger { }
