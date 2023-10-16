package com.megaman.maverick.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {

    private static final int FPS = 60;
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final boolean VSYNC = false;
    private static final String TITLE = "Megaman Maverick";

    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(TITLE);
        config.useVsync(VSYNC);
        config.setIdleFPS(FPS);
        config.setForegroundFPS(FPS);
        config.setWindowedMode(WIDTH, HEIGHT);
        Game game = new MegamanMaverickGame();
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
