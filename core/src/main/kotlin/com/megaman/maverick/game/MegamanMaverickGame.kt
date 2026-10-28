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
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsSystem
import com.maverick.game.engine.audio.AudioSystem
import com.maverick.game.engine.behaviors.BehaviorsSystem
import com.maverick.game.engine.common.GameLogLevel
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.LogReceiver
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.InsertionOrderPriorityQueue
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.controller.ControllerSystem
import com.megaman.maverick.game.controllers.ControllerUtils
import com.maverick.game.engine.controller.buttons.ControllerButtons
import com.maverick.game.engine.controller.polling.IControllerPoller
import com.maverick.game.engine.cullables.CullablesSystem
import com.maverick.game.engine.cullables.GameEntityCuller
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
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
import com.maverick.game.engine.motion.MotionSystem
import com.maverick.game.engine.pathfinding.AsyncPathfindingSystem
import com.maverick.game.engine.pathfinding.PathfinderResult
import com.maverick.game.engine.pathfinding.SimplePathfindingSystem
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.points.PointsSystem
import com.maverick.game.engine.screens.IScreen
import com.maverick.game.engine.screens.levels.tiledmap.TiledMapLoadResult
import com.maverick.game.engine.screens.viewports.PixelPerfectFitViewport
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.maverick.game.engine.world.container.IWorldContainer
import com.megaman.maverick.game.MegamanMaverickGame.Performance
import com.megaman.maverick.game.MegamanMaverickGame.RunType
import com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.ScreenController
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.controllers.loadControllerButtons
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.UnderWaterBubble
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelOverride
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.pathfinding.MegaPathfinderFactory
import com.maverick.game.engine.input.InputFrameBuilder
import com.maverick.game.engine.input.checkInputButtons
import com.megaman.maverick.game.input.FrameControllerPoller
import com.megaman.maverick.game.input.GamepadInput
import com.megaman.maverick.game.input.KeyboardInput
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.debug.DebugWindow
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.screens.menus.*
import com.megaman.maverick.game.screens.menus.level.LevelSelectScreenV2
import com.megaman.maverick.game.screens.other.*
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.utils.AsyncFileWriter
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getMusics
import com.megaman.maverick.game.utils.extensions.getSounds
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
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
    var debugText: Boolean = false
    var fixedStepScalar: Float = 1f
    var pixelPerfect: Boolean = false
    var musicVolume: Float = 0.5f
    var soundVolume: Float = 0.5f
    var screenshotTrigger: (() -> Boolean)? = null
    var showScreenController: Boolean = false
    var vsync: Boolean = false
    var diagnostics: Boolean = false
    var logLevels: OrderedSet<GameLogLevel> = OrderedSet()
    var worldContainer: String = "simple"
    var pathfinding: String = "async"
    var launchOptions: LaunchOptions = LaunchOptions()
}

