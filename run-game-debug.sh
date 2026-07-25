#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--diagnostics --pathfinding async --worldContainer simple --performance medium --logLevels debug,log,error --debugText --debugWindow --width 1920 --height 1080 --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1"
