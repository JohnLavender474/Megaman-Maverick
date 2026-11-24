package com.megaman.maverick.game.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.maverick.game.engine.common.GameLogLevel
import com.megaman.maverick.game.LaunchOptions
import com.megaman.maverick.game.parseEnumOption
import com.megaman.maverick.game.resolveLaunchOptions
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.state.Performance
import com.megaman.maverick.game.MegamanMaverickGame.RunType
import com.megaman.maverick.game.MegamanMaverickGameParams
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.titles.IGameTitle
import kotlin.system.exitProcess

class DesktopGame(val windowTitle: String, val windowIconPath: String, val title: IGameTitle)

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
object DesktopRunner {

    private val DEFAULT_RUN_TYPE = RunType.GAME.name
    private val DEFAULT_PERFORMANCE = Performance.MEDIUM.name
    private const val DEFAULT_WIDTH = 640
    private const val DEFAULT_HEIGHT = 480
    private const val DEFAULT_RESIZABLE = true
    private const val DEFAULT_MAXIMIZED = false
    private const val DEFAULT_FULLSCREEN = false
    private const val DEFAULT_WRITE_LOGS_TO_FILE = false
    private const val DEFAULT_PAUSE_ON_MINIMIZED = false
    private const val DEFAULT_PAUSE_ON_FOCUS_LOST = false
    private const val DEFAULT_RESUME_ON_FOCUS_GAINED = false
    private const val DEFAULT_DEBUG_WINDOW = false
    private const val DEFAULT_DEBUG_SHAPES = false
    private const val DEFAULT_DEBUG_PATHFINDING = false
    private const val DEFAULT_POOL_CHECKS = false
    private const val DEFAULT_DEBUG_TEXT = false
    private const val DEFAULT_LOG_LEVELS = ""
    private const val DEFAULT_FIXED_STEP_SCALAR = 1.0f
    private const val DEFAULT_MUSIC_VOLUME = 0.8f
    private const val DEFAULT_SOUND_VOLUME = 0.8f
    private const val DEFAULT_ALLOW_SCREENSHOTS = false
    private const val DEFAULT_SHOW_SCREEN_CONTROLLER = false
    private const val DEFAULT_PIXEL_PERFECT = false
    private const val DEFAULT_VSYNC = false
    private const val DEFAULT_INTERPOLATION = true
    private const val DEFAULT_DIAGNOSTICS = false
    private const val DEFAULT_WORLD_CONTAINER = "simple"
    private const val DEFAULT_PATHFINDING = "async"
    private const val DEFAULT_AUTO_PERF = true
    private const val DEFAULT_LOAD_KEYBOARD_PREFS = false
    private const val DEFAULT_RESET_BINDINGS = false
    private const val EXIT_CODE_BAD_ARGS = 2

