#!/usr/bin/env bash

./gradlew desktop:dist
java -jar ./desktop/build/libs/desktop-1.0.jar --debug --startScreen level --level reactor_man --soundVolume 0.5 --musicVolume 0.5 --fixedStepScalar 1