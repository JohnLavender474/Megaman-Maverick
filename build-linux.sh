#!/bin/bash
rm -rf ./game-builds/out-linux

./gradlew lwjgl3:build

java -jar ./game-builds/packr-all-4.0.0.jar \
     --platform linux64 \
     --jdk ./game-builds/jdk/openlogic-openjdk-17.0.13+11-linux-x64.tar.gz \
     --useZgcIfSupportedOs \
     --executable Megaman-Maverick \
     --classpath ./lwjgl3/build/libs/MegamanMaverick-1.0.0.jar \
     --mainclass com.megaman.maverick.game.lwjgl3.DesktopLauncher \
     --vmargs Xmx1G XstartOnFirstThread \
     --resources assets/* \
     --output ./game-builds/out-linux \
     --verbose
