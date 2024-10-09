package com.megaman.maverick.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.mega.game.engine.GameEngine
import com.mega.game.engine.animations.AnimationsSystem
import com.mega.game.engine.audio.AudioSystem
import com.mega.game.engine.behaviors.BehaviorsSystem
import com.mega.game.engine.common.GameLogLevel
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.objects.MultiCollectionIterable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.controller.ControllerSystem
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.ControllerButtons
import com.mega.game.engine.controller.polling.IControllerPoller
import com.mega.game.engine.cullables.CullablesSystem
import com.mega.game.engine.cullables.GameEntityCuller
import com.mega.game.engine.drawables.IDrawable
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
import com.mega.game.engine.pathfinding.IPathfinder
import com.mega.game.engine.pathfinding.IPathfinderFactory
import com.mega.game.engine.pathfinding.PathfinderParams
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
import com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerPoller
import com.megaman.maverick.game.controllers.loadButtons
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.hazards.Saw
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.Megaman.Companion.MEGAMAN_EVENT_LISTENER_TAG
import com.megaman.maverick.game.entities.megaman.MegamanUpgradeHandler
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.screens.menus.*
import com.megaman.maverick.game.screens.menus.bosses.BossIntroScreen
import com.megaman.maverick.game.screens.menus.bosses.BossSelectScreen
import com.megaman.maverick.game.screens.other.CreditsScreen
import com.megaman.maverick.game.screens.other.SimpleEndLevelScreen
import com.megaman.maverick.game.screens.other.SimpleInitGameScreen
import com.megaman.maverick.game.spawns.debugFilterByEntityTag
import com.megaman.maverick.game.utils.getMusics
import com.megaman.maverick.game.utils.getSounds
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactListener
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

enum class StartScreenOption { MAIN, SIMPLE, LEVEL }

class MegamanMaverickGameParams {
    var debug: Boolean = false
    var startScreen: StartScreenOption = StartScreenOption.MAIN
    var startLevel: Level? = null
    var fixedStepScalar: Float = 1f
    var musicVolume: Float = 0.5f
    var soundVolume: Float = 0.5f
}

class MegamanMaverickGame(val params: MegamanMaverickGameParams) : Game(), IEventListener, IPropertizable {

    companion object {
        const val TAG = "MegamanMaverickGame"
        val TAGS_TO_LOG: ObjectSet<String> = objectSetOf(MEGAMAN_EVENT_LISTENER_TAG)
        val CONTACT_LISTENER_DEBUG_FILTER: (Contact) -> Boolean = { contact ->
            contact.fixturesMatch(FixtureType.WATER, FixtureType.WATER_LISTENER)
        }
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.TURN_CONTROLLER_ON, EventType.TURN_CONTROLLER_OFF)
    override val properties = Properties()

    val viewports = ObjectMap<String, Viewport>()
    val screens = ObjectMap<String, IScreen>()
    val currentScreen: IScreen?
        get() = currentScreenKey?.let { screens[it] }

    lateinit var batch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer
    lateinit var buttons: ControllerButtons
    lateinit var controllerPoller: IControllerPoller
    lateinit var assMan: AssetManager
    lateinit var eventsMan: EventsManager
    lateinit var engine: GameEngine
    lateinit var state: GameState
    lateinit var megaman: Megaman
    lateinit var megamanUpgradeHandler: MegamanUpgradeHandler
    lateinit var audioMan: MegaAudioManager

    var paused = false

    private lateinit var debugText: IDrawable<Batch>
    private var currentScreenKey: String? = null

