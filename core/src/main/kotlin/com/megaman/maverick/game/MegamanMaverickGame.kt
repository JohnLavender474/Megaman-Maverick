package com.megaman.maverick.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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
import com.mega.game.engine.GameEngine
import com.mega.game.engine.animations.AnimationsSystem
import com.mega.game.engine.audio.AudioSystem
import com.mega.game.engine.behaviors.BehaviorsSystem
import com.mega.game.engine.common.GameLogLevel
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.LogReceiver
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.InsertionOrderPriorityQueue
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.ControllerSystem
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.ControllerButtons
import com.mega.game.engine.controller.polling.IControllerPoller
import com.mega.game.engine.cullables.CullablesSystem
import com.mega.game.engine.cullables.GameEntityCuller
import com.mega.game.engine.diagnostics.RuntimeDiagnostics
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.drawables.fonts.FontsSystem
import com.mega.game.engine.drawables.shapes.DrawableShapesSystem
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable
import com.mega.game.engine.drawables.sprites.SpritesSystem
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.EventsManager
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionSystem
import com.mega.game.engine.pathfinding.AsyncPathfindingSystem
import com.mega.game.engine.pathfinding.SimplePathfindingSystem
import com.mega.game.engine.points.PointsSystem
import com.mega.game.engine.screens.IScreen
import com.mega.game.engine.screens.levels.tiledmap.TiledMapLoadResult
import com.mega.game.engine.screens.viewports.PixelPerfectFitViewport
import com.mega.game.engine.systems.GameSystem
import com.mega.game.engine.updatables.UpdatablesSystem
import com.mega.game.engine.world.WorldSystem
import com.mega.game.engine.world.contacts.Contact
import com.mega.game.engine.world.container.IWorldContainer
import com.megaman.maverick.game.MegamanMaverickGame.Performance
import com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerPoller
import com.megaman.maverick.game.controllers.ScreenController
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.controllers.loadControllerButtons
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.pathfinding.MegaPathfinderFactory
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.debug.DebugWindow
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.screens.menus.ControllerSettingsScreen
import com.megaman.maverick.game.screens.menus.MainMenuScreen
import com.megaman.maverick.game.screens.menus.SaveGameScreen
import com.megaman.maverick.game.screens.menus.SelectDifficultyScreen
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
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.MegaContactListener
import java.time.LocalDateTime
import java.util.zip.Deflater
import kotlin.reflect.KClass
import kotlin.reflect.cast

class MegamanMaverickGameParams {
    lateinit var performance: Performance
    var writeLogsToFile: Boolean = false
    var debugWindow: Boolean = false
    var debugShapes: Boolean = false
    var debugText: Boolean = false
    var fixedStepScalar: Float = 1f
    var pixelPerfect: Boolean = false
    var musicVolume: Float = 0.5f
    var soundVolume: Float = 0.5f
    var allowScreenshots: Boolean = false
    var showScreenController: Boolean = false
    var vsync: Boolean = false
    var diagnostics: Boolean = false
    var logLevels: OrderedSet<GameLogLevel> = OrderedSet()
    var worldContainer: String = "quadtree"
    var pathfinding: String = "async"
}

