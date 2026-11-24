package com.megaman.maverick.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsSystem
import com.maverick.game.engine.audio.AudioSystem
import com.maverick.game.engine.behaviors.BehaviorsSystem
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.GameLogLevel
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.LogReceiver
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.InsertionOrderPriorityQueue
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.random.SeededRandom
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.cullables.CullablesSystem
import com.maverick.game.engine.cullables.GameEntityCuller
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.drawables.BodyInterpolation
import com.maverick.game.engine.drawables.fonts.BitmapFontHandle
import com.maverick.game.engine.drawables.fonts.FontsSystem
import com.maverick.game.engine.drawables.shapes.DrawableShapesSystem
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sorting.IComparableDrawable
import com.maverick.game.engine.drawables.sprites.SpritesSystem
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.EventsManager
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.groups.GroupsSystem
import com.maverick.game.engine.input.InputFrameBuilder
import com.maverick.game.engine.input.checkInputButtons
import com.maverick.game.engine.motion.MotionSystem
import com.maverick.game.engine.pathfinding.AsyncPathfindingSystem
import com.maverick.game.engine.pathfinding.PathfinderResult
import com.maverick.game.engine.pathfinding.SimplePathfindingSystem
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.points.PointsSystem
import com.maverick.game.engine.screens.IScreen
import com.maverick.game.engine.screens.viewports.PixelPerfectFitViewport
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.WorldWrap
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.MegamanMaverickGame.RunType
import com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.ScreenController
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.UnderWaterBubble
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.players.weapons
import com.megaman.maverick.game.levels.WeaponRewardListener
import com.megaman.maverick.game.entities.megamanx.MegamanX
import com.megaman.maverick.game.entities.protoman.Protoman
import com.megaman.maverick.game.entities.state.PlayerProgressComponent
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.input.GamepadInput
import com.megaman.maverick.game.input.KeyboardInput
import com.megaman.maverick.game.input.PadShare
import com.megaman.maverick.game.input.SystemButton
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelOverride
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.pathfinding.MegaPathfinderFactory
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.PlayerCharacter
import com.megaman.maverick.game.players.PlayerRoster
import com.megaman.maverick.game.players.PlayerSettingsStore
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.players.applyBootSettings
import com.megaman.maverick.game.players.bots.BotKind
import com.megaman.maverick.game.players.bots.FollowBrain
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.debug.DebugWindow
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.menus.*
import com.megaman.maverick.game.screens.menus.level.LevelSelectScreenV2
import com.megaman.maverick.game.screens.other.*
import com.megaman.maverick.game.state.AutoPerformance
import com.megaman.maverick.game.state.GameClock
import com.megaman.maverick.game.state.GameSettings
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.state.Performance
import com.megaman.maverick.game.titles.IGameTitle
import com.megaman.maverick.game.titles.MaverickTitle
import com.megaman.maverick.game.utils.AsyncFileWriter
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getMusics
import com.megaman.maverick.game.utils.extensions.getSounds
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition
import com.megaman.maverick.game.utils.extensions.stopSmoothFollow
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import com.megaman.maverick.game.world.wrap.WrapPolicy
import com.megaman.maverick.game.world.wrap.wrapRegion
import java.time.LocalDateTime
import java.util.zip.Deflater
import kotlin.reflect.KClass
import kotlin.reflect.cast

class MegamanMaverickGameParams {
    lateinit var performance: Performance
    var runType: RunType = RunType.GAME
    var writeLogsToFile: Boolean = false
    var debugWindow: Boolean = false
    var debugShapes: Boolean = false
    var debugPathfinding: Boolean = false
    var poolChecks: Boolean = false
    var debugText: Boolean = false
    var fixedStepScalar: Float = 1f
    var pixelPerfect: Boolean = false
    var musicVolume: Float = 0.5f
    var soundVolume: Float = 0.5f
    var screenshotTrigger: (() -> Boolean)? = null
    var interpolation: Boolean = false
    var interpolationToggle: (() -> Boolean)? = null
    var showScreenController: Boolean = false
    var vsync: Boolean = false
    var diagnostics: Boolean = false
    var logLevels: OrderedSet<GameLogLevel> = OrderedSet()
    var worldContainer: String = "simple"
    var pathfinding: String = "async"
    var loadKeyboardPrefs: Boolean = false
    var resetBindings: Boolean = false
    var launchOptions: LaunchOptions = LaunchOptions()
    var title: IGameTitle = MaverickTitle
}

