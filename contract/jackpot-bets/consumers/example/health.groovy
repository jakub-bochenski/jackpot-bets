import static org.springframework.cloud.contract.spec.Contract.make

make {
    description "Health endpoint returns 200 OK with status message"
    request {
        method GET()
        url "/health"
    }
    response {
        status OK()
        body(
            status: "ok"
        )
    }
}