class MegamanMaverickGame(
    val params: MegamanMaverickGameParams = MegamanMaverickGameParams()
) : Game(), IEventListener, IPropertizable {

    enum class Performance(val fps: Int, val fixedStep: Float) {
        VERY_LOW(30, 1f / 100f),
        LOW(45, 1f / 150f),
        MEDIUM(60, 1f / 200f),
        HIGH(90, 1f / 300f),
        VERY_HIGH(120, 1f / 400f)
    }

    companion object {
        const val TAG = "MegamanMaverickGame"
        const val VERSION = "ALPHA 1.12.1"
        private const val ASSET_MILLIS = 17
        private const val LOADING = "LOADING"
        private const val LOG_FILE_NAME = "logs.txt"
        private const val SCREENSHOT_KEY = Input.Keys.P
        private const val AUTO_PERF_FPS_THRESHOLD_SCALAR = 0.9f
        private const val AUTO_PERF_FPS_CRISIS_SCALAR = 0.5f
        private const val AUTO_PERF_SUSTAINED_DUR = 3f
        private const val NOTIFICATION_DUR = 5f
        val TAGS_TO_LOG: ObjectSet<String> = objectSetOf()
        val CONTACT_LISTENER_DEBUG_FILTER: (Contact) -> Boolean = { contact ->
            contact.fixturesMatch(FixtureType.FEET, FixtureType.DEATH)
        }
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.TURN_CONTROLLER_ON,
        EventType.TURN_CONTROLLER_OFF,
        EventType.TOGGLE_PIXEL_PERFECT
    )
    override val properties = Properties()

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
    lateinit var selectButtonAction: SelectButtonAction

    lateinit var assMan: AssetManager
    lateinit var eventsMan: EventsManager
    lateinit var audioMan: MegaAudioManager

    lateinit var engine: GameEngine
    lateinit var state: GameState

    lateinit var megaman: Megaman

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

    private var autoPerfTimer = Timer(AUTO_PERF_SUSTAINED_DUR)

    private var logFileWriter: AsyncFileWriter? = null
    private var debugWindow: DebugWindow? = null

    private var pixelPerfect = false
    private var vsync = false

    var diagnostics: RuntimeDiagnostics? = null
        private set

    override fun create() {
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
        GameLogger.filterByTag = true

        GameLogger.log(TAG, "create(): appType=${Gdx.app.type}")

        shapeRenderer = ShapeRenderer()
        shapeRenderer.setAutoShapeType(true)
        batch = SpriteBatch()

        buttons = ControllerUtils.loadControllerButtons()
        controllerPoller = defineControllerPoller(buttons)
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

        megaman = Megaman(this)
        // manually call init and set initialized to true before megaman is spawned
        megaman.init()
        megaman.initialized = true
        // add megaman as a game state listener
        state.addListener(megaman)

        val levelScreen = MegaLevelScreen(this)
        levelScreen.init()
        screens.put(ScreenEnum.LEVEL_SCREEN.name, levelScreen)

        screens.put(ScreenEnum.LOGO_SCREEN.name, LogoScreen(this))
        screens.put(ScreenEnum.MAIN_MENU_SCREEN.name, MainMenuScreen(this))
        screens.put(ScreenEnum.SAVE_GAME_SCREEN.name, SaveGameScreen(this))
        screens.put(ScreenEnum.GAME_OVER_SCREEN.name, GameOverScreen(this))
        screens.put(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, true))
        screens.put(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, false))
        screens.put(ScreenEnum.SELECT_DIFFICULTY_SCREEN.name, SelectDifficultyScreen(this))
        screens.put(ScreenEnum.LEVEL_SELECT_SCREEN.name, LevelSelectScreenV2(this))
        screens.put(ScreenEnum.ROBOT_MASTER_INTRO_SCREEN.name, RobotMasterIntroScreen(this))
        screens.put(ScreenEnum.GET_WEAPONS_SCREEN.name, GetWeaponScreen(this))
        screens.put(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name, SimpleInitGameScreen(this))
        screens.put(ScreenEnum.ENDING_SCREEN.name, EndingScreen(this))
        screens.put(ScreenEnum.CREDITS_SCREEN.name, CreditsScreen(this))

        setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name)
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val delta = Gdx.graphics.deltaTime

        setDebugText("FPS: ${Gdx.graphics.framesPerSecond}")

        while (!runQueue.isEmpty) {
            val runnable = runQueue.removeLast()
            runnable.invoke()
        }

        if (!finishedLoadingAssets) {
            val finished = assMan.update(ASSET_MILLIS)

            if (finished) {
                finishedLoadingAssets = true
                postCreate()
            } else {
                val progress = (assMan.progress * 100).toInt()
                loadingText.setText("${LOADING}: $progress%")

                viewports.get(ConstKeys.UI).apply()

                batch.projectionMatrix = getUiCamera().combined
                batch.begin()
                loadingText.draw(batch)
                batch.end()
            }
        } else {
            diagnostics?.beginFrame()

            if (currentScreenKey == ScreenEnum.LEVEL_SCREEN.name) {
                val performance = getPerformance()
                if (performance != Performance.VERY_LOW &&
                    Gdx.graphics.framesPerSecond < performance.fps * AUTO_PERF_FPS_THRESHOLD_SCALAR
                ) {
                    val crisis = Gdx.graphics.framesPerSecond < performance.fps * AUTO_PERF_FPS_CRISIS_SCALAR
                    autoPerfTimer.update(delta * if (crisis) 1.5f else 1f)

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

            diagnostics?.beginEntry("controllerPoller")
            controllerPoller.run()
            diagnostics?.endEntry()

            screenController?.update(delta)

            val screen = currentScreen
            if (screen != null) {
                screen.render(delta)
                screen.draw(batch)
                if (screen is IShapeDebuggable) {
                    shapeRenderer.begin(ShapeType.Line)
                    screen.draw(shapeRenderer)
                    shapeRenderer.end()
                }
            }

            if (params.allowScreenshots) {
                val takeScreenshot = Gdx.input.isKeyJustPressed(SCREENSHOT_KEY)
                if (takeScreenshot) {
                    val currentTime = LocalDateTime.now().toString()
                    val filename = "screenshot_${currentTime}.png"

                    val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.width, Gdx.graphics.height)
                    PixmapIO.writePNG(Gdx.files.external(filename), pixmap, Deflater.DEFAULT_COMPRESSION, true)
                    pixmap.dispose()
                }
            }

            diagnostics?.beginEntry("eventsMan")
            eventsMan.run()
            diagnostics?.endEntry()

            diagnostics?.beginEntry("audioMan")
            audioMan.update(delta)
            diagnostics?.endEntry()

            updatables.values().forEach { it.update(delta) }

            diagnostics?.endFrame()
        }

        if (params.debugText) {
            viewports.get(ConstKeys.UI).apply()
            batch.projectionMatrix = getUiCamera().combined
            batch.begin()
            debugText.draw(batch)
            batch.end()
        }

        if (showNotification) {
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
        }

        GameObjectPools.performNextReclaim()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.TURN_CONTROLLER_OFF -> {
                controllerPoller.on = false
                val turnSystemOff =
                    event.getOrDefaultProperty(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}", true, Boolean::class
                    )
                if (turnSystemOff) getSystem(ControllerSystem::class).on = false
            }

            EventType.TURN_CONTROLLER_ON -> {
                controllerPoller.on = true
                val turnSystemOn =
                    event.getOrDefaultProperty(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.ON}", true, Boolean::class
                    )
                if (turnSystemOn) getSystem(ControllerSystem::class).on = true
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

    fun setCurrentLevel(levelDef: LevelDefinition) = putProperty("${ConstKeys.LEVEL}_${ConstKeys.DEF}", levelDef)

    fun getCurrentLevel() = getProperty("${ConstKeys.LEVEL}_${ConstKeys.DEF}", LevelDefinition::class)!!

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

                return true
            }
        }

        GameLogger.debug(TAG, "loadSavedState(): no state to load")

        return false
    }

    private fun defineControllerPoller(buttons: ControllerButtons) = MegaControllerPoller(buttons)

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
        val drawables = ObjectMap<DrawingSection, InsertionOrderPriorityQueue<IComparableDrawable<Batch>>>()
        DrawingSection.entries.forEach { section -> drawables.put(section, InsertionOrderPriorityQueue()) }
        properties.put(ConstKeys.DRAWABLES, drawables)

        val shapes = Array<IDrawableShape>()
        properties.put(ConstKeys.SHAPES, shapes)

        val engine = GameEngine(
            systems = gdxArrayOf(
                ControllerSystem(controllerPoller),
                BehaviorsSystem(diagnostics),
                WorldSystem(
                    ppm = ConstVals.PPM,
                    fixedStep = getPerformance().fixedStep,
                    worldContainerSupplier = { getWorldContainer() },
                    contactListener = MegaContactListener(this, CONTACT_LISTENER_DEBUG_FILTER),
                    collisionHandler = MegaCollisionHandler(this),
                    contactFilter = MegaContactFilter(),
                    fixedStepScalar = params.fixedStepScalar,
                    maxIterations = 2 * MathUtils.ceil(
                        params.fixedStepScalar / (getPerformance().fixedStep * getPerformance().fps)
                    ),
                    diagnostics = diagnostics
                ),
                CullablesSystem(object : GameEntityCuller {
                    override fun cull(entity: IGameEntity) {
                        (entity as GameEntity).destroy()
                    }
                }, diagnostics = diagnostics),
                MotionSystem(diagnostics),
                when (params.pathfinding) {
                    "async" -> AsyncPathfindingSystem(MegaPathfinderFactory(this), diagnostics = diagnostics)
                    "sync" -> SimplePathfindingSystem(MegaPathfinderFactory(this), diagnostics = diagnostics)
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
                )
            ),
            onQueueToSpawn = { (it as MegaGameEntity).dead = false },
            onQueueToDestroy = { (it as MegaGameEntity).dead = true }
        )

        val systems = ObjectMap<String, GameSystem>()
        engine.systems.forEach { systems.put(it::class.simpleName, it) }
        properties.put(ConstKeys.SYSTEMS, systems)

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

    fun setCameraRotating(value: Boolean) = putProperty("${ConstKeys.CAM}_${ConstKeys.ROTATION}", value)

    fun isCameraRotating() = getOrDefaultProperty("${ConstKeys.CAM}_${ConstKeys.ROTATION}", false, Boolean::class)

    fun getUiCamera() = viewports.get(ConstKeys.UI).camera as OrthographicCamera

    fun getDrawables() =
        properties.get(ConstKeys.DRAWABLES) as ObjectMap<DrawingSection, java.util.Queue<IComparableDrawable<Batch>>>

    fun addDrawable(drawable: IComparableDrawable<Batch>) {
        val section = drawable.priority.section
        val queue = getDrawables().get(section)
        queue.add(drawable)
    }

    fun getShapes() = properties.get(ConstKeys.SHAPES) as Array<IDrawableShape>

    fun getSystems(): ObjectMap<String, GameSystem> =
        properties.get(ConstKeys.SYSTEMS) as ObjectMap<String, GameSystem>

    fun <T : GameSystem> getSystem(clazz: KClass<T>) = clazz.cast(getSystems()[clazz.simpleName]!!)

    fun setRoomsSupplier(supplier: () -> Array<RectangleMapObject>?) =
        properties.put("${ConstKeys.ROOMS}_${ConstKeys.SUPPLIER}", supplier)

    fun getRooms(out: Array<RectangleMapObject>): Array<RectangleMapObject> {
        if (properties.containsKey("${ConstKeys.ROOMS}_${ConstKeys.SUPPLIER}")) {
            val supplier =
                properties.get("${ConstKeys.ROOMS}_${ConstKeys.SUPPLIER}") as () -> Array<RectangleMapObject>?

            val rooms = supplier.invoke()

            if (rooms != null) out.addAll(rooms)
        }
        return out
    }

    fun setCurrentRoomSupplier(supplier: () -> RectangleMapObject?) =
        properties.put("${ConstKeys.ROOM}_${ConstKeys.SUPPLIER}", supplier)

    fun getCurrentRoom(): RectangleMapObject? {
        if (properties.containsKey("${ConstKeys.ROOM}_${ConstKeys.SUPPLIER}")) {
            val supplier = properties.get("${ConstKeys.ROOM}_${ConstKeys.SUPPLIER}") as () -> RectangleMapObject?
            return supplier.invoke()
        }

        return null
    }

    fun setWorldContainer(worldContainer: IWorldContainer) = properties.put(ConstKeys.WORLD_CONTAINER, worldContainer)

    fun getWorldContainer(): IWorldContainer? = properties.get(ConstKeys.WORLD_CONTAINER) as IWorldContainer?

    fun setTiledMapLoadResult(tiledMapLoadResult: TiledMapLoadResult) =
        properties.put(ConstKeys.TILED_MAP_LOAD_RESULT, tiledMapLoadResult)

    fun getTiledMapLoadResult() = properties.get(ConstKeys.TILED_MAP_LOAD_RESULT) as TiledMapLoadResult

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
    fun setFocusSnappedAway(value: Boolean) = putProperty("focus_snapped_away", value)

    fun isFocusSnappedAway() = getOrDefaultProperty("focus_snapped_away", false, Boolean::class)

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
        getSystem(WorldSystem::class).fixedStep = performance.fixedStep
        eventsMan.submitEvent(Event(EventType.CHANGE_PERFORMANCE_MODE, props(ConstKeys.MODE pairTo performance)))
    }

    fun isPerformanceSubpar() = getPerformance().ordinal > Performance.LOW.ordinal
}
