#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--performance medium --logLevels debug,log,error --writeLogsToFile --soundVolume 0 --musicVolume 0 --fixedStepScalar 1 --pixelPerfect"
