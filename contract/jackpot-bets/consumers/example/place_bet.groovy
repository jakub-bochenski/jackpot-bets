import static org.springframework.cloud.contract.spec.Contract.make

[
    make {
        request {
            method POST()
            url "/bets"
            body(
                betId: 1,
                userId: 1,
                jackpotId: 1,
                amount: 50,
            )
            headers {
                contentType(applicationJson())
            }
        }
        response {
            status ACCEPTED()
            body(
                betId: 1,
                userId: 1,
                jackpotId: 1,
                amount: 50,
            )
            headers {
                contentType(applicationJson())
            }
        }
    },
    make {
        description "Cannot place a bet with negative amount"
        request {
            method POST()
            url "/bets"
            body(
                betId: 1,
                userId: 1,
                jackpotId: 1,
                amount: -50,
            )
            headers {
                contentType(applicationJson())
            }
        }
        response {
            status BAD_REQUEST()
        }
    },
]
