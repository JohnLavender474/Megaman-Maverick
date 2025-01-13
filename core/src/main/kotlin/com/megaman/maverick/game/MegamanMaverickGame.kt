package com.megaman.maverick.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
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
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.controller.ControllerSystem
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.ControllerButtons
import com.mega.game.engine.controller.polling.IControllerPoller
import com.mega.game.engine.cullables.CullablesSystem
import com.mega.game.engine.cullables.GameEntityCuller
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
import com.mega.game.engine.pathfinding.IPathfinder
import com.mega.game.engine.pathfinding.IPathfinderFactory
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.SimplePathfindingSystem
import com.mega.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.mega.game.engine.pathfinding.heuristics.IHeuristic
import com.mega.game.engine.points.PointsSystem
import com.mega.game.engine.screens.IScreen
import com.mega.game.engine.screens.levels.tiledmap.TiledMapLoadResult
import com.mega.game.engine.systems.GameSystem
import com.mega.game.engine.updatables.UpdatablesSystem
import com.mega.game.engine.world.WorldSystem
import com.mega.game.engine.world.contacts.Contact
import com.mega.game.engine.world.container.IWorldContainer
import com.mega.game.engine.world.pathfinding.WorldPathfinder
import com.megaman.maverick.game.com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerPoller
import com.megaman.maverick.game.controllers.ScreenController
import com.megaman.maverick.game.controllers.loadButtons
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.StagedMoonLandingFlag
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.MegamanUpgradeHandler
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.screens.menus.*
import com.megaman.maverick.game.screens.menus.bosses.LevelSelectScreen
import com.megaman.maverick.game.screens.menus.temp.BossIntroScreen
import com.megaman.maverick.game.screens.other.CreditsScreen
import com.megaman.maverick.game.screens.other.SimpleEndLevelScreen
import com.megaman.maverick.game.screens.other.SimpleInitGameScreen
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
import java.util.*
import java.util.zip.Deflater
import kotlin.reflect.KClass
import kotlin.reflect.cast

class MegamanMaverickGameParams {
    var debugShapes: Boolean = false
    var debugText: Boolean = false
    var logLevel: GameLogLevel = GameLogLevel.LOG
    var fixedStepScalar: Float = 1f
    var musicVolume: Float = 0.5f
    var soundVolume: Float = 0.5f
    var showScreenController: Boolean = false
}

