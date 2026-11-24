#!/usr/bin/env bash

./gradlew lost-memories-desktop:run --args="--loadKeyboardPrefs --diagnostics --pathfinding async --worldContainer simple --logLevels debug,log,error --debugText --debugWindow --width 1920 --height 1080 --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1 $*"
