import static org.springframework.cloud.contract.spec.Contract.make

make {
    request {
        method PUT()
        url "/reward/2"
        headers {
            contentType(applicationJson())
        }
    }
    response {
        body(
            betId: 2,
            reward: 1001,
            win: true
        )
        status OK()
    }
}
