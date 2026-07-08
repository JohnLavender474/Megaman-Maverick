#!/usr/bin/env bash

./gradlew lwjgl3:run --args="--pathfinding async --worldContainer quadtree --performance medium --debugShapes --logLevels debug,log,error --debugText --debugWindow --width 1920 --height 1080 --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1"