    fun run(args: Array<String>, game: DesktopGame) {
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

        val errors = ArrayList<String>()

        val runType = parseEnumOption<RunType>("--runType", appArgs.runType, errors)
        val performance = parseEnumOption<Performance>("--performance", appArgs.performance, errors)

        val level =
            if (runType == RunType.TEST && appArgs.level.isNullOrBlank()) LevelDefinition.TEST_1.name else appArgs.level
        val launchOptions = LaunchOptions(
            screen = appArgs.screen,
            level = level,
            room = appArgs.room,
            logTags = appArgs.logTags,
            weapons = appArgs.weapons,
            heartTanks = appArgs.heartTanks,
            healthTanks = appArgs.healthTanks,
            autoPerf = appArgs.autoPerf,
            players = appArgs.players,
            bot = appArgs.bot,
            mode = appArgs.mode,
            viewEdges = appArgs.viewEdges,
            saveFile = appArgs.saveFile,
            difficulty = appArgs.difficulty,
            randSeed = appArgs.randSeed ?: System.currentTimeMillis().toString()
        )
        errors.addAll(resolveLaunchOptions(launchOptions).errors)

        if (errors.isNotEmpty() || runType == null || performance == null) {
            errors.forEach { System.err.println("[Application] $it") }
            jCommander.usage()
            exitProcess(EXIT_CODE_BAD_ARGS)
        }

        println("Game loaded with arguments:")
        println("- Run Type: " + appArgs.runType)
        println("- Screen: " + appArgs.screen)
        println("- Level: " + launchOptions.level)
        println("- Room: " + appArgs.room)
        println("- Log Tags: " + appArgs.logTags)
        println("- Weapons: " + appArgs.weapons)
        println("- Heart Tanks: " + appArgs.heartTanks)
        println("- Health Tanks: " + appArgs.healthTanks)
        println("- Auto Perf: " + appArgs.autoPerf)
        println("- Players: " + appArgs.players)
        println("- Bot: " + appArgs.bot)
        println("- Mode: " + appArgs.mode)
        println("- View Edges: " + appArgs.viewEdges)
        println("- Save File: " + appArgs.saveFile)
        println("- Difficulty: " + appArgs.difficulty)
        println("- Rand Seed: " + launchOptions.randSeed)
        println("- Interpolation: " + (appArgs.interpolation ?: DEFAULT_INTERPOLATION))
        println("- Load Keyboard Prefs: " + appArgs.loadKeyboardPrefs)
        println("- Reset Bindings: " + appArgs.resetBindings)
        println("- Performance: " + appArgs.performance)
        println("- Width: " + appArgs.width)
        println("- Height: " + appArgs.height)
        println("- Fullscreen: " + appArgs.fullScreen)
        println("- Debug Shapes: " + appArgs.debugShapes)
        println("- Debug Pathfinding: " + appArgs.debugPathfinding)
        println("- Pool Checks: " + appArgs.poolChecks)
        println("- Debug Text: " + appArgs.debugText)
        println("- Log Level: " + appArgs.logLevels)
        println("- Fixed Step Scalar: " + appArgs.fixedStepScalar)
        println("- Music volume: " + appArgs.musicVolume)
        println("- Sound volume: " + appArgs.soundVolume)

        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle(game.windowTitle)

        config.setIdleFPS(performance.fps)
        config.setForegroundFPS(performance.fps)

        config.setResizable(appArgs.resizable)
        config.setWindowIcon(game.windowIconPath)
        config.setPauseWhenMinimized(false)
        config.useVsync(appArgs.vsync)
        when {
            appArgs.fullScreen -> config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
            appArgs.maximized -> config.setMaximized(true)
            else -> config.setWindowedMode(appArgs.width, appArgs.height)
        }

        val params = MegamanMaverickGameParams()
        params.title = game.title
        params.runType = runType
        params.performance = performance
        params.vsync = appArgs.vsync
        params.debugText = appArgs.debugText
        params.debugWindow = appArgs.debugWindow
        params.debugShapes = appArgs.debugShapes
        params.debugPathfinding = appArgs.debugPathfinding
        params.poolChecks = appArgs.poolChecks
        params.musicVolume = appArgs.musicVolume
        params.soundVolume = appArgs.soundVolume
        params.pixelPerfect = appArgs.pixelPerfect
        params.writeLogsToFile = appArgs.writeLogsToFile
        params.fixedStepScalar = appArgs.fixedStepScalar
        // TODO: the screenshot trigger is hard-coded to the `P` key; it moves to a system key or a binding with the
        //  input work of item 031 (see its NOTES.md, "Owner's answers on bindings")
        if (appArgs.allowScreenshots) params.screenshotTrigger = { Gdx.input.isKeyJustPressed(Input.Keys.P) }
        params.interpolation = appArgs.interpolation ?: DEFAULT_INTERPOLATION
        // TODO: the interpolation dev key is hard-coded to the `I` key, in the same way as the screenshot key `P` (item 134)
        if (appArgs.interpolation != null) params.interpolationToggle = { Gdx.input.isKeyJustPressed(Input.Keys.I) }
        params.showScreenController = appArgs.showScreenController
        params.diagnostics = appArgs.diagnostics
        params.worldContainer = appArgs.worldContainer
        params.pathfinding = appArgs.pathfinding
        params.loadKeyboardPrefs = appArgs.loadKeyboardPrefs
        params.resetBindings = appArgs.resetBindings
        params.launchOptions = launchOptions

        val logLevels = appArgs.logLevels.replace(Regex("\\s+"), "").split(",").filter { !it.isBlank() }
        try {
            logLevels.forEach {
                val logLevel = GameLogLevel.valueOf(it.uppercase())
                params.logLevels.add(logLevel)
            }
        } catch (e: Exception) {
            System.err.println("Exception while setting log level: $e")
        }

        val megaGame = MegamanMaverickGame(params)

        config.setWindowListener(object : Lwjgl3WindowAdapter() {

            override fun focusLost() {
                if (appArgs.pauseOnFocusLost && !megaGame.clock.paused) megaGame.pause()
            }

            override fun focusGained() {
                if (appArgs.resumeOnFocusGained && megaGame.clock.paused) megaGame.resume()
            }

            override fun iconified(isIconified: Boolean) {
                if (appArgs.pauseOnMinimized && !megaGame.clock.paused) megaGame.pause()
            }
        })

        try {
            Lwjgl3Application(megaGame, config)
        } catch (e1: Exception) {
            e1.printStackTrace()

            try {
                megaGame.dispose()
            } catch (e2: Exception) {
                throw Exception("Caught exception while disposing game: $e2")
            }

            throw e1
        }
    }

