#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--pathfinding sync --worldContainer simple --performance medium --diagnostics --logLevels debug,log,error --writeLogsToFile --debugShapes --debugText --width 1920 --height 1080 --soundVolume 0 --musicVolume 0 --fixedStepScalar 1"
