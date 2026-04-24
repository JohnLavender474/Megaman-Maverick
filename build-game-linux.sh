#!/bin/bash

# Do the following before running this script
# - in `gradle.properties`, set `projectVersion` to the correct version
# - run `./gradlew lwjgl3:dist` to create the JAR corresponding to the name below

VERSION=$(grep "^projectVersion=" gradle.properties | cut -d'=' -f2)
./build-game.sh linux64 "Megaman-Maverick-$VERSION"
