#!/usr/bin/env bash

set -e

# ===
# With this command you can set a new version in all pom.xml files.
# Run it in the root folder of your project
#
# usage in Terminal:  ./setPomVersions.sh 1.0.0-SNAPSHOT
#
# after this ./mvnw verify, commit and push it to the develop branch
# ===

./mvnw versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
