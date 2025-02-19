#!/bin/bash
JDK_PATH="./game-builds/jdk/mac"
JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk/17.0.14+7/openlogic-openjdk-17.0.14+7-mac-x64.zip"

check_jdk() {
    if [ ! -d "$JDK_PATH" ]; then
        echo "JDK not found at $JDK_PATH"
        read -p "Would you like to download it? (y/n): " choice
        if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
            echo "Downloading JDK..."
            # Download JDK from the appropriate URL
            curl -L "$JDK_URL" -o "./game-builds/jdk/jdk.zip"
            mkdir -p "$JDK_PATH"
            unzip -q "./game-builds/jdk/jdk.zip" -d "$JDK_PATH"
            echo "JDK downloaded and extracted to $JDK_PATH"
        else
            echo "Exiting script. JDK is required."
            exit 1
        fi
    else
        echo "JDK found at $JDK_PATH"
    fi
}

check_jdk

rm -rf ./game-builds/out-mac

./gradlew lwjgl3:build

java -jar ./game-builds/packr-all-4.0.0.jar \
     --platform macos \
     --jdk $JDK_PATH \
     --useZgcIfSupportedOs \
     --executable Megaman-Maverick \
     --classpath ./lwjgl3/build/libs/MegamanMaverick-1.0.0.jar \
     --mainclass com.megaman.maverick.game.lwjgl3.DesktopLauncher \
     --vmargs Xmx1G \
     --resources assets/* \
     --output ./game-builds/out-mac \
     --verbose