    fun setCurrentScreen(key: String) {
        GameLogger.debug(TAG, "setCurrentScreen: set to screen with key = $key")
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

    fun getShapes() = properties.get(ConstKeys.SHAPES) as PriorityQueue<IDrawableShape>

    fun getSystems(): ObjectMap<String, GameSystem> =
        properties.get(ConstKeys.SYSTEMS) as ObjectMap<String, GameSystem>

    fun <T : GameSystem> getSystem(clazz: KClass<T>) = clazz.cast(getSystems()[clazz.simpleName]!!)

    fun setCurrentRoomSupplier(supplier: () -> String?) =
        properties.put("${ConstKeys.ROOM}_${ConstKeys.SUPPLIER}", supplier)

    fun getCurrentRoom(): String? {
        if (properties.containsKey("${ConstKeys.ROOM}_${ConstKeys.SUPPLIER}")) {
            val supplier = properties.get("${ConstKeys.ROOM}_${ConstKeys.SUPPLIER}") as () -> String?
            return supplier.invoke()
        }
        return null
    }

    fun setWorldContainer(worldContainer: IWorldContainer) = properties.put(ConstKeys.WORLD_CONTAINER, worldContainer)

    fun getWorldContainer(): IWorldContainer? = properties.get(ConstKeys.WORLD_CONTAINER) as IWorldContainer?

    fun setTiledMapLoadResult(tiledMapLoadResult: TiledMapLoadResult) =
        properties.put(ConstKeys.TILED_MAP_LOAD_RESULT, tiledMapLoadResult)

    fun getTiledMapLoadResult() = properties.get(ConstKeys.TILED_MAP_LOAD_RESULT) as TiledMapLoadResult

    fun setTargetFPS(value: Int) {
        putProperty(ConstKeys.FPS, value)
        Gdx.graphics?.setForegroundFPS(value) ?: GameLogger.error(
            TAG,
            "Tried setting target fps when Gdx.graphcis is null"
        )
    }

    fun getTargetFPS() = getProperty(ConstKeys.FPS, Int::class)!!

    fun setDoLerpGameCamera(lerp: Boolean) = putProperty(ConstKeys.LERP, lerp)

    fun doLerpGameCamera() = getProperty(ConstKeys.LERP, Boolean::class)!!

    fun setLerpValueForGameCamera(value: String) = putProperty("${ConstKeys.LERP}_${ConstKeys.VALUE}", value)

    // returns the string representation of the lerp value,
    // float value needs to be calculated
    fun getLerpValueForGameCamera() = getProperty("${ConstKeys.LERP}_${ConstKeys.VALUE}", String::class)!!

    // converts the string representation of the lerp value into the float value
    fun calculateLerpValueForGameCamera(): Float {
        val speed = this.getLerpValueForGameCamera()
        return when (speed) {
            ConstKeys.FAST -> ConstVals.FAST_LERP_VALUE
            ConstKeys.MEDIUM -> ConstVals.MEDIUM_LERP_VALUE
            ConstKeys.SLOW -> ConstVals.SLOW_LERP_VALUE
            else -> throw IllegalStateException("Illegal lerp value: $speed")
        }
    }

    fun doUseVsync() = getProperty(ConstKeys.VSYNC, Boolean::class)!!

    fun setUseVsync(use: Boolean) {
        putProperty(ConstKeys.VSYNC, use)
        Gdx.graphics?.setVSync(use) ?: GameLogger.error(TAG, "Tried setting vysnc when Gdx.graphics is null")
    }

    override fun create() {
        GameLogger.set(GameLogLevel.ERROR)
        GameLogger.filterByTag = true
        GameLogger.tagsToLog.addAll(TAGS_TO_LOG)
        GameLogger.debug(TAG, "create()")
        debugFilterByEntityTag.addAll(Saw.TAG)

        shapeRenderer = ShapeRenderer()
        shapeRenderer.setAutoShapeType(true)
        batch = SpriteBatch()
        controllerPoller = defineControllerPoller()
        assMan = AssetManager()
        loadAssets(assMan)
        assMan.finishLoading()
        eventsMan = EventsManager()
        eventsMan.addListener(this)

        engine = createGameEngine()

        val screenWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val screenHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM

        val gameCamera =
            RotatableCamera(onJustFinishedRotating = {
                setCameraRotating(false)
                eventsMan.submitEvent(Event(EventType.END_GAME_CAM_ROTATION))
            })
        val gameViewport = FitViewport(screenWidth, screenHeight, gameCamera)
        viewports.put(ConstKeys.GAME, gameViewport)

        val uiViewport = FitViewport(screenWidth, screenHeight)
        viewports.put(ConstKeys.UI, uiViewport)

        debugText = MegaFontHandle({ "FPS: ${Gdx.graphics.framesPerSecond}" })

        audioMan = MegaAudioManager(assMan.getSounds(), assMan.getMusics())
        audioMan.musicVolume = params.musicVolume
        audioMan.soundVolume = params.soundVolume

        EntityFactories.initialize(this)
        state = GameState()

        megaman = Megaman(this)
        // manually call init and set initialized to true before megaman is set to be spawned in the game engine
        megaman.init()
        megaman.initialized = true

        megamanUpgradeHandler = MegamanUpgradeHandler(state, megaman)

        // Megaman should have all upgrades at the start of the game.
        // This can be changed so that Megaman must earn each ability,
        // but doing so will require reworking level designs and certains parts of the codebase
        megamanUpgradeHandler.add(MegaAbility.CHARGE_WEAPONS)
        megamanUpgradeHandler.add(MegaAbility.AIR_DASH)
        megamanUpgradeHandler.add(MegaAbility.GROUND_SLIDE)
        megamanUpgradeHandler.add(MegaAbility.WALL_SLIDE)

        screens.put(
            ScreenEnum.LEVEL_SCREEN.name,
            MegaLevelScreen(this) {
                when (params.startScreen) {
                    StartScreenOption.MAIN, StartScreenOption.LEVEL -> Gdx.app.exit()
                    StartScreenOption.SIMPLE -> setCurrentScreen(ScreenEnum.SIMPLE_SELECT_LEVEL_SCREEN.name)
                }
            })
        screens.put(ScreenEnum.MAIN_MENU_SCREEN.name, MainMenuScreen(this))
        screens.put(ScreenEnum.SAVE_GAME_SCREEN.name, SaveGameScreen(this))
        screens.put(ScreenEnum.LOAD_PASSWORD_SCREEN.name, LoadPasswordScreen(this))
        screens.put(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, true))
        screens.put(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, false))
        screens.put(ScreenEnum.CAMERA_SETTINGS_SCREEN.name, CameraSettingsScreen(this))
        screens.put(ScreenEnum.BOSS_SELECT_SCREEN.name, BossSelectScreen(this))
        screens.put(ScreenEnum.BOSS_INTRO_SCREEN.name, BossIntroScreen(this))
        screens.put(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name, SimpleInitGameScreen(this))
        screens.put(ScreenEnum.SIMPLE_SELECT_LEVEL_SCREEN.name, SimpleSelectLevelScreen(this))
        screens.put(ScreenEnum.SIMPLE_END_LEVEL_SUCCESSFULLY_SCREEN.name, SimpleEndLevelScreen(this))
        screens.put(ScreenEnum.CREDITS_SCREEN.name, CreditsScreen(this))