class MegamanMaverickGame(
    val params: MegamanMaverickGameParams = MegamanMaverickGameParams()
) : Game(), IEventListener {

    enum class Performance(val fps: Int, val fixedStep: Float) {
        VERY_LOW(30, 1f / 75f),
        LOW(45, 1f / 112.5f),
        MEDIUM(60, 1f / 150f),
        HIGH(90, 1f / 225f)
    }

    enum class RunType { GAME, TEST }

    companion object {
        const val TAG = "MegamanMaverickGame"
        const val VERSION = "" // ""ALPHA 1.15.0"
        private const val ASSET_MILLIS = 17
        private const val LOADING = "LOADING"
        private const val LOG_FILE_NAME = "logs.txt"
        private const val AUTO_PERF_SUSTAINED_DUR = 3f
        private const val AUTO_PERF_GRACE_DUR = 5f         // suppression window after any perf change
        private const val AUTO_PERF_NEAR_DEFICIT = 5       // fps below target → ~10s tolerance
        private const val AUTO_PERF_MODERATE_DEFICIT = 10  // fps below target → ~6s tolerance
        private const val AUTO_PERF_NEAR_SPEED = 0.3f      // timer multiplier: 3s / ~10s
        private const val AUTO_PERF_MODERATE_SPEED = 0.5f  // timer multiplier: 3s / ~6s
        private const val AUTO_PERF_HIGH_SPEED = 1.0f      // timer multiplier: 3s / ~3s
        private const val AUTO_PERF_CRISIS_SPEED = 2.0f    // timer multiplier: 3s / ~1.5s
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

    private var cameraRotating = false
    private var focusSnappedAway = false

    private lateinit var drawables: ObjectMap<DrawingSection, java.util.Queue<IComparableDrawable<Batch>>>
    private lateinit var shapes: Array<IDrawableShape>
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

    lateinit var buttons: ControllerButtons
    lateinit var controllerPoller: IControllerPoller
    lateinit var gamepadInput: GamepadInput
    private lateinit var keyboardInput: KeyboardInput
    private val slot0FrameBuilder = InputFrameBuilder(MegaControllerButton.entries)
    lateinit var selectButtonAction: SelectButtonAction

    lateinit var assMan: AssetManager
    lateinit var eventsMan: EventsManager
    lateinit var audioMan: MegaAudioManager

    lateinit var engine: GameEngine
    lateinit var state: GameState
    var weaponsAttained: OrderedSet<MegamanWeapon>? = null

    lateinit var players: MegaPlayers
    lateinit var gravity: WorldGravity

    var paused = false
        private set

    private var currentScreenKey: String? = null
    private var screenController: ScreenController? = null

    private lateinit var debugText: MegaFontHandle
    private lateinit var loadingText: MegaFontHandle
    private var finishedLoadingAssets = false

    private lateinit var notificationFont: BitmapFontHandle
    private var notificationTimer = Timer(NOTIFICATION_DUR)
    private var notificationText = ""
    private var showNotification = false
    private var notificationColor = Color.WHITE

    private val autoPerfTimer = Timer(AUTO_PERF_SUSTAINED_DUR)
    private val autoPerfGraceTimer = Timer(AUTO_PERF_GRACE_DUR)

    private var logFileWriter: AsyncFileWriter? = null
    private var debugWindow: DebugWindow? = null

    private var pixelPerfect = false
    private var vsync = false

    var diagnostics: RuntimeDiagnostics? = null
        private set

    private lateinit var launch: ResolvedLaunch
    private var launchRoom: String? = null
    private var launchRoomLevel: LevelDefinition? = null

    override fun create() {
        val launchResult = resolveLaunchOptions(params.launchOptions)
        if (!launchResult.isValid)
            throw IllegalArgumentException("Invalid launch options: ${launchResult.errors.joinToString("; ")}")
        launch = launchResult.resolved!!
        launchRoom = launch.room
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
        buttons = ControllerUtils.loadControllerButtons()
        keyboardInput = KeyboardInput(buttons)
        gamepadInput = GamepadInput(buttons)
        controllerPoller = FrameControllerPoller { players.inputs[PlayerSlot(0)] }
        selectButtonAction = SelectButtonAction.NEXT_WEAPON

        eventsMan = EventsManager()
        eventsMan.addListener(this)

        if (params.diagnostics)
            diagnostics = RuntimeDiagnostics("${System.currentTimeMillis()}-diagnostics.txt")

        engine = createGameEngine()
        state = GameState()

        this.pixelPerfect = params.pixelPerfect
        this.vsync = params.vsync

        val gameWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val gameHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val gameCamera = RotatableCamera(onJustFinishedRotating = {
            setCameraRotating(false)
            setFocusSnappedAway(false)
            eventsMan.submitEvent(Event(EventType.END_GAME_CAM_ROTATION))
        })
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

        assMan = AssetManager()
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
        audioMan = MegaAudioManager(assMan.getSounds(sounds), assMan.getMusics(music))
        audioMan.musicVolume = params.musicVolume
        audioMan.soundVolume = params.soundVolume

        MegaEntityFactory.init(this)

        // TODO: should replace EntityFactories with MegaEntityFactory
        EntityFactories.initialize(this)

        val megaman = Megaman(this)
        // manually call init and set initialized to true before megaman is spawned
        megaman.init()
        megaman.initialized = true
        // add megaman as a game state listener
        state.addListener(megaman)

        players = MegaPlayers()
        // TODO: a player that registers after progress exists keeps the base max health, because the heart tank
        //  max health is set only when a heart tank changes; a late join must set it from the game state (item 047)
        players.register(PlayerSlot(0), megaman)
        gravity = WorldGravity(players)

        val levelScreen = MegaLevelScreen(this)
        levelScreen.init()
        screens.put(ScreenEnum.LEVEL_SCREEN.name, levelScreen)

        screens.put(ScreenEnum.LOGO_SCREEN.name, LogoScreen(this))
        screens.put(ScreenEnum.OPENING_SCREEN.name, OpeningScreen(this))
        screens.put(ScreenEnum.MAIN_MENU_SCREEN.name, MainMenuScreen(this))
        screens.put(ScreenEnum.SAVE_GAME_SCREEN.name, SaveGameScreen(this))
        screens.put(ScreenEnum.GAME_OVER_SCREEN.name, GameOverScreen(this))
        screens.put(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, true))
        screens.put(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, false))
        screens.put(ScreenEnum.SELECT_DIFFICULTY_SCREEN.name, SelectDifficultyScreen(this))
        screens.put(ScreenEnum.LEVEL_SELECT_SCREEN.name, LevelSelectScreenV2(this))
        // screens.put(ScreenEnum.ROBOT_MASTER_INTRO_SCREEN.name, RobotMasterIntroScreenV2(this))
        screens.put(ScreenEnum.ROBOT_MASTER_INTRO_SCREEN.name, RobotMasterIntroScreen(this))
        screens.put(ScreenEnum.GET_WEAPONS_SCREEN.name, GetWeaponScreen(this))
        screens.put(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name, SimpleInitGameScreen(this))
        screens.put(ScreenEnum.ENDING_SCREEN.name, EndingScreen(this))
        screens.put(ScreenEnum.CREDITS_SCREEN.name, CreditsScreen(this))
        screens.put(ScreenEnum.EXTRAS_SCREEN.name, ExtrasScreen(this))

        val start = launch.start
        if (start is LaunchStart.StartLevel) start.level.levelOverride?.let { applyLevelOverride(it) }
        launch.weapons.forEach { state.addWeapon(it) }

        when (start) {
            LaunchStart.Default -> when (params.runType) {
                RunType.GAME -> setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name)
                RunType.TEST -> startLevel(LevelDefinition.TEST_1)
            }

            is LaunchStart.StartLevel -> startLevel(start.level)

            is LaunchStart.OpenScreen -> {
                if (!screens.containsKey(start.screen.name))
                    throw IllegalStateException("Launch option --screen: screen ${start.screen.name} is not registered")
                start.stagedLevel?.let { setCurrentLevel(it) }
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
            val finished = assMan.update(ASSET_MILLIS)
            diagnostics?.endEntry()

            if (finished) {
                finishedLoadingAssets = true

                diagnostics?.beginEntry("postCreate")
                postCreate()
                diagnostics?.endEntry()
            } else {
                diagnostics?.beginEntry("loadingText")

                val progress = (assMan.progress * 100).toInt()
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

            if (launch.autoPerf) autoPerfGraceTimer.update(delta)
            if (launch.autoPerf && autoPerfGraceTimer.isFinished()) {
                val performance = getPerformance()
                val currentFps = Gdx.graphics.framesPerSecond
                if (performance != Performance.VERY_LOW && currentFps < performance.fps) {
                    val deficit = performance.fps - currentFps
                    val nextLowerFps = Performance.entries[performance.ordinal - 1].fps
                    val timerMultiplier = when {
                        deficit <= AUTO_PERF_NEAR_DEFICIT -> AUTO_PERF_NEAR_SPEED
                        deficit <= AUTO_PERF_MODERATE_DEFICIT -> AUTO_PERF_MODERATE_SPEED
                        currentFps > nextLowerFps -> AUTO_PERF_HIGH_SPEED
                        else -> AUTO_PERF_CRISIS_SPEED
                    }
                    autoPerfTimer.update(delta * timerMultiplier)

                    if (autoPerfTimer.isFinished()) {
                        val newPerformance = Performance.entries[performance.ordinal - 1]
                        setPerformance(newPerformance)

                        autoPerfTimer.reset()

                        val message = "Performance issue detected! Downgrading to ${newPerformance.fps} FPS."
                        GameLogger.error(TAG, message)
                        showNotification(message, Color.RED)
                    }
                } else autoPerfTimer.reset()
            } else autoPerfTimer.reset()

            diagnostics?.endEntry()

            diagnostics?.beginEntry("controllerPoller")
            players.inputs[PlayerSlot(0)] = slot0FrameBuilder.build(keyboardInput.held() or gamepadInput.held())
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
            eventsMan.run()
            diagnostics?.endEntry()

            diagnostics?.beginEntry("audioMan")
            audioMan.update(delta)
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
                slot0FrameBuilder.locked = true
                val turnSystemOff =
                    event.getOrDefaultProperty(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}", true, Boolean::class
                    )
                if (turnSystemOff) getSystem(ControllerSystem::class)?.on = false
            }

            EventType.TURN_CONTROLLER_ON -> {
                slot0FrameBuilder.locked = false
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

                val gameCam = getGameCamera()
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
        if (paused) return
        paused = true
        currentScreen?.pause()
    }

    override fun resume() {
        GameLogger.log(TAG, "resume()")
        if (!paused) return
        paused = false
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
        if (room == null || level != getCurrentLevel()) return null
        return room
    }

    fun setCurrentLevel(levelDef: LevelDefinition) {
        levelState.currentLevel = levelDef
    }

    fun getCurrentLevel() = levelState.currentLevel!!

    fun isInExtraLevel() = levelState.currentLevel?.type == LevelType.EXTRA_LEVEL

    /**
     * Removes all the player's progress from memory: the game state, the weapons, the lives, and the
     * weapons attained (`weaponsAttained`). The save file does not change.
     */
    fun resetPlayerProgress() {
        GameLogger.debug(TAG, "resetPlayerProgress()")
        state.reset()
        state.clearWeapons()
        players.forEach { (it as Megaman).currentWeapon = MegamanWeapon.MEGA_BUSTER }
        state.resetLives()
        weaponsAttained = null
    }

    /**
     * Replaces the player's progress in memory with the items and the difficulty mode in [override]. The save file
     * does not change.
     */
    fun applyLevelOverride(override: LevelOverride) {
        GameLogger.debug(TAG, "applyLevelOverride(): override=$override")
        resetPlayerProgress()
        val loadout = override.loadout
        loadout.heartTanks.forEach { state.addHeartTank(it) }
        loadout.healthTanks.forEach { (tank, value) -> state.putHealthTank(tank, value) }
        loadout.enhancements.forEach { state.addEnhancement(it) }
        loadout.weapons.forEach { state.addWeapon(it) }
        override.difficultyMode?.let { state.setDifficultyMode(it) }
        override.lives?.let { state.setLives(it) }
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
        val state = this.state.toString()
        GameLogger.debug(TAG, "saveState(): state=$state")

        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)
        saveFile.putString(ConstKeys.STATE, state)
        saveFile.flush()
    }

    fun hasSavedState(): Boolean {
        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)
        return saveFile.contains(ConstKeys.STATE)
    }

    fun loadSavedState(): Boolean {
        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)

        if (saveFile.contains(ConstKeys.STATE)) {
            val state = saveFile.getString(ConstKeys.STATE)

            if (state == null) {
                GameLogger.error(TAG, "loadSavedState(): state is null")

                return false
            } else {
                GameLogger.debug(TAG, "loadSavedState(): loading state: state=$state")

                this.state.reset()
                this.state.fromString(state)
                this.state.resetLives()

                return true
            }
        }

        GameLogger.debug(TAG, "loadSavedState(): no state to load")

        return false
    }


    private fun queueAssets() {
        val assets = Array<IAsset>()
        assets.addAll(MusicAsset.valuesAsIAssetArray())
        assets.addAll(SoundAsset.valuesAsIAssetArray())
        assets.addAll(TextureAsset.valuesAsIAssetArray())

        assMan.setLoader(TiledMap::class.java, TmxMapLoader(InternalFileHandleResolver()))
        LevelDefinition.entries.forEach { assets.add(it) }

        assets.forEach {
            GameLogger.debug(TAG, "loadAssets(): Loading ${it.assClass.simpleName} asset: ${it.source}")
            assMan.load(it.source, it.assClass)
        }
    }

    private fun createGameEngine(): GameEngine {
        val drawables = ObjectMap<DrawingSection, java.util.Queue<IComparableDrawable<Batch>>>()
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

        val engine = GameEngine(
            systems = gdxArrayOf(
                ControllerSystem(),
                BehaviorsSystem(diagnostics),
                WorldSystem(
                    ppm = ConstVals.PPM,
                    fixedStep = getPerformance().fixedStep,
                    worldContainerSupplier = { getWorldContainer() },
                    contactListener = ContactRuleDispatcher(
                        megaContactRules(this, CONTACT_LISTENER_DEBUG_FILTER),
                        CONTACT_LISTENER_DEBUG_FILTER
                    ),
                    collisionHandler = MegaCollisionHandler(),
                    contactFilter = MegaContactFilter(),
                    fixedStepScalar = params.fixedStepScalar,
                    maxIterations = 2 * MathUtils.ceil(
                        params.fixedStepScalar / (getPerformance().fixedStep * getPerformance().fps)
                    ),
                    diagnostics = diagnostics,
                ),
                CullablesSystem(object : GameEntityCuller {
                    override fun cull(entity: IGameEntity) {
                        entity.destroy()
                    }
                }, diagnostics = diagnostics),
                MotionSystem(diagnostics),
                when (params.pathfinding) {
                    "async" -> AsyncPathfindingSystem(
                        MegaPathfinderFactory(this),
                        diagnostics = diagnostics,
                        pathDebugConsumer = pathDebugConsumer
                    )

                    "sync" -> SimplePathfindingSystem(
                        MegaPathfinderFactory(this),
                        diagnostics = diagnostics,
                        pathDebugConsumer = pathDebugConsumer
                    )

                    else -> throw IllegalArgumentException("Invalid pathfinding value: ${params.pathfinding}")
                },
                PointsSystem(diagnostics),
                UpdatablesSystem(diagnostics),
                FontsSystem(this::addDrawable),
                AnimationsSystem(diagnostics),
                SpritesSystem(this::addDrawable, diagnostics),
                DrawableShapesSystem(
                    shapesCollector = { shapes.add(it) },
                    debug = params.debugShapes,
                    diagnostics = diagnostics
                ),
                AudioSystem(
                    { audioMan.playSound(it.source, it.loop) },
                    { audioMan.playMusic(it.source, it.loop) },
                    { audioMan.stopSound(it) },
                    { audioMan.stopMusic(it) },
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

    fun setCurrentScreen(key: String, props: Properties = Properties()) {
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
        if (paused) resume()
    }

    fun startLevel(level: LevelDefinition? = null) {
        if (level != null) setCurrentLevel(level)

        val levelScreen = screens.get(ScreenEnum.LEVEL_SCREEN.name) as MegaLevelScreen

        val currentLevel = getCurrentLevel()
        levelScreen.music = currentLevel.music
        levelScreen.screenOnCompletion = currentLevel.screenOnCompletion
        levelScreen.start(currentLevel.source)
        setCurrentLevel(currentLevel)

        setCurrentScreen(ScreenEnum.LEVEL_SCREEN.name)
    }

    fun getGameCamera() = viewports.get(ConstKeys.GAME).camera as RotatableCamera

    fun setCameraRotating(value: Boolean) {
        cameraRotating = value
    }

    fun isCameraRotating() = cameraRotating

    fun getUiCamera() = viewports.get(ConstKeys.UI).camera as OrthographicCamera

    fun getDrawables() = drawables

    fun addDrawable(drawable: IComparableDrawable<Batch>) {
        val section = drawable.priority.section
        val queue = getDrawables().get(section)
        queue.add(drawable)
    }

    fun getShapes() = shapes

    fun getSystems(): ObjectMap<String, GameSystem> = systems

    fun <T : GameSystem> getSystem(clazz: KClass<T>): T? {
        val systems = getSystems()
        if (!systems.containsKey(clazz.simpleName)) return null
        return clazz.cast(systems[clazz.simpleName]!!)
    }

    fun setRoomsSupplier(supplier: () -> Array<RectangleMapObject>?) {
        levelState.roomsSupplier = supplier
    }

    fun getRooms(out: Array<RectangleMapObject>): Array<RectangleMapObject> {
        val rooms = levelState.roomsSupplier?.invoke()
        if (rooms != null) out.addAll(rooms)
        return out
    }

    fun setCurrentRoomSupplier(supplier: () -> RectangleMapObject?) {
        levelState.currentRoomSupplier = supplier
    }

    fun getCurrentRoom(): RectangleMapObject? = levelState.currentRoomSupplier?.invoke()

    fun setWorldContainer(worldContainer: IWorldContainer) {
        levelState.worldContainer = worldContainer
    }

    fun getWorldContainer(): IWorldContainer? = levelState.worldContainer

    fun setTiledMapLoadResult(tiledMapLoadResult: TiledMapLoadResult) {
        levelState.tiledMapLoadResult = tiledMapLoadResult
    }

    fun getTiledMapLoadResult() = levelState.tiledMapLoadResult!!

    fun showNotification(text: String, color: Color = Color.WHITE) {
        showNotification = true
        notificationText = text
        notificationTimer.reset()
        notificationColor = color
    }

    fun setDebugText(text: String) = setDebugTextSupplier { text }

    fun setDebugTextSupplier(supplier: () -> String) = debugText.setTextSupplier(supplier)

    // This is set to true when the camera focus on Megaman has (potentially) been snapped away suddenly, and so
    // the camera should be interpolated to Megaman's current position to prevent a jarring camera experience. This
    // is reset back to false once the camera reaches Megaman's true position again.
    fun setFocusSnappedAway(value: Boolean) {
        focusSnappedAway = value
    }

    fun isFocusSnappedAway() = focusSnappedAway

    fun isPixelPerfect() = pixelPerfect

    fun isVsync() = vsync

    fun setVsync(value: Boolean) {
        if (vsync == value) return
        vsync = value
        Gdx.graphics.setVSync(value)
    }

    fun getPerformance() = params.performance

    fun setPerformance(performance: Performance) {
        if (params.performance == performance) return
        params.performance = performance
        Gdx.graphics.setForegroundFPS(performance.fps)
        getSystem(WorldSystem::class)?.fixedStep = performance.fixedStep
        eventsMan.submitEvent(Event(EventType.CHANGE_PERFORMANCE_MODE, props(ConstKeys.MODE pairTo performance)))
        autoPerfGraceTimer.reset()
        autoPerfTimer.reset()
    }

    fun isPerformanceSubpar() = getPerformance().ordinal > Performance.LOW.ordinal

    fun resetAutoPerfGracePeriod() = autoPerfGraceTimer.reset()
}