    class DesktopAppArgs {
        @Parameter(
            names = ["--runType"],
            description = "Deprecated: use --level TEST_1 instead of 'test'. The type of run to perform ('game' or 'test')"
        )
        var runType = DEFAULT_RUN_TYPE

        @Parameter(
            names = ["--performance"],
            description = "Performance of the game: low, medium, or high.",
        )
        var performance = DEFAULT_PERFORMANCE

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
            names = ["--resumeOnFocusGained"],
            description = "Whether to resume the application when the window gains focus. " +
                "Default value = $DEFAULT_RESUME_ON_FOCUS_GAINED"
        )
        var resumeOnFocusGained = DEFAULT_RESUME_ON_FOCUS_GAINED

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
            names = ["--debugPathfinding"],
            description = ("Draw the current path of each pathfinding entity as lines. Default value = " +
                "$DEFAULT_DEBUG_PATHFINDING.")
        )
        var debugPathfinding = DEFAULT_DEBUG_PATHFINDING

        @Parameter(
            names = ["--poolChecks"],
            description = ("Poison each pooled scratch object at reclaim (NaN) and never reuse it, so that a " +
                "scratch object that is kept too long shows at once. Dev runs only. Default value = " +
                "$DEFAULT_POOL_CHECKS.")
        )
        var poolChecks = DEFAULT_POOL_CHECKS

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
            description = ("Sets if the screen controller UI should be shown. Default value is " +
                "$DEFAULT_SHOW_SCREEN_CONTROLLER")
        )
        var showScreenController = DEFAULT_SHOW_SCREEN_CONTROLLER

        @Parameter(
            names = ["--allowScreenshots"],
            description = ("Sets if pressing the SCREENSHOT key will trigger a screenshot of the game to be taken. " +
                "Default value is $DEFAULT_ALLOW_SCREENSHOTS")
        )
        var allowScreenshots = DEFAULT_ALLOW_SCREENSHOTS

        @Parameter(
            names = ["--pixelPerfect"],
            description = ("Sets whether to use a pixel-perfect viewport. Default value is $DEFAULT_PIXEL_PERFECT")
        )
        var pixelPerfect = DEFAULT_PIXEL_PERFECT

        @Parameter(
            names = ["--vsync"],
            description = ("Sets whether to enable VSync.")
        )
        var vsync = DEFAULT_VSYNC

        @Parameter(
            names = ["--diagnostics"],
            description = ("Enable runtime diagnostics. Writes per-frame timing data to a " +
                "<epoch>-diagnostics.txt file in the working directory. Default value is $DEFAULT_DIAGNOSTICS")
        )
        var diagnostics = DEFAULT_DIAGNOSTICS

        @Parameter(
            names = ["--worldContainer"],
            description = "Specify which world container type to use for the world system"
        )
        var worldContainer = DEFAULT_WORLD_CONTAINER

        @Parameter(
            names = ["--pathfinding"],
            description = "Specify which pathfiding to use. Default value is $DEFAULT_PATHFINDING"
        )
        var pathfinding = DEFAULT_PATHFINDING

        @Parameter(
            names = ["--screen"],
            description = "The screen to open at start, for example MAIN_MENU_SCREEN. With --level and a screen " +
                "that is not LEVEL_SCREEN, the level is staged: the next level start with no level choice loads it."
        )
        var screen: String? = null

        @Parameter(
            names = ["--level"],
            description = "The level to start (or to stage, with --screen), for example MOON_MAN."
        )
        var level: String? = null

        @Parameter(
            names = ["--room"],
            description = "Needs --level. On the first level start, start at the first player spawn in this room."
        )
        var room: String? = null

        @Parameter(
            names = ["--logTags"],
            description = "Comma-separated log tags to print, or 'all' to print every tag."
        )
        var logTags: String? = null

        @Parameter(
            names = ["--weapons"],
            description = "Comma-separated weapons to give Megaman at start, for example MOON_SCYTHES, or 'all'. " +
                "The weapons are not written to the save file."
        )
        var weapons: String? = null

        @Parameter(
            names = ["--heartTanks"],
            description = "Comma-separated heart tanks to give Megaman at start, for example A,B, or 'all'. " +
                "When given, the game does not write the save file."
        )
        var heartTanks: String? = null

        @Parameter(
            names = ["--healthTanks"],
            description = "Comma-separated health tanks to give Megaman at start, full, for example A,B, or 'all'. " +
                "When given, the game does not write the save file."
        )
        var healthTanks: String? = null

        @Parameter(
            names = ["--players"],
            description = "Dev option: the number of players at start, 1 or 2. Default value = 1."
        )
        var players: String? = null

        @Parameter(
            names = ["--bot"],
            description = "Dev option: a bot in slot 1, next to 1 player. Kinds: follow (copies player 1's input " +
                "with a delay). A session with a bot uses the coop rules, with WARP at the left and right view " +
                "edges, and the camera follows player 1 only. An error with --players 2 or --mode pvp."
        )
        var bot: String? = null

        @Parameter(
            names = ["--mode"],
            description = "Dev option: the rules of a session with 2 players, coop or pvp. Default value = coop."
        )
        var mode: String? = null

        @Parameter(
            names = ["--viewEdges"],
            description = "Dev option, needs --players 2 or --bot: the rule at each edge of the shared view, " +
                "for example left=WALL,right=WALL,top=NONE,bottom=WARP,delay=1.5,damage=3,cap=3. " +
                "Rules: NONE, WALL, BOUNCE, WARP, KILL. A missing key keeps its default " +
                "(left=WALL,right=WALL,top=NONE,bottom=NONE,delay=0,damage=0,cap=3). " +
                "cap is the most seconds after the delay that a WARP waits for a safe spot."
        )
        var viewEdges: String? = null

        @Parameter(
            names = ["--saveFile"],
            description = "Dev option: the name of the preferences file that holds the save, in place of the game's " +
                "own save file, for example test-save (a file under ~/.prefs/). A new name starts with no save."
        )
        var saveFile: String? = null

        @Parameter(
            names = ["--difficulty"],
            description = "Dev option: the difficulty mode at start, normal or hard. A later save load or a choice " +
                "in the difficulty screen replaces it. When given, the game does not write the save file."
        )
        var difficulty: String? = null

        @Parameter(
            names = ["--randSeed"],
            description = "Dev option: the seed of the game's random source, a whole number. With no value, the " +
                "game takes the clock time and prints it as the seed at start."
        )
        var randSeed: String? = null

        @Parameter(
            names = ["--interpolation"],
            arity = 1,
            description = "Dev option: draw bodies and the camera between the last 2 physics steps. " +
                "When the option is given, the I key switches it on and off. Default value is $DEFAULT_INTERPOLATION"
        )
        var interpolation: Boolean? = null

        @Parameter(
            names = ["--autoPerf"],
            arity = 1,
            description = "Whether the game may change the performance tier at runtime. Default value is $DEFAULT_AUTO_PERF"
        )
        var autoPerf = DEFAULT_AUTO_PERF

        @Parameter(
            names = ["--loadKeyboardPrefs"],
            description = "Load the saved keyboard settings at startup, the same as \"LOAD SAVED SETTINGS\" in the " +
                "keyboard settings screen. Default value = $DEFAULT_LOAD_KEYBOARD_PREFS."
        )
        var loadKeyboardPrefs = DEFAULT_LOAD_KEYBOARD_PREFS

        @Parameter(
            names = ["--resetBindings"],
            description = "Clears the saved bindings and the saved select action, at every launch that passes this " +
                "flag. Default value = $DEFAULT_RESET_BINDINGS."
        )
        var resetBindings = DEFAULT_RESET_BINDINGS
    }
}