        when (params.startScreen) {
            StartScreenOption.LEVEL -> startLevelScreen(params.startLevel!!)
            StartScreenOption.SIMPLE -> setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name)
            else -> setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
        }

        setDoLerpGameCamera(ConstVals.DEFAULT_LERP_SETTING)
        setLerpValueForGameCamera(ConstKeys.FAST)
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

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val delta = Gdx.graphics.deltaTime

        controllerPoller.run()
        eventsMan.run()
        audioMan.update(delta)

        currentScreen?.render(delta)
        // TODO: should not apply viewports all at once at end of render(); rather should apply each viewport
        //  individually right before drawing drawables that are to be contained in the viewport
        // viewports.values().forEach { it.apply(true) }

        if (params.debug) {
            batch.projectionMatrix = getUiCamera().combined
            batch.begin()
            debugText.draw(batch)
            batch.end()
        }
    }

    override fun resize(width: Int, height: Int) {
        viewports.values().forEach { it.update(width, height) }
        currentScreen?.resize(width, height)
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

    private fun loadAssets(assMan: AssetManager) =
        MultiCollectionIterable<IAsset>(
            gdxArrayOf(
                MusicAsset.valuesAsIAssetArray(),
                SoundAsset.valuesAsIAssetArray(),
                TextureAsset.valuesAsIAssetArray()
            )
        ).forEach {
            GameLogger.debug(TAG, "loadAssets(): Loading ${it.assClass.simpleName} asset: ${it.source}")
            assMan.load(it.source, it.assClass)
        }

    private fun createGameEngine(): GameEngine {
        val drawables = ObjectMap<DrawingSection, PriorityQueue<IComparableDrawable<Batch>>>()
        DrawingSection.values().forEach { section -> drawables.put(section, PriorityQueue()) }
        properties.put(ConstKeys.DRAWABLES, drawables)

        val shapes = PriorityQueue<IDrawableShape> { s1, s2 -> s1.shapeType.ordinal - s2.shapeType.ordinal }
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
                    contactFilterMap = objectMapOf(
                        FixtureType.CONSUMER pairTo objectSetOf(*FixtureType.values()),
                        FixtureType.PLAYER pairTo objectSetOf(FixtureType.BODY, FixtureType.ITEM),
                        FixtureType.DAMAGEABLE pairTo objectSetOf(FixtureType.DAMAGER),
                        FixtureType.BODY pairTo objectSetOf(
                            FixtureType.BODY,
                            FixtureType.BLOCK,
                            FixtureType.FORCE,
                            FixtureType.GRAVITY_CHANGE
                        ),
                        FixtureType.DEATH pairTo objectSetOf(
                            FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                        ),
                        FixtureType.WATER pairTo objectSetOf(FixtureType.WATER_LISTENER),
                        FixtureType.LADDER pairTo objectSetOf(FixtureType.HEAD, FixtureType.FEET),
                        FixtureType.SIDE pairTo objectSetOf(
                            FixtureType.ICE, FixtureType.GATE, FixtureType.BLOCK, FixtureType.BOUNCER
                        ),
                        FixtureType.FEET pairTo objectSetOf(
                            FixtureType.ICE, FixtureType.BLOCK, FixtureType.BOUNCER, FixtureType.SAND, FixtureType.CART
                        ),
                        FixtureType.HEAD pairTo objectSetOf(FixtureType.BLOCK, FixtureType.BOUNCER),
                        FixtureType.PROJECTILE pairTo objectSetOf(
                            FixtureType.BODY,
                            FixtureType.BLOCK,
                            FixtureType.WATER,
                            FixtureType.SHIELD,
                            FixtureType.SAND,
                            FixtureType.PROJECTILE
                        ),
                        FixtureType.LASER pairTo objectSetOf(FixtureType.BLOCK),
                        FixtureType.TELEPORTER pairTo objectSetOf(FixtureType.TELEPORTER_LISTENER)
                    ),
                    fixedStepScalar = params.fixedStepScalar
                ),
                CullablesSystem(object : GameEntityCuller {
                    override fun cull(entity: IGameEntity) {
                        (entity as GameEntity).destroy()
                    }
                }),
                MotionSystem(),
                AsyncPathfindingSystem(
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
                FontsSystem { font -> drawables.get(font.priority.section).add(font) },
                SpritesSystem { sprite -> drawables.get(sprite.priority.section).add(sprite) },
                DrawableShapesSystem({ shapes.add(it) }, params.debug),
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
