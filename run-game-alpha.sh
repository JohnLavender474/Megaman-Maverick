#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--logLevels debug,log,error --debugText --writeLogsToFile --debugWindow --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1 --pixelPerfect"
