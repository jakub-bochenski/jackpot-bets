@file:Suppress("UnusedImports")

package com.acme.jackpotbets.db

import com.acme.jackpotbets.jackpot.ContributionConfig
import com.acme.jackpotbets.jackpot.Jackpot
import com.acme.jackpotbets.jackpot.RewardConfig
import com.acme.jackpotbets.jooq.coFetchOne
import com.acme.jackpotbets.model.jooq.enums.ContributionType
import com.acme.jackpotbets.model.jooq.enums.ContributionType.CONTRIBUTION
import com.acme.jackpotbets.model.jooq.enums.ContributionType.REWARD
import com.acme.jackpotbets.model.jooq.enums.ContributionType.UNREWARDED
import com.acme.jackpotbets.model.jooq.tables.references.CONTRIBUTION_CONFIG
import com.acme.jackpotbets.model.jooq.tables.references.JACKPOT
import com.acme.jackpotbets.model.jooq.tables.references.JACKPOT_CONTRIBUTION
import com.acme.jackpotbets.model.jooq.tables.references.REWARD_CONFIG
import com.acme.jackpotbets.tx.Transactional
import kotlinx.coroutines.reactive.awaitSingle
import java.math.BigDecimal
import javax.inject.Inject

class JackpotDao @Inject constructor() {

    context(tx: Transactional)
    suspend fun findById(jackpotId: Long): Jackpot? {
        return findJackpot {
            where(JACKPOT.ID.eq(jackpotId))
        }
    }

    context(tx: Transactional)
    suspend fun findByBetId(betId: Long): Jackpot? {
        return findJackpot {
            join(JACKPOT_CONTRIBUTION)
                .on(JACKPOT.ID.eq(JACKPOT_CONTRIBUTION.JACKPOT_ID))
                .where(JACKPOT_CONTRIBUTION.BET_ID.eq(betId))
        }
    }

    context(tx: Transactional)
    private suspend fun findJackpot(
        queryModifier: org.jooq.SelectJoinStep<*>.() -> org.jooq.SelectConditionStep<*>
    ): Jackpot? {
        val record = tx.dsl()
            .select(
                JACKPOT.ID,
                JACKPOT.INITIAL_POOL,
                JACKPOT.CREATED_AT,
                CONTRIBUTION_CONFIG.ID,
                CONTRIBUTION_CONFIG.TYPE,
                CONTRIBUTION_CONFIG.PERCENTAGE,
                CONTRIBUTION_CONFIG.VARIABLE_INITIAL_PERCENTAGE,
                CONTRIBUTION_CONFIG.VARIABLE_DECAY_RATE,
                CONTRIBUTION_CONFIG.MIN_PERCENTAGE,
                CONTRIBUTION_CONFIG.CREATED_AT,
                REWARD_CONFIG.ID,
                REWARD_CONFIG.TYPE,
                REWARD_CONFIG.CHANCE,
                REWARD_CONFIG.INITIAL_CHANCE,
                REWARD_CONFIG.MAX_POOL_MULTIPLIER,
                REWARD_CONFIG.CREATED_AT,
            )
            .from(JACKPOT)
            .join(CONTRIBUTION_CONFIG).on(JACKPOT.CONTRIBUTION_CONFIG_ID.eq(CONTRIBUTION_CONFIG.ID))
            .join(REWARD_CONFIG).on(JACKPOT.REWARD_CONFIG_ID.eq(REWARD_CONFIG.ID))
            .let(queryModifier)
            .coFetchOne()
            ?: return null

        val currentPool = getCurrentPool(record.get(JACKPOT.ID)!!)

        val contributionConfig = when (record.get(CONTRIBUTION_CONFIG.TYPE)) {
            "FIXED" ->
                ContributionConfig.Fixed(
                    record.get(CONTRIBUTION_CONFIG.ID)!!,
                    record.get(CONTRIBUTION_CONFIG.CREATED_AT)!!,
                    record.get(CONTRIBUTION_CONFIG.PERCENTAGE)!!,
                )

            "VARIABLE" ->
                ContributionConfig.Variable(
                    record.get(CONTRIBUTION_CONFIG.ID)!!,
                    record.get(CONTRIBUTION_CONFIG.CREATED_AT)!!,
                    record.get(CONTRIBUTION_CONFIG.VARIABLE_INITIAL_PERCENTAGE)!!,
                    record.get(CONTRIBUTION_CONFIG.VARIABLE_DECAY_RATE)!!,
                    record.get(CONTRIBUTION_CONFIG.MIN_PERCENTAGE)!!,
                )

            else -> error(
                "Unsupported contribution_config.type=${record.get(CONTRIBUTION_CONFIG.TYPE)} for jackpot id=${
                    record.get(JACKPOT.ID)
                }"
            )
        }

        val rewardConfig = when (record.get(REWARD_CONFIG.TYPE)) {
            "FIXED" -> RewardConfig.Fixed(
                record.get(REWARD_CONFIG.ID)!!,
                record.get(REWARD_CONFIG.CHANCE)!!,
                record.get(REWARD_CONFIG.CREATED_AT)!!
            )

            "VARIABLE" -> RewardConfig.Variable(
                record.get(REWARD_CONFIG.ID)!!,
                record.get(REWARD_CONFIG.INITIAL_CHANCE)!!,
                record.get(REWARD_CONFIG.MAX_POOL_MULTIPLIER)!!,
                record.get(REWARD_CONFIG.CREATED_AT)!!
            )

            else -> error(
                "Unsupported reward_config.type=${record.get(REWARD_CONFIG.TYPE)} " +
                    "for jackpot id=${record.get(JACKPOT.ID)}"
            )
        }