class MegamanMaverickGame(
    val params: MegamanMaverickGameParams = MegamanMaverickGameParams()
) : Game(), IEventListener, IPropertizable {

    companion object {
        const val TAG = "MegamanMaverickGame"
        private const val ASSET_MILLIS = 17
        private const val LOADING = "LOADING"
        private const val SCREENSHOT_KEY = Input.Keys.P
        val TAGS_TO_LOG: ObjectSet<String> = objectSetOf(StagedMoonLandingFlag.TAG)
        val CONTACT_LISTENER_DEBUG_FILTER: (Contact) -> Boolean = { contact ->
            contact.fixturesMatch(FixtureType.DEATH, FixtureType.FEET)
        }
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.TURN_CONTROLLER_ON, EventType.TURN_CONTROLLER_OFF)
    override val properties = Properties()

    val viewports = ObjectMap<String, Viewport>()
    val screens = ObjectMap<String, IScreen>()
    val currentScreen: IScreen?
        get() = currentScreenKey?.let { screens[it] }

    val disposables = OrderedMap<String, Disposable>()

    lateinit var batch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer

    lateinit var buttons: ControllerButtons
    lateinit var controllerPoller: IControllerPoller

    lateinit var assMan: AssetManager
    lateinit var eventsMan: EventsManager
    lateinit var audioMan: MegaAudioManager

    lateinit var engine: GameEngine
    lateinit var state: GameState

    lateinit var megaman: Megaman
    lateinit var megamanUpgradeHandler: MegamanUpgradeHandler

    var paused = false

    private var currentScreenKey: String? = null
    private var screenController: ScreenController? = null

    private lateinit var debugText: MegaFontHandle
    private lateinit var loadingText: MegaFontHandle
    private var finishedLoadingAssets = false

    fun setCurrentScreen(key: String) {
        GameLogger.debug(TAG, "setCurrentScreen(): set to screen with key = $key")
        currentScreen?.let {
            it.hide()
            it.reset()
        }
        currentScreenKey = key
        currentScreen?.let {
            it.show()
            it.resize(Gdx.graphics.width, Gdx.graphics.height)
        }
        if (paused) resume()
    }

    fun startLevelScreen(levelDef: LevelDefinition) {
        val levelScreen = screens.get(ScreenEnum.LEVEL_SCREEN.name) as MegaLevelScreen

        levelScreen.screenOnCompletion = ScreenEnum.LEVEL_SELECT_SCREEN // TODO: levelDef.screenOnCompletion
        levelScreen.music = levelDef.music
        levelScreen.tmxMapSource = levelDef.source

        setCurrentScreen(ScreenEnum.LEVEL_SCREEN.name)
    }

    fun startLevelScreen(level: Level) {
        val levelScreen = screens.get(ScreenEnum.LEVEL_SCREEN.name) as MegaLevelScreen
        levelScreen.level = level
        levelScreen.music = level.musicAss
        levelScreen.tmxMapSource = level.tmxSourceFile
        setCurrentScreen(ScreenEnum.LEVEL_SCREEN.name)
    }

    fun getGameCamera() = viewports.get(ConstKeys.GAME).camera as RotatableCamera

    fun setCameraRotating(value: Boolean) = putProperty("${ConstKeys.CAM}_${ConstKeys.ROTATION}", value)

    fun isCameraRotating() = getOrDefaultProperty("${ConstKeys.CAM}_${ConstKeys.ROTATION}", false, Boolean::class)

    fun getUiCamera() = viewports.get(ConstKeys.UI).camera as OrthographicCamera

    fun getDrawables() =
        properties.get(ConstKeys.DRAWABLES) as ObjectMap<DrawingSection, PriorityQueue<IComparableDrawable<Batch>>>

    fun addDrawable(drawable: IComparableDrawable<Batch>) {
        val section = drawable.priority.section
        getDrawables().get(section).add(drawable)
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

    fun setDebugText(text: String) = setDebugTextSupplier { text }

    fun setDebugTextSupplier(supplier: () -> String) = debugText.setTextSupplier(supplier)

    override fun create() {
        GameLogger.setLogLevel(params.logLevel)
        GameLogger.filterByTag = true
        GameLogger.tagsToLog.addAll(TAGS_TO_LOG)

        GameLogger.debug(TAG, "create(): appType=${Gdx.app.type}")

        shapeRenderer = ShapeRenderer()
        shapeRenderer.setAutoShapeType(true)
        batch = SpriteBatch()

        controllerPoller = defineControllerPoller()

        eventsMan = EventsManager()
        eventsMan.addListener(this)

        engine = createGameEngine()

        state = GameState()
        EntityFactories.initialize(this)

        val gameWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val gameHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val gameCamera =
            RotatableCamera(onJustFinishedRotating = {
                setCameraRotating(false)
                eventsMan.submitEvent(Event(EventType.END_GAME_CAM_ROTATION))
            })
        gameCamera.setToDefaultPosition()

        val gameViewport = FitViewport(gameWidth, gameHeight, gameCamera)
        viewports.put(ConstKeys.GAME, gameViewport)

        val uiWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val uiHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val uiCamera = OrthographicCamera(uiWidth, uiHeight)
        uiCamera.setToDefaultPosition()

        val uiViewport = FitViewport(uiWidth, uiHeight, uiCamera)
        viewports.put(ConstKeys.UI, uiViewport)

        loadingText = MegaFontHandle(
            "${LOADING}: 0%",
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        )

        debugText = MegaFontHandle(
            ConstVals.EMPTY_STRING,
            positionX = ConstVals.PPM.toFloat(),
            positionY = (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM,
            centerX = false
        )
        // val fpsTextSupplier: () -> String = { "FPS: ${Gdx.graphics.framesPerSecond}" }
        /*
        val megamanPosTextSupplier: () -> String = {
            val pos = megaman.body.getCenter()
            val x = pos.x.toInt() / ConstVals.PPM
            val y = pos.y.toInt() / ConstVals.PPM
            "MM POS: $x,$y"
        }
         */
        // setDebugTextSupplier(fpsTextSupplier)

        assMan = AssetManager()
        queueAssets()
    }

    private fun postCreate() {
        val sounds = OrderedMap<SoundAsset, Sound>()
        val music = OrderedMap<MusicAsset, Music>()
        audioMan = MegaAudioManager(assMan.getSounds(sounds), assMan.getMusics(music))
        audioMan.musicVolume = params.musicVolume
        audioMan.soundVolume = params.soundVolume

        megaman = Megaman(this)
        // manually call init and set initialized to true before megaman is spawned
        megaman.init()
        megaman.initialized = true

        megamanUpgradeHandler = MegamanUpgradeHandler(state, megaman)
        // Megaman should have all upgrades at the start of the game.
        // This can be changed so that Megaman must earn each of these ability (or never attains any of them),
        // but doing so will require reworking level designs and certain parts of the codebase
        megamanUpgradeHandler.add(MegaAbility.CHARGE_WEAPONS)
        megamanUpgradeHandler.add(MegaAbility.AIR_DASH)
        megamanUpgradeHandler.add(MegaAbility.CROUCH)
        megamanUpgradeHandler.add(MegaAbility.GROUND_SLIDE)
        megamanUpgradeHandler.add(MegaAbility.WALL_SLIDE)

        // TODO: remove comment line to debug enhancements
        // megamanUpgradeHandler.add(MegaEnhancement.DAMAGE_INCREASE)
        // megamanUpgradeHandler.add(MegaEnhancement.JUMP_BOOST)
        // megamanUpgradeHandler.add(MegaEnhancement.GROUND_SLIDE_BOOST)
        // megamanUpgradeHandler.add(MegaEnhancement.AIR_DASH_BOOST)
        // megamanUpgradeHandler.add(MegaEnhancement.FASTER_BUSTER_CHARGING)

        screens.put(
            ScreenEnum.LEVEL_SCREEN.name,
            MegaLevelScreen(this) { setCurrentScreen(ScreenEnum.LEVEL_SELECT_SCREEN.name) }
        )
        screens.put(ScreenEnum.MAIN_MENU_SCREEN.name, MainMenuScreen(this))
        screens.put(ScreenEnum.SAVE_GAME_SCREEN.name, SaveGameScreen(this))
        screens.put(ScreenEnum.LOAD_PASSWORD_SCREEN.name, LoadPasswordScreen(this))
        screens.put(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, true))
        screens.put(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, false))
        screens.put(ScreenEnum.LEVEL_SELECT_SCREEN.name, LevelSelectScreen(this))
        screens.put(ScreenEnum.BOSS_INTRO_SCREEN.name, BossIntroScreen(this))
        screens.put(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name, SimpleInitGameScreen(this))
        screens.put(ScreenEnum.SIMPLE_SELECT_LEVEL_SCREEN.name, SimpleSelectLevelScreen(this))
        screens.put(ScreenEnum.SIMPLE_END_LEVEL_SUCCESSFULLY_SCREEN.name, SimpleEndLevelScreen(this))
        screens.put(ScreenEnum.CREDITS_SCREEN.name, CreditsScreen(this))

        // setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
        // startLevelScreen(LevelDefinition.TEST_TILESET_SIZE)
        startLevelScreen(LevelDefinition.TEST_1)

        /*
        if (Gdx.app.type == ApplicationType.Android || params.showScreenController)
            screenController = ScreenController(this)
         */

        /*
        val pixmap = Pixmap(Gdx.files.internal("sprites/frames/Megaman_v2/stand_shoot.png"));
        println("Width: " + pixmap.width + " Height: " + pixmap.height)
        for (x in 0 until pixmap.width) {
            for (y in 0 until pixmap.height) {
                val pixel = pixmap.getPixel(x, y)
                println("Pixel at (" + x + ", " + y + "): " + Integer.toHexString(pixel))
            }
        }
         */
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

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
            val delta = Gdx.graphics.deltaTime

            controllerPoller.run()
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

            val takeScreenshot = Gdx.input.isKeyJustPressed(SCREENSHOT_KEY)
            if (takeScreenshot) {
                val currentTime = LocalDateTime.now().toString()
                val filename = "screenshot_${currentTime}.png"

                val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.width, Gdx.graphics.height)
                PixmapIO.writePNG(Gdx.files.external(filename), pixmap, Deflater.DEFAULT_COMPRESSION, true)
                pixmap.dispose()
            }

            audioMan.update(delta)
            eventsMan.run()
        }

        if (params.debugText) {
            viewports.get(ConstKeys.GAME).apply()

            batch.projectionMatrix = getUiCamera().combined
            batch.begin()
            debugText.draw(batch)
            batch.end()
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
        }
    }

    override fun resize(width: Int, height: Int) {
        GameLogger.debug(TAG, "resize(): width=$width, height=$height")
        viewports.values().forEach { it.update(width, height) }
        currentScreen?.resize(width, height)
        screenController?.resize(width, height)
    }

    override fun pause() {
        GameLogger.debug(TAG, "pause()")
        if (paused) return
        paused = true
        currentScreen?.pause()
    }

    override fun resume() {
        GameLogger.debug(TAG, "resume()")
        if (!paused) return
        paused = false
        currentScreen?.resume()
    }

    override fun dispose() {
        GameLogger.debug(TAG, "dispose()")
        if (this::batch.isInitialized) batch.dispose()
        if (this::shapeRenderer.isInitialized) shapeRenderer.dispose()
        if (this::engine.isInitialized) engine.dispose()
        screens.values().forEach { it.dispose() }
        disposables.values().forEach { it.dispose() }
    }

    fun saveState() {
        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)
        val password = GamePasswords.getGamePassword(state)
        saveFile.putString(ConstKeys.PASSWORD, password.joinToString(""))
        saveFile.flush()
    }

    fun hasSavedState(): Boolean {
        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)
        return saveFile.contains(ConstKeys.PASSWORD)
    }

    fun loadSavedState(): Boolean {
        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)
        if (saveFile.contains(ConstKeys.PASSWORD)) {
            val password = saveFile.getString(ConstKeys.PASSWORD)
            if (password == null) {
                GameLogger.error(TAG, "loadSavedState(): Password is null")
                return false
            } else {
                GameLogger.debug(TAG, "loadSavedState(): Password found")
                GamePasswords.loadGamePassword(state, password.toCharArray().map { it.toString().toInt() }.toIntArray())
                return true
            }
        }
        GameLogger.error(TAG, "loadSavedState(): Password not found")
        return false
    }

    private fun defineControllerPoller(): IControllerPoller {
        buttons = ControllerUtils.loadButtons()
        return MegaControllerPoller(buttons)
    }

    private fun queueAssets() {
        val assets = Array<IAsset>()
        assets.addAll(MusicAsset.valuesAsIAssetArray())
        assets.addAll(SoundAsset.valuesAsIAssetArray())
        assets.addAll(TextureAsset.valuesAsIAssetArray())

        assMan.setLoader(TiledMap::class.java, TmxMapLoader(InternalFileHandleResolver()));
        LevelDefinition.entries.forEach { assets.add(it) }

        assets.forEach {
            GameLogger.debug(TAG, "loadAssets(): Loading ${it.assClass.simpleName} asset: ${it.source}")
            assMan.load(it.source, it.assClass)
        }
    }

    private fun createGameEngine(): GameEngine {
        val drawables = ObjectMap<DrawingSection, PriorityQueue<IComparableDrawable<Batch>>>()
        DrawingSection.entries.forEach { section -> drawables.put(section, PriorityQueue()) }
        properties.put(ConstKeys.DRAWABLES, drawables)

        val shapes = Array<IDrawableShape>()
        properties.put(ConstKeys.SHAPES, shapes)

        val engine = GameEngine(
            systems = gdxArrayOf(
                ControllerSystem(controllerPoller),
                AnimationsSystem(),
                BehaviorsSystem(),
                WorldSystem(
                    ppm = ConstVals.PPM,
                    fixedStep = ConstVals.FIXED_TIME_STEP,
                    worldContainerSupplier = { getWorldContainer() },
                    contactListener = MegaContactListener(this, CONTACT_LISTENER_DEBUG_FILTER),
                    collisionHandler = MegaCollisionHandler(this),
                    contactFilter = MegaContactFilter(),
                    fixedStepScalar = params.fixedStepScalar
                ),
                CullablesSystem(object : GameEntityCuller {
                    override fun cull(entity: IGameEntity) {
                        (entity as GameEntity).destroy()
                    }
                }),
                MotionSystem(),
                SimplePathfindingSystem(
                    factory = object : IPathfinderFactory {
                        override fun getPathfinder(params: PathfinderParams): IPathfinder {
                            val tiledMapResult = getTiledMapLoadResult()
                            return WorldPathfinder(
                                start = params.startCoordinateSupplier(),
                                target = params.targetCoordinateSupplier(),
                                worldWidth = tiledMapResult.worldWidth,
                                worldHeight = tiledMapResult.worldHeight,
                                allowDiagonal = params.allowDiagonal(),
                                allowOutOfWorldBounds = params.getOrDefaultProperty(
                                    ConstKeys.ALLOW_OUT_OF_BOUNDS,
                                    true,
                                    Boolean::class
                                ),
                                filter = params.filter,
                                heuristic = params.getOrDefaultProperty(
                                    ConstKeys.HEURISTIC,
                                    EuclideanHeuristic(),
                                    IHeuristic::class
                                ),
                                maxIterations = params.getOrDefaultProperty(
                                    ConstKeys.ITERATIONS,
                                    ConstVals.DEFAULT_PATHFINDING_MAX_ITERATIONS,
                                    Int::class
                                ),
                                maxDistance = params.getOrDefaultProperty(
                                    ConstKeys.DISTANCE,
                                    ConstVals.DEFAULT_PATHFINDING_MAX_DISTANCE,
                                    Int::class
                                ),
                                returnBestPathOnFailure = params.getOrDefaultProperty(
                                    ConstKeys.DEFAULT,
                                    ConstVals.DEFAULT_RETURN_BEST_PATH,
                                    Boolean::class
                                )
                            )
                        }
                    }
                ),
                PointsSystem(),
                UpdatablesSystem(),
                FontsSystem(this::addDrawable),
                SpritesSystem(this::addDrawable),
                DrawableShapesSystem({ shapes.add(it) }, params.debugShapes),
                AudioSystem(
                    { audioMan.playSound(it.source, it.loop) },
                    { audioMan.playMusic(it.source, it.loop) },
                    { audioMan.stopSound(it) },
                    { audioMan.stopMusic(it) })
            ),
            onQueueToSpawn = { (it as MegaGameEntity).dead = false },
            onQueueToDestroy = { (it as MegaGameEntity).dead = true }
        )

        val systems = ObjectMap<String, GameSystem>()
        engine.systems.forEach { systems.put(it::class.simpleName, it) }
        properties.put(ConstKeys.SYSTEMS, systems)

        return engine
    }
}