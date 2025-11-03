import static org.springframework.cloud.contract.spec.Contract.make

make {
    description "Cannot reward a bet that is not ready"
    // e.g. the bet might still be waiting in the queue to be processed
    request {
        method PUT()
        url "/reward/9999"
        headers {
            contentType(applicationJson())
        }
    }
    response {
        status CONFLICT() // there is no clear HTTP status for "not ready"
        // should also include retry information
    }
}