        return Jackpot(
            record.get(JACKPOT.ID)!!,
            record.get(JACKPOT.INITIAL_POOL)!!,
            currentPool,
            record.get(JACKPOT.CREATED_AT)!!,
            contributionConfig,
            rewardConfig,
        )
    }

    context(tx: Transactional)
    private suspend fun getCurrentPool(jackpotId: Long): BigDecimal {
        return tx.dsl()
            .select(JACKPOT_CONTRIBUTION.JACKPOT_AMOUNT_AFTER)
            .from(JACKPOT_CONTRIBUTION)
            .where(JACKPOT_CONTRIBUTION.JACKPOT_ID.eq(jackpotId))
            .orderBy(JACKPOT_CONTRIBUTION.CREATED_AT.desc(), JACKPOT_CONTRIBUTION.ID.desc())
            .limit(1)
            .coFetchOne()
            ?.get(JACKPOT_CONTRIBUTION.JACKPOT_AMOUNT_AFTER)
            ?: tx.dsl()
                .select(JACKPOT.INITIAL_POOL)
                .from(JACKPOT)
                .where(JACKPOT.ID.eq(jackpotId))
                .coFetchOne()
                ?.get(JACKPOT.INITIAL_POOL)
            ?: error("Jackpot not found: jackpot_id=$jackpotId")
    }

    /**
     * Add an entry to the jackpot_contribution table.
     *
     * For contributions:
     * - type = CONTRIBUTION
     * - stakeAmount = actual bet stake amount
     * - contributionAmount = positive amount to add to pool
     *
     * For rewards:
     * - type = REWARD
     * - stakeAmount = 0
     * - contributionAmount = negative amount to withdraw from pool
     * - resets pool back to initial value
     *
     * For unrewarded:
     * - type = UNREWARDED
     * - stakeAmount = 0
     * - contributionAmount = 0
     * - keeps current pool value
     */
    context(tx: Transactional)
    private suspend fun insertEntry(
        type: ContributionType,
        betId: Long,
        userId: Long,
        jackpot: Jackpot,
        stakeAmount: BigDecimal = BigDecimal.ZERO,
        contributionAmount: BigDecimal = BigDecimal.ZERO,
    ) {
        val newPool = when (type) {
            CONTRIBUTION -> jackpot.currentPool + contributionAmount
            REWARD -> jackpot.initialPool
            UNREWARDED -> jackpot.currentPool
        }

        tx.dsl()
            .insertInto(
                JACKPOT_CONTRIBUTION,
                JACKPOT_CONTRIBUTION.TYPE,
                JACKPOT_CONTRIBUTION.BET_ID,
                JACKPOT_CONTRIBUTION.USER_ID,
                JACKPOT_CONTRIBUTION.JACKPOT_ID,
                JACKPOT_CONTRIBUTION.STAKE_AMOUNT,
                JACKPOT_CONTRIBUTION.CONTRIBUTION_AMOUNT,
                JACKPOT_CONTRIBUTION.JACKPOT_AMOUNT_AFTER
            )
            .values(
                type,
                betId,
                userId,
                jackpot.id,
                stakeAmount,
                contributionAmount,
                newPool
            )
            .awaitSingle()
    }

    /**
     * Add a contribution to the jackpot.
     */
    context(tx: Transactional)
    suspend fun insertContribution(
        betId: Long,
        userId: Long,
        jackpot: Jackpot,
        stakeAmount: BigDecimal,
        contributionAmount: BigDecimal
    ) = insertEntry(
        type = CONTRIBUTION,
        betId = betId,
        userId = userId,
        jackpot = jackpot,
        stakeAmount = stakeAmount,
        contributionAmount = contributionAmount
    )

    /**
     * Add a reward withdrawal from the jackpot and reset pool to initial value.
     */
    context(tx: Transactional)
    suspend fun insertReward(
        betId: Long,
        jackpot: Jackpot,
        rewardAmount: BigDecimal
    ) = insertEntry(
        type = REWARD,
        betId = betId,
        userId = findUserIdByBetId(betId),
        jackpot = jackpot,
        contributionAmount = -rewardAmount,
    )

    /**
     * Record a bet that didn't win the jackpot.
     * This creates an audit trail and ensures reward requests are idempotent
     * by preventing double rewards of the same bet.
     */
    context(tx: Transactional)
    suspend fun insertUnrewarded(
        betId: Long,
        jackpot: Jackpot
    ) = insertEntry(
        type = UNREWARDED,
        betId = betId,
        userId = findUserIdByBetId(betId),
        jackpot = jackpot,
    )

    /**
     * Find contribution of specific type for a bet.
     * Returns null if no contribution of that type exists.
     */
    context(tx: Transactional)
    suspend fun findContribution(betId: Long, type: ContributionType) = tx.dsl()
        .selectFrom(JACKPOT_CONTRIBUTION)
        .where(JACKPOT_CONTRIBUTION.BET_ID.eq(betId))
        .and(JACKPOT_CONTRIBUTION.TYPE.eq(type))
        .coFetchOne()

    context(tx: Transactional)
    private suspend fun findUserIdByBetId(betId: Long): Long = tx.dsl()
        .select(JACKPOT_CONTRIBUTION.USER_ID)
        .from(JACKPOT_CONTRIBUTION)
        .where(JACKPOT_CONTRIBUTION.BET_ID.eq(betId))
        .and(JACKPOT_CONTRIBUTION.TYPE.eq(CONTRIBUTION))
        .coFetchOne()
        ?.get(JACKPOT_CONTRIBUTION.USER_ID)
        ?: error("No contribution found for bet_id=$betId")
}
