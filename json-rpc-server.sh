#!/bin/bash
# ------------------------------------------------------------------------
# Runs a JSON-RPC server program
#
# Usage:
#   ./json-rpc-server.sh 8090
#
# will run a server at port 8090
# ------------------------------------------------------------------------

M2_REPO=$HOME/.m2/repository
SLF4J="$M2_REPO"/org/slf4j/slf4j-api/1.7.0/slf4j-api-1.7.0.jar
SLF4J_SIMPLE="$M2_REPO"/org/slf4j/slf4j-simple/1.7.21/slf4j-simple-1.7.21.jar
JSON="$M2_REPO"/org/json/json/20140107/json-20140107.jar

CP=./target/classes:"$SLF4J":"$SLF4J_SIMPLE":"$JSON"

DEBUG=-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
java -cp "$CP" "$DEBUG" babble.net.json.JSONRPCServer $@

