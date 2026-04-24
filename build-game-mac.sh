#!/bin/bash

# Run `./gradlew lwjgl3:build` before running this script

VERSION=$(grep "^projectVersion=" gradle.properties | cut -d'=' -f2)
./build-game.sh mac "Megaman-Maverick-$VERSION"
