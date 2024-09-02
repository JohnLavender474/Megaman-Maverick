#!/usr/bin/env bash

./gradlew desktop:dist
java -jar ./desktop/build/libs/desktop-1.0.jar --startScreen level --level desert_man