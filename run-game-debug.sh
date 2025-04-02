#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--logLevels debug,log,error --writeLogsToFile --debugWindow --debugShapes --debugText --width 1920 --height 1080 --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1"
