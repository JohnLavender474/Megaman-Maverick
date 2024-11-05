package com.megaman.maverick.game.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.mega.game.engine.common.GameLogLevel
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.MegamanMaverickGameParams
import com.megaman.maverick.game.StartScreenOption
import com.megaman.maverick.game.screens.levels.Level
import kotlin.system.exitProcess

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
object DesktopLauncher {

    private const val DEFAULT_WIDTH = 800
    private const val DEFAULT_HEIGHT = 600
    private const val DEFAULT_FULLSCREEN = false
    private const val DEFAULT_DEBUG_SHAPES = false
    private const val DEFAULT_DEBUG_FPS = false
    private const val DEFAULT_LOG_LEVEL = "off"
    private const val DEFAULT_FIXED_STEP_SCALAR = 1.0f
    private const val DEFAULT_MUSIC_VOLUME = 0.5f
    private const val DEFAULT_SOUND_VOLUME = 0.5f
    private const val DEFAULT_START_SCREEN = "simple"
    private const val DEFAULT_LEVEL = "null"
    private const val DEFAULT_SHOW_SCREEN_CONTROLLER = false
    private const val TITLE = "Megaman Maverick"

    @JvmStatic
    fun main(args: Array<String>) {
        if (StartupHelper.startNewJvmIfRequired()) return

        val appArgs = DesktopAppArgs()
        val jCommander = JCommander.newBuilder().addObject(appArgs).build()
        try {
            jCommander.parse(*args)
        } catch (e: ParameterException) {
            System.err.println("[Application] Error in main method while parsing parameters: " + e.message)
            e.printStackTrace()
            jCommander.usage()
            exitProcess(1)
        }

        println("Game loaded with arguments:")
        println("- Width: " + appArgs.width)
        println("- Height: " + appArgs.height)
        println("- Fullscreen: " + appArgs.fullScreen)
        println("- Debug Shapes: " + appArgs.debugShapes)
        println("- Debug FPS: " + appArgs.debugFPS)
        println("- Log Level: " + appArgs.logLevel)
        println("- Start Screen: " + appArgs.startScreen)
        println("- Level: " + appArgs.level)
        println("- Fixed Step Scalar: " + appArgs.fixedStepScalar)
        println("- Music volume: " + appArgs.musicVolume)
        println("- Sound volume: " + appArgs.soundVolume)

        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle(TITLE)
        config.setIdleFPS(ConstVals.FPS)
        config.setForegroundFPS(ConstVals.FPS)
        config.setWindowedMode(appArgs.width, appArgs.height)
        if (appArgs.fullScreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
        }

        val params = MegamanMaverickGameParams()
        params.debugShapes = appArgs.debugShapes
        params.debugFPS = appArgs.debugFPS
        params.fixedStepScalar = appArgs.fixedStepScalar
        params.musicVolume = appArgs.musicVolume
        params.soundVolume = appArgs.soundVolume
        params.showScreenController = appArgs.showScreenController
        try {
            params.logLevel = GameLogLevel.valueOf(appArgs.logLevel.uppercase())
        } catch (e: Exception) {
            System.err.println("Exception while setting log level: $e")
        }
        val startScreenOption = when {
            appArgs.startScreen.isBlank() || appArgs.startScreen.lowercase() == "main" -> StartScreenOption.MAIN
            appArgs.startScreen.lowercase() == "level" -> StartScreenOption.LEVEL
            appArgs.startScreen.lowercase() == "simple" -> StartScreenOption.SIMPLE
            else -> {
                System.err.println("Invalid start screen option: " + appArgs.startScreen)
                jCommander.usage()
                exitProcess(1)
            }
        }
        params.startScreen = startScreenOption
        if (startScreenOption == StartScreenOption.LEVEL) {
            val level = Level.valueOf(appArgs.level.uppercase())
            params.startLevel = level
        }

        val game = MegamanMaverickGame(params)

        config.setWindowListener(object : Lwjgl3WindowAdapter() {
            @Override
            override fun iconified(isIconified: Boolean) {
                game.pause()
            }

            @Override
            override fun focusGained() {
                game.resume()
            }

            @Override
            override fun focusLost() {
                game.pause()
            }
        })

        try {
            Lwjgl3Application(game, config)
        } catch (e: Exception) {
            System.err.println("Exception while running game!")
            System.err.println("Exception message: " + e.message)
            System.err.println("Exception stacktrace: ")
            e.printStackTrace()
            game.dispose()
        }
    }

    class DesktopAppArgs {
        @Parameter(
            names = ["--width"],
            description = "Window width: min of 600. Default value = $DEFAULT_WIDTH."
        )
        var width = DEFAULT_WIDTH

        @Parameter(
            names = ["--height"],
            description = "Window height: min of 400. Default value = $DEFAULT_HEIGHT."
        )
        var height = DEFAULT_HEIGHT

        @Parameter(
            names = ["--fullscreen"],
            description = "Enable fullscreen. Default value = $DEFAULT_FULLSCREEN."
        )
        var fullScreen = DEFAULT_FULLSCREEN

        @Parameter(
            names = ["--debugShapes"],
            description = ("Enable debugging shapes. Default value = $DEFAULT_DEBUG_SHAPES.")
        )
        var debugShapes = DEFAULT_DEBUG_SHAPES

        @Parameter(
            names = ["--debugFPS"],
            description = ("Enable debugging FPS. Default value = $DEFAULT_DEBUG_FPS")
        )
        var debugFPS = DEFAULT_DEBUG_FPS

        @Parameter(
            names = ["--logLevel"],
            description = ("Set the log level of the game logger. Default value = $DEFAULT_LOG_LEVEL")
        )
        var logLevel = DEFAULT_LOG_LEVEL

        @Parameter(
            names = ["--startScreen"],
            description = ("The screen to start the game app on. Options: \"main\", " +
                "\"level\", \"simple\". Options not case sensitive. Default value = " + DEFAULT_START_SCREEN + ".")
        )
        var startScreen = DEFAULT_START_SCREEN

        @Parameter(
            names = ["--level"],
            description =
                ("The level to start the game app on. This option only works if " +
                    "\"level\" has been selected as the start screen. Choose the name of the level from the Level " +
                    "enum class (not case sensitive). No default value. If the level is not found, an exception is " +
                    "thrown.")
        )
        var level = DEFAULT_LEVEL

        @Parameter(
            names = ["--fixedStepScalar"],
            description = ("Sets the world fixed step scalar, useful for " +
                "debugging. Default value is " + DEFAULT_FIXED_STEP_SCALAR + ". Should be default value if not " +
                "debugging")
        )
        var fixedStepScalar = DEFAULT_FIXED_STEP_SCALAR

        @Parameter(
            names = ["--musicVolume"],
            description = ("Sets the music volume. Must be between 0 and 1. Default " +
                "value is " + DEFAULT_MUSIC_VOLUME)
        )
        var musicVolume = DEFAULT_MUSIC_VOLUME

        @Parameter(
            names = ["--soundVolume"],
            description = ("Sets the sound volume. Must be between 0 and 1. Default " +
                "value is " + DEFAULT_SOUND_VOLUME)
        )
        var soundVolume = DEFAULT_SOUND_VOLUME

        @Parameter(
            names = ["--showScreenController"],
            description = ("Sets if the screen controller UI should be shown. Default " +
                "value is " + DEFAULT_SHOW_SCREEN_CONTROLLER)
        )
        var showScreenController = DEFAULT_SHOW_SCREEN_CONTROLLER
    }
}