class MegamanMaverickGame(
    val params: MegamanMaverickGameParams = MegamanMaverickGameParams()
) : Game(), IEventListener {

    enum class RunType { GAME, TEST }

    companion object {
        const val TAG = "MegamanMaverickGame"
        const val VERSION = "" // ""ALPHA 1.15.0"
        private const val ASSET_MILLIS = 17
        private const val LOADING = "LOADING"
        private const val LOG_FILE_NAME = "logs.txt"
        private const val NOTIFICATION_DUR = 5f
        val TAGS_TO_LOG: ObjectSet<String> = objectSetOf()
        val CONTACT_LISTENER_DEBUG_FILTER: (IFixture, IFixture) -> Boolean = { fixture1, fixture2 ->
            ((fixture1.getType() == FixtureType.WATER && fixture2.getType() == FixtureType.WATER_LISTENER) ||
                (fixture2.getType() == FixtureType.WATER && fixture1.getType() == FixtureType.WATER_LISTENER)) &&
                (fixture1.getEntity() is UnderWaterBubble || fixture2.getEntity() is UnderWaterBubble)
        }
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.TURN_CONTROLLER_ON,
        EventType.TURN_CONTROLLER_OFF,
        EventType.TOGGLE_PIXEL_PERFECT
    )

    val levelState = LevelState()
    val clock = GameClock()
    val cameraRotation = CameraRotation {
        cameraController.stopSmoothFollow()
        events.submitEvent(Event(EventType.END_GAME_CAM_ROTATION))
    }
    val gameCamera = OrthographicCamera()
    val cameraView = CameraView(gameCamera, cameraRotation)
    val cameraController = CameraController(gameCamera)
    val settings = GameSettings()

    lateinit var services: MegaServices
        private set
    lateinit var drawables: ObjectMap<DrawingSection, InsertionOrderPriorityQueue<IComparableDrawable<Batch>>>
        private set
    lateinit var shapes: Array<IDrawableShape>
        private set
    private lateinit var systems: ObjectMap<String, GameSystem>

    val viewports = ObjectMap<String, Viewport>()
    val screens = ObjectMap<String, IScreen>()
    val currentScreen: IScreen?
        get() = currentScreenKey?.let { screens[it] }

    val updatables = OrderedMap<String, Updatable>()
    val disposables = OrderedMap<String, Disposable>()

    val runQueue = Queue<() -> Unit>()

    lateinit var batch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer

    private lateinit var settingsStore: PlayerSettingsStore
    lateinit var gamepadInput: GamepadInput
    private var slot1GamepadInput: GamepadInput? = null
    private var followBrain: FollowBrain? = null
    private lateinit var keyboardInput: KeyboardInput
    private val frameBuilders = List(MAX_PLAYERS) { InputFrameBuilder(MegaControllerButton.entries) }
    private val systemFrameBuilder = InputFrameBuilder(SystemButton.entries)
    val systemInput get() = systemFrameBuilder.frame
    val inputLocked get() = frameBuilders[0].locked

    lateinit var assets: AssetManager
    lateinit var events: EventsManager
    lateinit var audio: MegaAudioManager

    lateinit var engine: GameEngine
    lateinit var bodyInterpolation: BodyInterpolation
        private set
    lateinit var gameState: GameState

    val players = MegaPlayers(MAX_PLAYERS)
    private val roster = PlayerRoster(players)
    val gravity = WorldGravity(players)

    private var currentScreenKey: String? = null
    private var heldLevelScreenKey: String? = null
    private val heldUiCameraPosition = Vector3()
    private var screenController: ScreenController? = null

    private lateinit var debugText: MegaFontHandle
    private lateinit var loadingText: MegaFontHandle
    private var finishedLoadingAssets = false

    private lateinit var notificationFont: BitmapFontHandle
    private var notificationTimer = Timer(NOTIFICATION_DUR)
    private var notificationText = ""
    private var showNotification = false
    private var notificationColor = Color.WHITE

    private val autoPerformance = AutoPerformance()

    private var logFileWriter: AsyncFileWriter? = null
    private var debugWindow: DebugWindow? = null

    private var pixelPerfect = false
    private var vsync = false

    var diagnostics: RuntimeDiagnostics? = null
        private set

    private lateinit var launch: ResolvedLaunch
    private lateinit var saveFileName: String
    private var launchRoom: String? = null
    private var launchRoomLevel: LevelDefinition? = null

    override fun create() {
        settings.performance = params.performance
        GameObjectPools.debugChecks = params.poolChecks

        val launchResult = resolveLaunchOptions(params.launchOptions)
        if (!launchResult.isValid)
            throw IllegalArgumentException("Invalid launch options: ${launchResult.errors.joinToString("; ")}")
        launch = launchResult.resolved!!
        launchRoom = launch.room
        saveFileName = launch.saveFile ?: params.title.saveFileName
        if (saveFileName != params.title.saveFileName) GameLogger.log(TAG, "create(): save file: $saveFileName")
        launchRoomLevel = when (val start = launch.start) {
            is LaunchStart.StartLevel -> start.level
            is LaunchStart.OpenScreen -> start.stagedLevel
            LaunchStart.Default -> null
        }

        params.logLevels.forEach { GameLogger.setLogLevel(it, true) }

        // only print errors to terminal; all other log levels should only be displayed in the logger window
        GameLogger.logReceivers.add(object : LogReceiver {

            override fun receive(
                fullMessage: String,
                time: String,
                level: GameLogLevel,
                tag: String,
                message: String,
                throwable: Throwable?
            ) {
                if (level == GameLogLevel.ERROR) println(fullMessage)
            }
        })

        GameLogger.tagsToLog.addAll(TAGS_TO_LOG)
        launch.logTags.forEach { GameLogger.tagsToLog.add(it) }
        GameLogger.filterByTag = !launch.logAllTags

        GameLogger.log(TAG, "create(): appType=${Gdx.app.type}")

        shapeRenderer = ShapeRenderer()
        shapeRenderer.setAutoShapeType(true)
        batch = SpriteBatch()

        checkInputButtons(MegaControllerButton.entries)
        settingsStore = PlayerSettingsStore(PlayerSlot(0), params.title.playerSettingsFileName)
        applyBootSettings(settingsStore, players.settings(PlayerSlot(0)), params.loadKeyboardPrefs, params.resetBindings)
        if (params.resetBindings) GameLogger.log(TAG, "create(): cleared the saved bindings")
        if (params.loadKeyboardPrefs) GameLogger.log(TAG, "create(): loaded keyboard prefs")
        keyboardInput = KeyboardInput(players.settings(PlayerSlot(0)))
        val slot1Pad = launch.slots > 1 && launch.bot == null
        val slot0Pads = if (slot1Pad) PadShare.ALL_BUT_LAST else PadShare.ALL
        gamepadInput = GamepadInput(players.settings(PlayerSlot(0)), settingsStore, slot0Pads)
        if (slot1Pad) slot1GamepadInput = GamepadInput(
            players.settings(PlayerSlot(1)), PlayerSettingsStore(PlayerSlot(1), params.title.playerSettingsFileName), PadShare.LAST_ONLY
        )
        followBrain = when (launch.bot) {
            BotKind.FOLLOW -> FollowBrain(players, clock)
            null -> null
        }

        events = EventsManager()
        events.addListener(this)

        if (params.diagnostics)
            diagnostics = RuntimeDiagnostics("${System.currentTimeMillis()}-diagnostics.txt")

        engine = createGameEngine()
        gameState = GameState()

        this.pixelPerfect = params.pixelPerfect
        this.vsync = params.vsync

        val gameWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val gameHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        gameCamera.setToDefaultPosition()

        val gameViewport = when {
            pixelPerfect -> PixelPerfectFitViewport(gameWidth, gameHeight, gameCamera)
            else -> FitViewport(gameWidth, gameHeight, gameCamera)
        }
        viewports.put(ConstKeys.GAME, gameViewport)

        val uiWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val uiHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val uiCamera = OrthographicCamera(uiWidth, uiHeight)
        uiCamera.setToDefaultPosition()

        val uiViewport = when {
            pixelPerfect -> PixelPerfectFitViewport(uiWidth, uiHeight, uiCamera)
            else -> FitViewport(uiWidth, uiHeight, uiCamera)
        }
        viewports.put(ConstKeys.UI, uiViewport)

        loadingText = MegaFontHandle(
            "${LOADING}: 0%",
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        )

        debugText = MegaFontHandle(
            ConstVals.EMPTY_STRING,
            positionX = ConstVals.PPM.toFloat(),
            positionY = 0.75f * ConstVals.PPM,
            centerX = false
        )

        notificationFont = BitmapFontHandle(
            textSupplier = { notificationText },
            attachment = Position.CENTER_LEFT,
            positionX = 0f,
            positionY = (ConstVals.VIEW_HEIGHT - 1f) * ConstVals.PPM
        )

        assets = AssetManager()
        queueAssets()

        if (params.writeLogsToFile) {
            logFileWriter = AsyncFileWriter(LOG_FILE_NAME)
            logFileWriter!!.init()

            GameLogger.logReceivers.add(object : LogReceiver {

                override fun receive(
                    fullMessage: String,
                    time: String,
                    level: GameLogLevel,
                    tag: String,
                    message: String,
                    throwable: Throwable?
                ) = logFileWriter!!.write(fullMessage)
            })
        }

        if (params.debugWindow) {
            debugWindow = DebugWindow()

            GameLogger.logReceivers.add(object : LogReceiver {

                override fun receive(
                    fullMessage: String,
                    time: String,
                    level: GameLogLevel,
                    tag: String,
                    message: String,
                    throwable: Throwable?
                ) = debugWindow!!.log(fullMessage)
            })
        }
    }

    private fun postCreate() {
        val sounds = OrderedMap<SoundAsset, Sound>()
        val music = OrderedMap<MusicAsset, Music>()
        audio = MegaAudioManager(assets.getSounds(sounds), assets.getMusics(music))
        audio.musicVolume = params.musicVolume
        audio.soundVolume = params.soundVolume

        services = MegaServices(
            engine,
            events,
            assets,
            audio,
            gameState,
            players,
            gravity,
            levelState,
            clock,
            settings,
            updatables,
            runQueue,
            getSystem(GroupsSystem::class)!!,
            cameraView,
            cameraController,
            launch.rules,
            @Suppress("ForbiddenMethodCall") SeededRandom(launch.randSeed ?: System.currentTimeMillis())
        )

        MegaEntityFactory.init(services)

        // TODO: should replace EntityFactories with MegaEntityFactory
        EntityFactories.initialize(services)

        for (i in 0 until launch.slots) {
            val megaman = Megaman(services)
            // manually call init and set initialized to true before megaman is spawned
            megaman.init()
            megaman.initialized = true
            // add megaman's progress component as a game state listener
            gameState.addListener(megaman.require(PlayerProgressComponent::class))

            players.register(PlayerSlot(i), megaman)
            roster.add(megaman, if (i == 0) PlayerCharacter.MEGAMAN else null)
        }
        if (launch.bot != null) {
            players.setBot(PlayerSlot(1))
            players.settings(PlayerSlot(1)).selectButtonAction = players.settings(PlayerSlot(0)).selectButtonAction
        }

        gameState.addListener(WeaponRewardListener(levelState, players))

        val megamanX = MegamanX(services)
        megamanX.init()
        megamanX.initialized = true
        gameState.addListener(megamanX.require(PlayerProgressComponent::class))
        roster.add(megamanX, PlayerCharacter.X)

        val protoman = Protoman(services)
        protoman.init()
        protoman.initialized = true
        gameState.addListener(protoman.require(PlayerProgressComponent::class))
        roster.add(protoman, PlayerCharacter.PROTOMAN)

        val levelScreen = MegaLevelScreen(this)
        levelScreen.init()
        screens.put(ScreenEnum.LEVEL_SCREEN.name, levelScreen)

        screens.put(ScreenEnum.LOGO_SCREEN.name, LogoScreen(this))
        screens.put(ScreenEnum.OPENING_SCREEN.name, OpeningScreen(this))
        screens.put(ScreenEnum.MAIN_MENU_SCREEN.name, MainMenuScreen(this))
        screens.put(ScreenEnum.SAVE_GAME_SCREEN.name, SaveGameScreen(this))
        screens.put(ScreenEnum.GAME_OVER_SCREEN.name, GameOverScreen(this))
        screens.put(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, players.settings(PlayerSlot(0)), settingsStore, true))
        screens.put(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, players.settings(PlayerSlot(0)), settingsStore, false))
        screens.put(ScreenEnum.SELECT_DIFFICULTY_SCREEN.name, SelectDifficultyScreen(this))
        screens.put(ScreenEnum.LEVEL_SELECT_SCREEN.name, LevelSelectScreenV2(this))
        // screens.put(ScreenEnum.ROBOT_MASTER_INTRO_SCREEN.name, RobotMasterIntroScreenV2(this))
        screens.put(ScreenEnum.ROBOT_MASTER_INTRO_SCREEN.name, RobotMasterIntroScreen(this))
        screens.put(ScreenEnum.GET_WEAPONS_SCREEN.name, GetWeaponScreen(this))
        screens.put(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name, SimpleInitGameScreen(this))
        screens.put(ScreenEnum.ENDING_SCREEN.name, EndingScreen(this))
        screens.put(ScreenEnum.CREDITS_SCREEN.name, CreditsScreen(this))
        screens.put(ScreenEnum.EXTRAS_SCREEN.name, ExtrasScreen(this))
        params.title.registerScreens(this)

        val start = launch.start
        if (start is LaunchStart.StartLevel) start.level.levelOverride?.let { applyLevelOverride(it) }
        launch.weapons.forEach { gameState.addWeaponToEachLoadout(it) }
        launch.heartTanks.forEach { gameState.addHeartTank(it) }
        launch.healthTanks.forEach { gameState.fillHealthTank(it) }
        launch.difficulty?.let { gameState.setDifficultyMode(it) }

        when (start) {
            LaunchStart.Default -> params.title.openDefaultStart(this)

            is LaunchStart.StartLevel -> startLevel(start.level)

            is LaunchStart.OpenScreen -> {
                if (!screens.containsKey(start.screen.name))
                    throw IllegalStateException("Launch option --screen: screen ${start.screen.name} is not registered")
                start.stagedLevel?.let { levelState.currentLevel = it }
                setCurrentScreen(start.screen.name)
            }
        }
    }

    override fun render() {
        diagnostics?.beginFrame()

        diagnostics?.beginEntry("clearScreen")
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)
        diagnostics?.endEntry()

        val delta = Gdx.graphics.deltaTime

        setDebugText("FPS: ${Gdx.graphics.framesPerSecond}")

        diagnostics?.beginEntry("runQueue")
        while (!runQueue.isEmpty) {
            val runnable = runQueue.removeLast()
            runnable.invoke()
        }
        diagnostics?.endEntry()

        if (!finishedLoadingAssets) {
            diagnostics?.beginEntry("assMan.update")
            val finished = assets.update(ASSET_MILLIS)
            diagnostics?.endEntry()

            if (finished) {
                finishedLoadingAssets = true

                diagnostics?.beginEntry("postCreate")
                postCreate()
                diagnostics?.endEntry()
            } else {
                diagnostics?.beginEntry("loadingText")

                val progress = (assets.progress * 100).toInt()
                loadingText.setText("${LOADING}: $progress%")

                viewports.get(ConstKeys.UI).apply()

                batch.projectionMatrix = getUiCamera().combined
                batch.begin()
                loadingText.draw(batch)
                batch.end()

                diagnostics?.endEntry()
            }
        } else {
            diagnostics?.beginEntry("autoPerf")

            val lowerPerformance = if (launch.autoPerf) autoPerformance.update(delta, settings.performance) else null
            if (lowerPerformance != null) {
                setPerformance(lowerPerformance)
                val message = "Performance issue detected! Downgrading to ${lowerPerformance.fps} FPS."
                GameLogger.error(TAG, message)
                showNotification(message, Color.RED)
            }

            diagnostics?.endEntry()

            diagnostics?.beginEntry("inputFrames")
            val leaderFrame = frameBuilders[0].build(keyboardInput.held() or gamepadInput.held())
            players.inputs[PlayerSlot(0)] = leaderFrame
            slot1GamepadInput?.let { players.inputs[PlayerSlot(1)] = frameBuilders[1].build(it.held()) }
            followBrain?.let { players.inputs[PlayerSlot(1)] = frameBuilders[1].build(it.held(leaderFrame, delta)) }
            systemFrameBuilder.build(keyboardInput.systemHeld())
            diagnostics?.endEntry()

            diagnostics?.beginEntry("screenController")
            screenController?.update(delta)
            diagnostics?.endEntry()

            val screen = currentScreen
            if (screen != null) {
                // the game engine — and so every instrumented system — runs inside screen.render(),
                // which is why the systems appear one level below this entry
                diagnostics?.beginEntry("screen.render")
                screen.render(delta)
                diagnostics?.endEntry()

                diagnostics?.beginEntry("screen.draw")
                screen.draw(batch)
                diagnostics?.endEntry()

                if (screen is IShapeDebuggable) {
                    diagnostics?.beginEntry("screen.drawShapes")
                    shapeRenderer.begin(ShapeType.Line)
                    screen.draw(shapeRenderer)
                    shapeRenderer.end()
                    diagnostics?.endEntry()
                }
            }

            if (!clock.paused && params.interpolationToggle?.invoke() == true) bodyInterpolation.let {
                it.interpolation = !it.interpolation
                GameLogger.log(TAG, "render(): interpolation = ${it.interpolation}")
            }

            if (params.screenshotTrigger?.invoke() == true) {
                diagnostics?.beginEntry("screenshot")

                val currentTime = LocalDateTime.now().toString()
                val filename = "screenshot_${currentTime}.png"

                val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.width, Gdx.graphics.height)
                PixmapIO.writePNG(Gdx.files.external(filename), pixmap, Deflater.DEFAULT_COMPRESSION, true)
                pixmap.dispose()

                diagnostics?.endEntry()
            }

            diagnostics?.beginEntry("eventsMan")
            events.run()
            diagnostics?.endEntry()

            diagnostics?.beginEntry("audioMan")
            audio.update(delta)
            diagnostics?.endEntry()

            diagnostics?.beginEntry("updatables")
            updatables.values().forEach { it.update(delta) }
            diagnostics?.endEntry()
        }

        if (params.debugText) {
            diagnostics?.beginEntry("debugText")

            viewports.get(ConstKeys.UI).apply()
            batch.projectionMatrix = getUiCamera().combined
            batch.begin()
            debugText.draw(batch)
            batch.end()

            diagnostics?.endEntry()
        }

        if (showNotification) {
            diagnostics?.beginEntry("notification")

            notificationFont.drawingColor = notificationColor

            viewports.get(ConstKeys.UI).apply()

            val uiCam = getUiCamera()

            shapeRenderer.projectionMatrix = uiCam.combined
            shapeRenderer.begin(ShapeType.Filled)
            shapeRenderer.setColor(0f, 0f, 0f, 1f)
            shapeRenderer.rect(
                0f,
                (ConstVals.VIEW_HEIGHT - 1.75f) * ConstVals.PPM,
                ConstVals.VIEW_WIDTH * ConstVals.PPM.toFloat(),
                ConstVals.PPM.toFloat()
            )
            shapeRenderer.end()

            batch.projectionMatrix = uiCam.combined
            batch.begin()
            notificationFont.draw(batch)
            batch.end()

            notificationTimer.update(delta)
            if (notificationTimer.isFinished()) showNotification = false

            diagnostics?.endEntry()
        }

        diagnostics?.beginEntry("reclaimPools")
        GameObjectPools.performNextReclaim()
        diagnostics?.endEntry()

        diagnostics?.endFrame()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.TURN_CONTROLLER_OFF -> {
                frameBuilders.forEach { it.locked = true }
                val turnSystemOff =
                    event.getOrDefaultProperty(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}", true, Boolean::class
                    )
                if (turnSystemOff) getSystem(ControllerSystem::class)?.on = false
            }

            EventType.TURN_CONTROLLER_ON -> {
                frameBuilders.forEach { it.locked = false }
                val turnSystemOn =
                    event.getOrDefaultProperty(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.ON}", true, Boolean::class
                    )
                if (turnSystemOn) getSystem(ControllerSystem::class)?.on = true
            }

            EventType.TOGGLE_PIXEL_PERFECT -> {
                val pixelPerfect = event.getProperty(ConstKeys.VALUE, Boolean::class)!!
                if (this.pixelPerfect == pixelPerfect) return
                this.pixelPerfect = pixelPerfect

                val newGameViewport: Viewport
                val newUiViewport: Viewport

                val gameCam = gameCamera
                val uiCam = getUiCamera()

                val gameWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
                val gameHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM

                val uiWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
                val uiHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM

                if (pixelPerfect) {
                    newGameViewport = PixelPerfectFitViewport(gameWidth, gameHeight, gameCam)
                    newUiViewport = PixelPerfectFitViewport(uiWidth, uiHeight, uiCam)
                } else {
                    newGameViewport = FitViewport(gameWidth, gameHeight, gameCam)
                    newUiViewport = FitViewport(uiWidth, uiHeight, uiCam)
                }

                newGameViewport.apply()
                newUiViewport.apply()

                viewports.put(ConstKeys.GAME, newGameViewport)
                viewports.put(ConstKeys.UI, newUiViewport)

                resize(Gdx.graphics.width, Gdx.graphics.height)
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        GameLogger.debug(TAG, "resize(): width=$width, height=$height")
        viewports.values().forEach { it.update(width, height) }
        currentScreen?.resize(width, height)
        screenController?.resize(width, height)
    }

    override fun pause() {
        GameLogger.log(TAG, "pause()")
        if (clock.paused) return
        clock.paused = true
        currentScreen?.pause()
    }

    override fun resume() {
        GameLogger.log(TAG, "resume()")
        if (!clock.paused) return
        if (heldLevelScreenKey != null) return
        clock.paused = false
        currentScreen?.resume()
    }

    override fun dispose() {
        GameLogger.log(TAG, "dispose()")

        val disposeActions = Array<() -> Unit>()

        disposeActions.add { if (this::batch.isInitialized) batch.dispose() }
        disposeActions.add { if (this::shapeRenderer.isInitialized) shapeRenderer.dispose() }
        disposeActions.add { if (this::engine.isInitialized) engine.dispose() }
        disposeActions.add {
            screens.values().forEach {
                try {
                    it.dispose()
                } catch (e: Exception) {
                    GameLogger.error(TAG, "dispose(): Failed to dispose screen: ${it::class.simpleName}", e)
                }
            }
        }
        disposeActions.add {
            disposables.values().forEach {
                try {
                    it.dispose()
                } catch (e: Exception) {
                    GameLogger.error(TAG, "dispose(): Failed to run dispose action", e)
                }
            }
        }
        disposeActions.add { debugWindow?.dispose() }
        disposeActions.add { logFileWriter?.dispose() }
        disposeActions.add { diagnostics?.dispose() }

        disposeActions.forEach {
            try {
                it.invoke()
            } catch (e: Exception) {
                GameLogger.error(TAG, "dispose(): Exception while running dispose action", e)
            }
        }
    }

    fun consumeLaunchRoom(): String? {
        val room = launchRoom
        val level = launchRoomLevel
        launchRoom = null
        launchRoomLevel = null
        if (room == null || level != levelState.currentLevel!!) return null
        return room
    }

    /**
     * Removes all the player's progress from memory: the game state, the weapons, the lives, and the
     * weapons attained (`weaponsAttained`). The save file does not change.
     */
    fun resetPlayerProgress() {
        GameLogger.debug(TAG, "resetPlayerProgress()")
        gameState.reset()
        gameState.clearWeapons()
        roster.forEach { it.weapons?.selectFirstWeapon() }
        gameState.resetLives()
        levelState.weaponsAttained = null
    }

    /**
     * Replaces the player's progress in memory with the items and the difficulty mode in [override]. The save file
     * does not change.
     */
    fun applyLevelOverride(override: LevelOverride) {
        GameLogger.debug(TAG, "applyLevelOverride(): override=$override")
        resetPlayerProgress()
        val loadout = override.loadout
        loadout.heartTanks.forEach { gameState.addHeartTank(it) }
        loadout.healthTanks.forEach { (tank, value) -> gameState.putHealthTank(tank, value) }
        loadout.enhancements.forEach { gameState.addEnhancement(it) }
        loadout.weapons.forEach { gameState.addWeaponToEachLoadout(it) }
        override.difficultyMode?.let { gameState.setDifficultyMode(it) }
        override.lives?.let { gameState.setLives(it) }
    }

    fun startExtraLevel(level: LevelDefinition) {
        GameLogger.debug(TAG, "startExtraLevel(): level=$level")
        // TODO: the level override is only applied for EXTRA_LEVEL levels. Apply it for other level types when
        //  needed. Note that [LevelOverride.canSpawn] is already used for every level type (see
        //  [MegaGameEntity.canSpawn]), so a level override on a non-extra level is only partly applied.
        val override = level.levelOverride
        if (override != null) applyLevelOverride(override) else resetPlayerProgress()
        startLevel(level)
    }

    fun saveState() {
        if (!launch.savesAllowed) {
            GameLogger.error(TAG, "saveState(): not saved, because --heartTanks, --healthTanks or --difficulty was given")
            return
        }

        val state = this.gameState.toString()
        GameLogger.debug(TAG, "saveState(): state=$state")

        val saveFile = Gdx.app.getPreferences(saveFileName)
        saveFile.putString(ConstKeys.STATE, state)
        saveFile.flush()
    }

    fun hasSavedState(): Boolean {
        val saveFile = Gdx.app.getPreferences(saveFileName)
        return saveFile.contains(ConstKeys.STATE)
    }

    fun loadSavedState(): Boolean {
        val saveFile = Gdx.app.getPreferences(saveFileName)

        if (saveFile.contains(ConstKeys.STATE)) {
            val state = saveFile.getString(ConstKeys.STATE)

            if (state == null) {
                GameLogger.error(TAG, "loadSavedState(): state is null")

                return false
            } else {
                GameLogger.debug(TAG, "loadSavedState(): loading state: state=$state")

                this.gameState.reset()
                this.gameState.fromString(state)
                this.gameState.resetLives()

                return true
            }
        }

        GameLogger.debug(TAG, "loadSavedState(): no state to load")

        return false
    }


    private fun queueAssets() {
        val queuedAssets = Array<IAsset>()
        queuedAssets.addAll(MusicAsset.valuesAsIAssetArray())
        queuedAssets.addAll(SoundAsset.valuesAsIAssetArray())
        queuedAssets.addAll(TextureAsset.valuesAsIAssetArray())

        assets.setLoader(TiledMap::class.java, TmxMapLoader(InternalFileHandleResolver()))
        LevelDefinition.entries.forEach { queuedAssets.add(it) }

        queuedAssets.forEach {
            GameLogger.debug(TAG, "loadAssets(): Loading ${it.assClass.simpleName} asset: ${it.source}")
            assets.load(it.source, it.assClass)
        }
    }

    private fun createGameEngine(): GameEngine {
        val drawables = ObjectMap<DrawingSection, InsertionOrderPriorityQueue<IComparableDrawable<Batch>>>()
        DrawingSection.entries.forEach { section -> drawables.put(section, InsertionOrderPriorityQueue()) }
        this.drawables = drawables

        val shapes = Array<IDrawableShape>()
        this.shapes = shapes

        fun addPathDebugLines(result: PathfinderResult) {
            val path = result.path ?: return
            val halfTile = ConstVals.PPM / 2f
            for (i in 0 until path.size - 1) {
                val node1 = path[i]
                val node2 = path[i + 1]
                shapes.add(
                    GameLine(
                        node1.x * ConstVals.PPM + halfTile,
                        node1.y * ConstVals.PPM + halfTile,
                        node2.x * ConstVals.PPM + halfTile,
                        node2.y * ConstVals.PPM + halfTile
                    )
                )
            }
        }

        val pathDebugConsumer = if (params.debugPathfinding) ::addPathDebugLines else null

        val worldWrap = WorldWrap(
            region = levelState::wrapRegion,
            wraps = WrapPolicy::wraps,
            maxWraps = WrapPolicy::maxWraps
        )
        val worldSystem = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = settings.performance.fixedStep,
            worldContainerSupplier = { levelState.worldContainer },
            contactListener = ContactRuleDispatcher(
                megaContactRules(players, gravity, launch.rules, CONTACT_LISTENER_DEBUG_FILTER),
                CONTACT_LISTENER_DEBUG_FILTER
            ),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter(),
            fixedStepScalar = params.fixedStepScalar,
            maxIterations = 2 * MathUtils.ceil(
                params.fixedStepScalar / (settings.performance.fixedStep * settings.performance.fps)
            ),
            diagnostics = diagnostics,
            wrap = worldWrap
        )
        bodyInterpolation = BodyInterpolation(world = worldSystem, interpolation = params.interpolation)

        val engine = GameEngine(
            systems = gdxArrayOf(
                ControllerSystem(),
                BehaviorsSystem(diagnostics),
                worldSystem,
                CullablesSystem(object : GameEntityCuller {
                    override fun cull(entity: IGameEntity) {
                        entity.destroy()
                    }
                }, diagnostics = diagnostics),
                MotionSystem(diagnostics),
                when (params.pathfinding) {
                    "async" -> AsyncPathfindingSystem(
                        MegaPathfinderFactory(levelState, worldSystem),
                        diagnostics = diagnostics,
                        pathDebugConsumer = pathDebugConsumer
                    )

                    "sync" -> SimplePathfindingSystem(
                        MegaPathfinderFactory(levelState, worldSystem),
                        diagnostics = diagnostics,
                        pathDebugConsumer = pathDebugConsumer
                    )

                    else -> throw IllegalArgumentException("Invalid pathfinding value: ${params.pathfinding}")
                },
                PointsSystem(diagnostics),
                UpdatablesSystem(diagnostics),
                FontsSystem(this::addDrawable),
                AnimationsSystem(diagnostics),
                SpritesSystem(this::addDrawable, diagnostics, bodyInterpolation, worldWrap),
                DrawableShapesSystem(
                    shapesCollector = { shapes.add(it) },
                    debug = params.debugShapes,
                    diagnostics = diagnostics
                ),
                AudioSystem(
                    { audio.playSound(it.source, it.loop) },
                    { audio.playMusic(it.source, it.loop) },
                    { audio.stopSound(it) },
                    { audio.stopMusic(it) },
                    diagnostics = diagnostics
                ),
                GroupsSystem()
            ),
            onSpawnRejected = {
                // return a rejected pooled entity to its pool; never free an entity that is spawned or queued to
                // spawn, and do nothing for an entity that is not pooled (it has no pool runnable)
                val entity = it as MegaGameEntity
                if (!entity.engine.hasClaimOn(entity)) entity.runnablesOnDestroy[ConstKeys.POOL]?.invoke()
            }
        )

        val systems = ObjectMap<String, GameSystem>()
        engine.systems.forEach { systems.put(it::class.simpleName, it) }
        this.systems = systems

        return engine
    }

    fun setCurrentScreen(requestedKey: String, props: Properties = Properties()) {
        // TODO: screen redirects are a stopgap until the host contract and a level registry give each title its own flow (item 185, 001b)
        val key = params.title.redirectScreen(requestedKey) ?: requestedKey
        if (key != requestedKey) GameLogger.debug(TAG, "setCurrentScreen(): redirect $requestedKey to $key")
        GameLogger.log(TAG, "setCurrentScreen(): set to screen with key = $key")
        currentScreen?.let {
            it.hide()
            it.reset()
        }
        currentScreenKey = key
        currentScreen?.let {
            it.show(props)
            it.resize(Gdx.graphics.width, Gdx.graphics.height)
        }
        if (clock.paused) resume()
    }

    // TODO: the pause grid shows for 1 frame when the bindings screen opens from pause, because this call runs in the
    //  frame of the pause screen (see item 013)
    fun showOverPause(key: String, props: Properties) {
        GameLogger.log(TAG, "showOverPause(): show screen with key = $key")
        heldLevelScreenKey = currentScreenKey
        heldUiCameraPosition.set(getUiCamera().position)
        currentScreenKey = key
        currentScreen?.let {
            it.show(props)
            it.resize(Gdx.graphics.width, Gdx.graphics.height)
        }
    }

    fun closeOverPause() {
        GameLogger.log(TAG, "closeOverPause(): back to screen with key = $heldLevelScreenKey")
        currentScreen?.let {
            it.hide()
            it.reset()
        }
        currentScreenKey = heldLevelScreenKey
        heldLevelScreenKey = null
        getUiCamera().position.set(heldUiCameraPosition)
        currentScreen?.resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    fun startLevel(level: LevelDefinition? = null) {
        if (level != null) levelState.currentLevel = level

        val levelScreen = screens.get(ScreenEnum.LEVEL_SCREEN.name) as MegaLevelScreen

        val currentLevel = levelState.currentLevel!!
        roster.putInSlot0(currentLevel.characters, launch.slots)
        levelScreen.music = currentLevel.music
        levelScreen.screenOnCompletion = currentLevel.screenOnCompletion
        levelScreen.start(currentLevel.source)
        levelState.currentLevel = currentLevel

        setCurrentScreen(ScreenEnum.LEVEL_SCREEN.name)
    }

    fun getUiCamera() = viewports.get(ConstKeys.UI).camera as OrthographicCamera

    fun addDrawable(drawable: IComparableDrawable<Batch>) {
        val section = drawable.priority.section
        val queue = drawables.get(section)
        queue.add(drawable)
    }

    fun <T : GameSystem> getSystem(clazz: KClass<T>): T? {
        if (!systems.containsKey(clazz.simpleName)) return null
        return clazz.cast(systems[clazz.simpleName]!!)
    }

    fun showNotification(text: String, color: Color = Color.WHITE) {
        showNotification = true
        notificationText = text
        notificationTimer.reset()
        notificationColor = color
    }

    fun setDebugText(text: String) = setDebugTextSupplier { text }

    fun setDebugTextSupplier(supplier: () -> String) = debugText.setTextSupplier(supplier)

    fun isPixelPerfect() = pixelPerfect

    fun isVsync() = vsync

    fun setVsync(value: Boolean) {
        if (vsync == value) return
        vsync = value
        Gdx.graphics.setVSync(value)
    }

    fun setPerformance(performance: Performance) {
        if (settings.performance == performance) return
        settings.performance = performance
        Gdx.graphics.setForegroundFPS(performance.fps)
        getSystem(WorldSystem::class)?.fixedStep = performance.fixedStep
        events.submitEvent(Event(EventType.CHANGE_PERFORMANCE_MODE, props(ConstKeys.MODE pairTo performance)))
        autoPerformance.restartGracePeriod()
    }

    fun isPerformanceSubpar() = settings.performance.ordinal > Performance.LOW.ordinal

    fun resetAutoPerfGracePeriod() = autoPerformance.restartGracePeriod()
}
