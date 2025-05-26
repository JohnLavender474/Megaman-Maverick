#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--logLevels debug,log,error --writeLogsToFile --debugWindow --soundVolume 0 --musicVolume 0 --fixedStepScalar 1 --pixelPerfect"
