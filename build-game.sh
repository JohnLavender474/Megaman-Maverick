#!/bin/bash

if [ $# -lt 2 ]; then
    echo "Usage: $0 <mega-man-maverick|mega-man-x-lost-memories> <linux64|windows64|mac>"
    exit 1
fi

GAME=$1
PLATFORM=$2
GAME_BUILDS_DIR=${GAME_BUILDS_DIR:-./game-builds}

case "$GAME" in
    mega-man-maverick)
        GAME_NAME="Megaman-Maverick"
        VERSION_KEY="projectVersion"
        JAR_TASK="lwjgl3:jar"
        JAR_DIR="./lwjgl3/build/libs"
        MAIN_CLASS="com.megaman.maverick.game.lwjgl3.DesktopLauncher"
        ;;
    mega-man-x-lost-memories)
        GAME_NAME="Mega-Man-X-Lost-Memories"
        VERSION_KEY="lostMemoriesVersion"
        JAR_TASK="lost-memories-desktop:jar"
        JAR_DIR="./games/mega-man-x-lost-memories/desktop/build/libs"
        MAIN_CLASS="com.megaman.lostmemories.desktop.LostMemoriesLauncher"
        ;;
    *)
        echo "Error: Invalid game. Please use mega-man-maverick or mega-man-x-lost-memories."
        exit 1
        ;;
esac

# Define platform-specific paths and URLs (JDK downloads MUST include JRE bundle)
case "$PLATFORM" in
    windows64)
        JDK_PATH="$GAME_BUILDS_DIR/jdk/windows64"
        JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk-jre/17.0.14+7/openlogic-openjdk-jre-17.0.14+7-windows-x64.zip"
        ;;
    mac)
        JDK_PATH="$GAME_BUILDS_DIR/jdk/mac"
        JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk-jre/17.0.14+7/openlogic-openjdk-jre-17.0.14+7-mac-x64.zip"
        ;;
    linux64)
        JDK_PATH="$GAME_BUILDS_DIR/jdk/linux64"
        JDK_URL="https://builds.openlogic.com/downloadJDK/openlogic-openjdk-jre/17.0.14+7/openlogic-openjdk-jre-17.0.14+7-linux-x64.tar.gz"
        ;;
    *)
        echo "Error: Invalid platform specified. Please use windows64, mac, or linux64."
        exit 1
        ;;
esac

VERSION=$(grep "^$VERSION_KEY=" gradle.properties | cut -d'=' -f2)
if [ -z "$VERSION" ]; then
    echo "Error: $VERSION_KEY not found in gradle.properties."
    exit 1
fi
EXECUTABLE_NAME="$GAME_NAME"
JAR_PATH="$JAR_DIR/$GAME_NAME-$VERSION.jar"
OUTPUT_DIR="$GAME_BUILDS_DIR/$GAME"
BUILD_NAME="$GAME_NAME-$VERSION-$PLATFORM"

# Function to check and fetch the JDK if not found
check_and_fetch_jdk() {
    if [ ! -d "$JDK_PATH" ]; then
        echo "JDK not found at $JDK_PATH"
        read -p "Would you like to download it? (y/n): " choice
        if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
            echo "Downloading JDK..."

            mkdir -p "$JDK_PATH"

            case "$PLATFORM" in
                windows64|mac)
                    curl -L "$JDK_URL" -o "$JDK_PATH/jdk.zip"
                    unzip -q "$JDK_PATH/jdk.zip" -d "$JDK_PATH"
                    ;;
                linux64)
                    curl -L "$JDK_URL" -o "$JDK_PATH/jdk.tar.gz"
                    tar -xzf "$JDK_PATH/jdk.tar.gz" -C "$JDK_PATH"
                    ;;
            esac

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

echo "Building $JAR_PATH with ./gradlew $JAR_TASK"
if ! ./gradlew "$JAR_TASK"; then
    echo "The jar build failed. Exiting script."
    exit 1
fi
if [ ! -f "$JAR_PATH" ]; then
    echo "The jar $JAR_PATH does not exist. Exiting script."
    exit 1
fi

echo "Removing previous build $OUTPUT_DIR/$BUILD_NAME"
mkdir -p "$OUTPUT_DIR"
rm -rf "${OUTPUT_DIR:?}/$BUILD_NAME" "${OUTPUT_DIR:?}/$BUILD_NAME.zip"

echo "Running packr for game=$GAME, platform=$PLATFORM, jdk=$JDK_PATH"
java -jar "$GAME_BUILDS_DIR/packr-all-4.0.0.jar" \
     --platform "$PLATFORM" \
     --jdk "$JDK_PATH" \
     --useZgcIfSupportedOs \
     --executable "$EXECUTABLE_NAME" \
     --classpath "$JAR_PATH" \
     --mainclass "$MAIN_CLASS" \
     --vmargs Xms512M Xmx1024M \
     --resources assets/* \
     --output "$OUTPUT_DIR/$BUILD_NAME" \
     --verbose

if [ "$PLATFORM" = "linux64" ]; then
    echo "Setting executable bit on $EXECUTABLE_NAME"
    chmod +x "$OUTPUT_DIR/$BUILD_NAME/$EXECUTABLE_NAME"
fi

echo "Checking that the build output exists"
if [ ! -d "$OUTPUT_DIR/$BUILD_NAME" ]; then
    echo "The $OUTPUT_DIR/$BUILD_NAME folder does not exist. Exiting script."
    exit 1
fi

echo "Zipping the $BUILD_NAME folder..."
if (cd "$OUTPUT_DIR" && zip -qr "$BUILD_NAME.zip" "$BUILD_NAME"); then
    echo "Zipping completed successfully."
else
    echo "Zipping failed."
    exit 1
fi

echo "Process finished: $OUTPUT_DIR/$BUILD_NAME.zip"
exit 0
