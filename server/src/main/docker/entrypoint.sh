#!/bin/sh -eux

exec \
  java -server \
      -Dlogback.configurationFile="${LOGBACK_CONFIGURATION_FILE:-logback.xml}" \
      -agentlib:jdwp='transport=dt_socket,server=y,suspend=n,address=*:8000' \
      -cp '/app/*:/app/lib/*' com.acme.jackpotbets.Runner \
      "$@"
