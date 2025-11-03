package com.acme.jackpotbets.rest

import com.acme.jackpotbets.bet.BetService
import io.vertx.ext.web.RoutingContext
import javax.inject.Inject

class RewardRestHandler @Inject constructor(
    val betService: BetService,
) {

    suspend fun reward(rc: RoutingContext) {
        val betId = rc.pathParam("betId").toLong()

        val reward = betService.rewardBet(betId)
        val win = reward != null

        rc.sendJson(
            200,
            mapOf(
                "betId" to betId,
                "reward" to reward,
                "win" to win,
            )
        )
    }
}
