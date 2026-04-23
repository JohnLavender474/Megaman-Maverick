#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--fps 60 --diagnostics --logLevels debug,log,error --writeLogsToFile --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1 --pixelPerfect"
