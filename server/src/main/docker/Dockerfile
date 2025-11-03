ARG DOCKER_REGISTRY=${env.DOCKER_REGISTRY}

FROM $DOCKER_REGISTRY/library/eclipse-temurin:21-alpine

EXPOSE 8080 8000
WORKDIR /app

HEALTHCHECK --interval=1s --timeout=1s --start-period=1s --retries=3 CMD wget -qO- http://127.0.0.1:8080/health

COPY dependencies /app/lib

COPY artifact /app

COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]


