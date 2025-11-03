# Jackpot Bets

Project is split between main service and API contracts under `contract/` folder.
Typically, the contracts would live in a separate repository, to allow Consumer-Driven approach.

To build the project, first build the contracts:

```shell
./mvnw -f contract
```

Then build the main service:

```shell
./mvnw
```

This will start the service and execute contract tests against it (then stop the service).

To run the service use:

```shell
./mvnw -P run
```

The process will follow docker logs. To stop it use `CTRL-C`.

The service will be available at `http://localhost:8080`.

The build will also produce a docker image named `jackpot-bets` (see `server/pom.xml` property `build.outputImage`)
The docker image can be configured via Microprofile configuration (see `api/src/main/kotlin/com/acme/jackpotbets/option` package).
E.g. with environment variables:

```
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

## Structure

The project is based on Vert.x/RDBC/Kotlin-coroutines POC I've prepared earlier (in turn based on
`github.com/metaloom/poc-vertx-hibernate`)
I've used dockerized Postgres as a database instead of in-memory H2 as this was already available.

For the actual business code it's best to start from second commit.

## Comments

I've used numeric ids for simplicity, but in real-world scenario UUIDs would be preferred.

The contributions table relies on sequence generator, this can be a point of contention under high load.
I've added it to fix automated test failures quickly (the test was inserting multiple rows on the same millisecond).

All functions are implemented in single process, in real world scenario it makes little sense to have interprocess
communication over Kafka.
