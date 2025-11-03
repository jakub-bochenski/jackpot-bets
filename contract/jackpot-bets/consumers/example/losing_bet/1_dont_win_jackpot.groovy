import static org.springframework.cloud.contract.spec.Contract.make

// bet is preplaced to avoid randomness in tests

make {
    request {
        method PUT()
        url "/reward/3"
        headers {
            contentType(applicationJson())
        }
    }
    response {
        body(
            betId : 3,
            reward : null,
            win: false
        )
        status OK()
    }
}
