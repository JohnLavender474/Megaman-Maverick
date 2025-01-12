#!/bin/bash
rm -rf ./game-builds/out-windows

java -jar ./game-builds/packr-all-4.0.0.jar \
     --platform windows64 \
     --jdk ./game-builds/jdk/openlogic-openjdk-17.0.13+11-windows-x64.zip \
     --useZgcIfSupportedOs \
     --executable Megaman-Maverick \
     --classpath ./lwjgl3/build/libs/MegamanMaverick-1.0.0.jar \
     --mainclass com.megaman.maverick.game.lwjgl3.DesktopLauncher \
     --vmargs Xmx1G XstartOnFirstThread \
     --resources assets/* \
     --output ./game-builds/out-windows
     --verbose
