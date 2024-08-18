package com.megaman.maverick.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.megaman.maverick.game.screens.levels.Level;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {

    private static final int DEFAULT_FPS = 60;
    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;
    private static final boolean DEFAULT_FULLSCREEN = true;
    private static final boolean DEFAULT_VSYNC = false;
    private static final boolean DEFAULT_DEBUG = false;
    private static final String DEFAULT_START_SCREEN = "main";
    private static final String DEFAULT_LEVEL = "null";
    private static final String TITLE = "Megaman Maverick";

    public static class DesktopAppArgs {
        @Parameter(names = {"--fps"}, description =
                "Frames per second: min of 30 and max of 90. Default value = " + DEFAULT_FPS + ".")
        public int fps = DEFAULT_FPS;

        @Parameter(names = {"--width"}, description =
                "Window width: min of 600. Default value = " + DEFAULT_WIDTH + ".")
        public int width = DEFAULT_WIDTH;

        @Parameter(names = {"--height"}, description =
                "Window height: min of 400. Default value = " + DEFAULT_HEIGHT + ".")
        public int height = DEFAULT_HEIGHT;

        @Parameter(names = {"--fullscreen"}, description =
                "Enable fullscreen. Default value = " + DEFAULT_FULLSCREEN + ".")
        public boolean fullScreen = DEFAULT_FULLSCREEN;

        @Parameter(names = {"--vsync"}, description =
                "Enable vsync. Default value = " + DEFAULT_VSYNC + ".")
        public boolean vsync = DEFAULT_VSYNC;

        @Parameter(names = {"--debug"}, description =
                "Enable debugging mode which turns on DEBUG-level logging, debug text rendering, " +
                        "debug shape rendering, and the ability to specify the startup screen. Default value = " +
                        DEFAULT_DEBUG + ".")
        public boolean debug = DEFAULT_DEBUG;

        @Parameter(names = {"--startScreen"}, description =
                "The screen to start the game app on. This option only works if debugging is enabled. " +
                        "Options: \"main\", \"level\". Options not case sensitive. Default value = " +
                        DEFAULT_START_SCREEN + ".")
        public String startScreen = DEFAULT_START_SCREEN;

        @Parameter(names = {"--level"}, description =
                "The level to start the game app on. This option only works if debugging is enabled and \"level\" has" +
                        " been selected as the start screen. Choose the name of the level from the Level enum class " +
                        "(not case sensitive). No default value. If the level is not found, the game will start on " +
                        "the main screen.")
        public String level = DEFAULT_LEVEL;
    }

    public static void main(String[] args) {
        DesktopAppArgs appArgs = new DesktopAppArgs();
        JCommander jCommander = JCommander.newBuilder().addObject(appArgs).build();
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            System.err.println("[Application] Error in main method while parsing parameters: " + e.getMessage());
            jCommander.usage();
            Gdx.app.exit();
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(TITLE);
        config.useVsync(appArgs.vsync);
        config.setIdleFPS(appArgs.fps);
        config.setForegroundFPS(appArgs.fps);
        config.setWindowedMode(appArgs.width, appArgs.height);
        if (appArgs.fullScreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        }

        MegamanMaverickGameParams params = new MegamanMaverickGameParams();
        params.setDebug(appArgs.debug);
        if (appArgs.debug) {
            params.setStartScreen(appArgs.startScreen.toLowerCase());
            if (appArgs.startScreen.equalsIgnoreCase("level")) {
                params.setStartLevel(Level.valueOf(appArgs.level.toUpperCase()));
            }
        }

        Game game = new MegamanMaverickGame(params);

        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void iconified(boolean isIconified) {
                game.pause();
            }

            @Override
            public void focusGained() {
                game.resume();
            }

            @Override
            public void focusLost() {
                game.pause();
            }
        });

        new Lwjgl3Application(game, config);
    }
}
