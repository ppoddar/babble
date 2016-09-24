#!/bin/bash
# ------------------------------------------------------------------------
# Runs a program that talks to a HTTP server
#
# Usage:
#   ./http-client.sh localhost:8090 < commands.txt
#
# will run a client targeting a server running at localhost listening to port 8090
# and send all requests found in commands.txt
# ------------------------------------------------------------------------

M2_REPO=$HOME/.m2/repository
SLF4J="$M2_REPO"/org/slf4j/slf4j-api/1.7.0/slf4j-api-1.7.0.jar
SLF4J_SIMPLE="$M2_REPO"/org/slf4j/slf4j-simple/1.7.21/slf4j-simple-1.7.21.jar

CP=./target/classes:"$SLF4J":"$SLF4J_SIMPLE"


DEBUG=-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
java -cp "$CP" "$DEBUG" babble.net.http.HttpClient $1
