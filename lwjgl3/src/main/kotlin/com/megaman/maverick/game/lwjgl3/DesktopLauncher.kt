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
import kotlin.system.exitProcess

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
object DesktopLauncher {

    private const val TITLE = "Megaman Maverick"
    private const val WINDOW_ICON_PATH = "Megaman.png"

    private const val DEFAULT_WIDTH = 1920
    private const val DEFAULT_HEIGHT = 1080
    private const val DEFAULT_RESIZABLE = true
    private const val DEFAULT_MAXIMIZED = true
    private const val DEFAULT_FULLSCREEN = false
    private const val DEFAULT_WRITE_LOGS_TO_FILE = false
    private const val DEFAULT_PAUSE_ON_MINIMIZED = true
    private const val DEFAULT_PAUSE_ON_FOCUS_LOST = true
    private const val DEFAULT_DEBUG_WINDOW = false
    private const val DEFAULT_DEBUG_SHAPES = false
    private const val DEFAULT_DEBUG_TEXT = false
    private const val DEFAULT_LOG_LEVELS = ""
    private const val DEFAULT_FIXED_STEP_SCALAR = 1.0f
    private const val DEFAULT_MUSIC_VOLUME = 0.8f
    private const val DEFAULT_SOUND_VOLUME = 0.8f
    private const val DEFAULT_SHOW_SCREEN_CONTROLLER = false

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
        println("- Debug FPS: " + appArgs.debugText)
        println("- Log Level: " + appArgs.logLevels)
        println("- Fixed Step Scalar: " + appArgs.fixedStepScalar)
        println("- Music volume: " + appArgs.musicVolume)
        println("- Sound volume: " + appArgs.soundVolume)

        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle(TITLE)
        config.setIdleFPS(ConstVals.FPS)
        config.setForegroundFPS(ConstVals.FPS)
        config.setResizable(appArgs.resizable)
        config.setPauseWhenLostFocus(appArgs.pauseOnFocusLost)
        config.setPauseWhenMinimized(appArgs.pauseOnMinimized)
        config.setWindowIcon(WINDOW_ICON_PATH)
        when {
            appArgs.fullScreen -> config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
            appArgs.maximized -> config.setMaximized(true)
            else -> config.setWindowedMode(appArgs.width, appArgs.height)
        }

        val params = MegamanMaverickGameParams()
        params.writeLogsToFile = appArgs.writeLogsToFile
        params.debugWindow = appArgs.debugWindow
        params.debugShapes = appArgs.debugShapes
        params.debugText = appArgs.debugText
        params.fixedStepScalar = appArgs.fixedStepScalar
        params.musicVolume = appArgs.musicVolume
        params.soundVolume = appArgs.soundVolume
        params.showScreenController = appArgs.showScreenController

        val logLevels = appArgs.logLevels.replace("\\s+", "").split(",").filter { !it.isBlank() }
        try {
            logLevels.forEach {
                val logLevel = GameLogLevel.valueOf(it.uppercase())
                params.logLevels.add(logLevel)
            }
        } catch (e: Exception) {
            System.err.println("Exception while setting log level: $e")
        }

        val game = MegamanMaverickGame(params)

        config.setWindowListener(object : Lwjgl3WindowAdapter() {

            override fun focusLost() {
                if (!game.paused) game.pause()
            }

            override fun focusGained() {
                if (game.paused) game.resume()
            }
        })

        try {
            Lwjgl3Application(game, config)
        } catch (e: Exception) {
            game.dispose()
            throw e
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
            names = ["--maximized"],
            description = "Whether the game should start maximized. Default value = $DEFAULT_MAXIMIZED"
        )
        var maximized = DEFAULT_MAXIMIZED

        @Parameter(
            names = ["--fullScreen"],
            description = "Enable fullscreen. Default value = $DEFAULT_FULLSCREEN."
        )
        var fullScreen = DEFAULT_FULLSCREEN

        @Parameter(
            names = ["--resizable"],
            description = "Whether to enable the game window to be resized. Default value = $DEFAULT_RESIZABLE"
        )
        var resizable = DEFAULT_RESIZABLE

        @Parameter(
            names = ["--pauseOnFocusLost"],
            description = "Whether to pause the application when the window loses focus. " +
                "Default value = $DEFAULT_PAUSE_ON_FOCUS_LOST"
        )
        var pauseOnFocusLost = DEFAULT_PAUSE_ON_FOCUS_LOST

        @Parameter(
            names = ["--pauseOnMinimized"],
            description = "Whether to pause the application when the window is minimized. " +
                "Default value = $DEFAULT_PAUSE_ON_MINIMIZED"
        )
        var pauseOnMinimized = DEFAULT_PAUSE_ON_MINIMIZED

        @Parameter(
            names = ["--writeLogsToFile"],
            description = "Write logs to a log file. Default value = $DEFAULT_WRITE_LOGS_TO_FILE."
        )
        var writeLogsToFile = DEFAULT_WRITE_LOGS_TO_FILE

        @Parameter(
            names = ["--debugWindow"],
            description = ("Enable displaying a secondary window for logs. Default value = $DEFAULT_DEBUG_WINDOW.")
        )
        var debugWindow = DEFAULT_DEBUG_WINDOW

        @Parameter(
            names = ["--debugShapes"],
            description = ("Enable debugging shapes. Default value = $DEFAULT_DEBUG_SHAPES.")
        )
        var debugShapes = DEFAULT_DEBUG_SHAPES

        @Parameter(
            names = ["--debugText"],
            description = ("Enable debug text to be displayed on the screen. Default value = $DEFAULT_DEBUG_TEXT")
        )
        var debugText = DEFAULT_DEBUG_TEXT

        @Parameter(
            names = ["--logLevels"],
            description = ("Set the log levels of the game logger. Each log level should be separated with a comma. " +
                "Default value = $DEFAULT_LOG_LEVELS")
        )
        var logLevels = DEFAULT_LOG_LEVELS

        @Parameter(
            names = ["--fixedStepScalar"],
            description = ("Sets the world fixed step scalar, useful for debugging. Default value is " +
                "$DEFAULT_FIXED_STEP_SCALAR. Should be default value if not debugging")
        )
        var fixedStepScalar = DEFAULT_FIXED_STEP_SCALAR

        @Parameter(
            names = ["--musicVolume"],
            description = ("Sets the music volume. Must be between 0 and 1. Default value is $DEFAULT_MUSIC_VOLUME")
        )
        var musicVolume = DEFAULT_MUSIC_VOLUME

        @Parameter(
            names = ["--soundVolume"],
            description = ("Sets the sound volume. Must be between 0 and 1. Default value is $DEFAULT_SOUND_VOLUME")
        )
        var soundVolume = DEFAULT_SOUND_VOLUME

        @Parameter(
            names = ["--showScreenController"],
            description = ("Sets if the screen controller UI should be shown. Default value is $DEFAULT_SHOW_SCREEN_CONTROLLER")
        )
        var showScreenController = DEFAULT_SHOW_SCREEN_CONTROLLER
    }
}
