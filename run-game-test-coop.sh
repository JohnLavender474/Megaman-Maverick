#!/usr/bin/env bash

defaults="--players 2"
[[ " $* " == *" --level "* ]] || defaults="$defaults --level REFACTOR_TEST_047"
[[ " $* " == *" --mode "* ]] || defaults="$defaults --mode coop"

./gradlew lwjgl3:run --args="--loadKeyboardPrefs $defaults --debugShapes --diagnostics --pathfinding async --worldContainer simple --logLevels debug,log,error --writeLogsToFile --width 1920 --height 1080 --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1 $*"
