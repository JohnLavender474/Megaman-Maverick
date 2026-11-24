#!/usr/bin/env bash

./gradlew lost-memories-desktop:run --args="--loadKeyboardPrefs --worldContainer simple --logLevels debug,log,error --writeLogsToFile --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1 --pixelPerfect $*"
