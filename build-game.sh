#!/bin/bash

# Check if platform argument is provided
if [ -z "$1" ]; then
    echo "Error: No platform specified. Please specify a platform (windows64, mac, linux64)."
    exit 1
fi

# Platform argument
PLATFORM=$1

# Define platform-specific paths and URLs
case "$PLATFORM" in
    windows64)
        JDK_PATH="./game-builds/jdk/windows64"
        JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk-jre/17.0.14+7/openlogic-openjdk-jre-17.0.14+7-windows-x64.zip"
        ;;
    mac)
        JDK_PATH="./game-builds/jdk/mac"
        JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk-jre/17.0.14+7/openlogic-openjdk-jre-17.0.14+7-mac-x64.zip"
        ;;
    linux64)
        JDK_PATH="./game-builds/jdk/linux64"
        JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk-jre/17.0.14+7/openlogic-openjdk-jre-17.0.14+7-linux-x64.tar.gz"
        ;;
    *)
        echo "Error: Invalid platform specified. Please use windows64, mac, or linux64."
        exit 1
        ;;
esac

# Function to check and fetch the JDK if not found
check_and_fetch_jdk() {
    if [ ! -d "$JDK_PATH" ]; then
        echo "JDK not found at $JDK_PATH"
        read -p "Would you like to download it? (y/n): " choice
        if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
            echo "Downloading JDK..."

            # Download JDK from the appropriate URL
            mkdir -p "$JDK_PATH"

            # Download JDK based on the platform
            case "$PLATFORM" in
                windows64)
                    curl -L "$JDK_URL" -o "$JDK_PATH/jdk.zip"
                    unzip -q "$JDK_PATH/jdk.zip" -d "$JDK_PATH"
                    ;;
                mac)
                    curl -L "$JDK_URL" -o "$JDK_PATH/jdk.zip"
                    unzip -q "$JDK_PATH/jdk.zip" -d "$JDK_PATH"
                    ;;
                linux64)
                    curl -L "$JDK_URL" -o "$JDK_PATH/jdk.tar.gz"
                    tar -xzf "$JDK_PATH/jdk.tar.gz" -C "$JDK_PATH"
                    ;;
                *)
                    echo "Error: Unsupported platform. Please use windows64, mac, or linux64."
                    exit 1
                    ;;
            esac

            unzip -q "$JDK_PATH/jdk.zip" -d "$JDK_PATH"

            echo "JDK downloaded and extracted to $JDK_PATH"
        else
            echo "Exiting script. JDK is required."
            exit 1
        fi
    else
        echo "JDK found at $JDK_PATH"
    fi
}

check_and_fetch_jdk

# Clean up previous builds
echo "Removing previous build folder for $PLATFORM"
rm -rf ./game-builds/out-$PLATFORM

# Build the project with Gradle
echo "Building desktop JAR"
./gradlew lwjgl3:build

# Run the Packr tool for the specific platform
echo "Running packr for platform=$PLATFORM, jdk=$JDK_PATH"
java -jar ./game-builds/packr-all-4.0.0.jar \
     --platform $PLATFORM \
     --jdk $JDK_PATH \
     --useZgcIfSupportedOs \
     --executable Megaman-Maverick \
     --classpath ./lwjgl3/build/libs/MegamanMaverick-1.0.0.jar \
     --mainclass com.megaman.maverick.game.lwjgl3.DesktopLauncher \
     --vmargs Xmx1G \
     --resources assets/* \
     --output ./game-builds/out-$PLATFORM \
     --verbose

# Check if the output folder exists
echo "Checking that game-build output exists for platform=$PLATFORM"
if [ ! -d "./game-builds/out-$PLATFORM" ]; then
    echo "The out-$PLATFORM folder does not exist. Exiting script."
    exit 1
fi

# Zip the out-platform folder
echo "Zipping the out-$PLATFORM folder..."
zip -r "./game-builds/out-$PLATFORM.zip" "./game-builds/out-$PLATFORM"

if [ $? -eq 0 ]; then
    echo "Zipping completed successfully."
else
    echo "Zipping failed."
    exit 1
fi

# End of script
echo "Process finished"
exit 0
