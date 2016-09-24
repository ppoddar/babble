#!/bin/bash
# ------------------------------------------------------------------------
# Runs a HTTP server program
#
# Usage:
#   ./http-server.sh 8090
#
# will run a server at port 8090
# ------------------------------------------------------------------------

log_level=${2:-debug}
M2_REPO=$HOME/.m2/repository
SLF4J="$M2_REPO"/org/slf4j/slf4j-api/1.7.0/slf4j-api-1.7.0.jar
SLF4J_SIMPLE="$M2_REPO"/org/slf4j/slf4j-simple/1.7.21/slf4j-simple-1.7.21.jar

CP=./target/classes:"$SLF4J":"$SLF4J_SIMPLE"



DEBUG=-Dorg.slf4j.simpleLogger.defaultLogLevel="$log_level"
java -cp "$CP" "$DEBUG" babble.net.http.HttpServer $1

