package com.megaman.maverick.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.engine.GameEngine
import com.engine.animations.AnimationsSystem
import com.engine.audio.AudioSystem
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.GameLogLevel
import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.IPropertizable
import com.engine.common.objects.MultiCollectionIterable
import com.engine.common.objects.Properties
import com.engine.controller.ControllerSystem
import com.engine.controller.ControllerUtils
import com.engine.controller.buttons.Buttons
import com.engine.controller.polling.IControllerPoller
import com.engine.cullables.CullablesSystem
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.drawables.fonts.FontsSystem
import com.engine.drawables.shapes.DrawableShapesSystem
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sorting.IComparableDrawable
import com.engine.drawables.sprites.SpritesSystem
import com.engine.events.Event
import com.engine.events.EventsManager
import com.engine.events.IEventListener
import com.engine.events.IEventsManager
import com.engine.graph.IGraphMap
import com.engine.motion.MotionSystem
import com.engine.pathfinding.Pathfinder
import com.engine.pathfinding.PathfindingSystem
import com.engine.points.PointsSystem
import com.engine.screens.IScreen
import com.engine.systems.IGameSystem
import com.engine.updatables.UpdatablesSystem
import com.engine.world.Contact
import com.engine.world.WorldSystem
import com.megaman.maverick.game.assets.IAsset
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerPoller
import com.megaman.maverick.game.controllers.loadButtons
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.MegamanUpgradeHandler
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.projectiles.FallingIcicle
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.menus.ControllerSettingsScreen
import com.megaman.maverick.game.screens.menus.LoadPasswordScreen
import com.megaman.maverick.game.screens.menus.MainMenuScreen
import com.megaman.maverick.game.screens.menus.SaveGameScreen
import com.megaman.maverick.game.screens.menus.bosses.BossIntroScreen
import com.megaman.maverick.game.screens.menus.bosses.BossSelectScreen
import com.megaman.maverick.game.screens.other.CreditsScreen
import com.megaman.maverick.game.screens.other.SimpleEndLevelScreen
import com.megaman.maverick.game.screens.other.SimpleInitGameScreen
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.getMusics
import com.megaman.maverick.game.utils.getSounds
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.MegaCollisionHandler
import com.megaman.maverick.game.world.MegaContactListener
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.cast

enum class StartScreenOption {
    MAIN, LEVEL
}

class MegamanMaverickGameParams {
    var debug: Boolean = false
    var startScreen: StartScreenOption = StartScreenOption.MAIN
    var startLevel: Level? = null
}

class MegamanMaverickGame(val params: MegamanMaverickGameParams) : Game(), IEventListener, IPropertizable {

    companion object {
        const val TAG = "MegamanMaverickGame"
        const val DEFAULT_VOLUME = 0.5f
        val TAGS_TO_LOG: ObjectSet<String> = objectSetOf(FallingIcicle.TAG)
        val CONTACT_LISTENER_DEBUG_FILTER: (Contact) -> Boolean = { contact ->
            contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)
        }
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.TURN_CONTROLLER_ON,
        EventType.TURN_CONTROLLER_OFF
    )
    override val properties = Properties()

    val viewports = ObjectMap<String, Viewport>()
    val screens = ObjectMap<String, IScreen>()
    val currentScreen: IScreen?
        get() = currentScreenKey?.let { screens[it] }

    lateinit var batch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer
    lateinit var buttons: Buttons
    lateinit var controllerPoller: IControllerPoller
    lateinit var assMan: AssetManager
    lateinit var eventsMan: IEventsManager
    lateinit var engine: GameEngine
    lateinit var state: GameState
    lateinit var megaman: Megaman
    lateinit var megamanUpgradeHandler: MegamanUpgradeHandler
    lateinit var audioMan: MegaAudioManager

    var paused = false

    private lateinit var debugText: BitmapFontHandle
    private var currentScreenKey: String? = null

    fun setCurrentScreen(key: String) {
        GameLogger.debug(TAG, "setCurrentScreen: set to screen with key = $key")
        currentScreenKey?.let { screens[it] }?.hide()
        currentScreenKey = key
        screens[key]?.let { nextScreen ->
            nextScreen.show()
            nextScreen.resize(Gdx.graphics.width, Gdx.graphics.height)
            if (paused) nextScreen.pause()
        }
    }

    fun startLevelScreen(level: Level) {
        val levelScreen = screens.get(ScreenEnum.LEVEL_SCREEN.name) as MegaLevelScreen
        levelScreen.level = level
        levelScreen.music = level.musicAss
        levelScreen.tmxMapSource = level.tmxSourceFile
        setCurrentScreen(ScreenEnum.LEVEL_SCREEN.name)
    }

    fun getBackgroundCamera() = viewports.get(ConstKeys.BACKGROUND).camera as OrthographicCamera

    fun getGameCamera() = viewports.get(ConstKeys.GAME).camera as OrthographicCamera

    fun getForegroundCamera() = viewports.get(ConstKeys.FOREGROUND).camera as OrthographicCamera

    fun getUiCamera() = viewports.get(ConstKeys.UI).camera as OrthographicCamera

    fun getDrawables() =
        properties.get(ConstKeys.DRAWABLES) as ObjectMap<DrawingSection, PriorityQueue<IComparableDrawable<Batch>>>

    fun getShapes() = properties.get(ConstKeys.SHAPES) as PriorityQueue<IDrawableShape>

    fun getSystems(): ObjectMap<String, IGameSystem> =
        properties.get(ConstKeys.SYSTEMS) as ObjectMap<String, IGameSystem>

    fun <T : IGameSystem> getSystem(clazz: KClass<T>) = clazz.cast(getSystems()[clazz.simpleName]!!)

    fun setGraphMap(graphMap: IGraphMap) = properties.put(ConstKeys.WORLD_GRAPH_MAP, graphMap)

    fun getGraphMap(): IGraphMap? = properties.get(ConstKeys.WORLD_GRAPH_MAP) as IGraphMap?

    override fun create() {
        GameLogger.set(GameLogLevel.ERROR)
        GameLogger.filterByTag = true
        GameLogger.tagsToLog.addAll(TAGS_TO_LOG)
        GameLogger.debug(TAG, "create()")

        shapeRenderer = ShapeRenderer()
        shapeRenderer.setAutoShapeType(true)
        batch = SpriteBatch()
        controllerPoller = defineControllerPoller()
        assMan = AssetManager()
        loadAssets(assMan)
        assMan.finishLoading()
        engine = createGameEngine()
        eventsMan = EventsManager()
        eventsMan.addListener(this)

        val screenWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val screenHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val backgroundViewport = FitViewport(screenWidth, screenHeight)
        viewports.put(ConstKeys.BACKGROUND, backgroundViewport)
        val gameViewport = FitViewport(screenWidth, screenHeight)
        viewports.put(ConstKeys.GAME, gameViewport)
        val foregroundViewport = FitViewport(screenWidth, screenHeight)
        viewports.put(ConstKeys.FOREGROUND, foregroundViewport)
        val uiViewport = FitViewport(screenWidth, screenHeight)
        viewports.put(ConstKeys.UI, uiViewport)

        debugText = BitmapFontHandle(
            { "FPS: ${Gdx.graphics.framesPerSecond}" }, getDefaultFontSize(), Vector2(
                (ConstVals.VIEW_WIDTH - 2) * ConstVals.PPM, (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM
            ), centerX = true, centerY = true, fontSource = ConstVals.MEGAMAN_MAVERICK_FONT
        )

        audioMan = MegaAudioManager(assMan.getSounds(), assMan.getMusics())
        audioMan.musicVolume = DEFAULT_VOLUME
        audioMan.soundVolume = DEFAULT_VOLUME

        EntityFactories.initialize(this)

        state = GameState()

        megaman = Megaman(this)
        megaman.init()
        megaman.initialized = true

        megamanUpgradeHandler = MegamanUpgradeHandler(state, megaman)

        // Megaman should have all upgrades at the start of the game
        megamanUpgradeHandler.add(MegaAbility.CHARGE_WEAPONS)
        megamanUpgradeHandler.add(MegaAbility.AIR_DASH)
        megamanUpgradeHandler.add(MegaAbility.GROUND_SLIDE)
        megamanUpgradeHandler.add(MegaAbility.WALL_SLIDE)

        screens.put(ScreenEnum.LEVEL_SCREEN.name, MegaLevelScreen(this))
        screens.put(ScreenEnum.MAIN_MENU_SCREEN.name, MainMenuScreen(this))
        screens.put(ScreenEnum.SAVE_GAME_SCREEN.name, SaveGameScreen(this))
        screens.put(ScreenEnum.LOAD_PASSWORD_SCREEN.name, LoadPasswordScreen(this))
        screens.put(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, true))
        screens.put(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, false))
        screens.put(ScreenEnum.BOSS_SELECT_SCREEN.name, BossSelectScreen(this))
        screens.put(ScreenEnum.BOSS_INTRO_SCREEN.name, BossIntroScreen(this))
        screens.put(ScreenEnum.SIMPLE_END_LEVEL_SUCCESSFULLY_SCREEN.name, SimpleEndLevelScreen(this))
        screens.put(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name, SimpleInitGameScreen(this))
        screens.put(ScreenEnum.CREDITS.name, CreditsScreen(this))

        if (params.startScreen == StartScreenOption.LEVEL) startLevelScreen(params.startLevel!!)
        else setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.TURN_CONTROLLER_OFF -> {
                controllerPoller.on = false
                val turnSystemOff =
                    event.getOrDefaultProperty(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}",
                        true,
                        Boolean::class
                    )
                if (turnSystemOff) getSystem(ControllerSystem::class).on = false
            }

            EventType.TURN_CONTROLLER_ON -> {
                controllerPoller.on = true
                val turnSystemOn =
                    event.getOrDefaultProperty(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.ON}",
                        true,
                        Boolean::class
                    )
                if (turnSystemOn) getSystem(ControllerSystem::class).on = true
            }
        }
    }

    override fun render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit()

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val delta = Gdx.graphics.deltaTime
        controllerPoller.run()
        eventsMan.run()
        currentScreen?.render(delta)
        viewports.values().forEach { it.apply() }
        audioMan.update(delta)

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
        batch.dispose()
        shapeRenderer.dispose()
        screens.values().forEach { it.dispose() }
    }

    fun saveState() {
        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)
        val password = GamePasswords.getGamePassword(state)
        saveFile.putString(ConstKeys.PASSWORD, password.joinToString(""))
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
            ControllerSystem(controllerPoller),
            AnimationsSystem(),
            BehaviorsSystem(),
            WorldSystem(
                contactListener = MegaContactListener(this, CONTACT_LISTENER_DEBUG_FILTER),
                worldGraphSupplier = { getGraphMap() },
                fixedStep = ConstVals.FIXED_TIME_STEP,
                collisionHandler = MegaCollisionHandler(this),
                contactFilterMap = objectMapOf(
                    FixtureType.CONSUMER to objectSetOf(*FixtureType.values()),
                    FixtureType.PLAYER to objectSetOf(FixtureType.BODY, FixtureType.ITEM),
                    FixtureType.DAMAGEABLE to objectSetOf(FixtureType.DAMAGER),
                    FixtureType.BODY to objectSetOf(FixtureType.BLOCK, FixtureType.FORCE, FixtureType.GRAVITY_CHANGE),
                    FixtureType.DEATH to objectSetOf(
                        FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                    ),
                    FixtureType.WATER to objectSetOf(FixtureType.WATER_LISTENER),
                    FixtureType.LADDER to objectSetOf(FixtureType.HEAD, FixtureType.FEET),
                    FixtureType.SIDE to objectSetOf(
                        FixtureType.ICE, FixtureType.GATE, FixtureType.BLOCK, FixtureType.BOUNCER
                    ),
                    FixtureType.FEET to objectSetOf(
                        FixtureType.ICE, FixtureType.BLOCK, FixtureType.BOUNCER, FixtureType.SAND, FixtureType.CART
                    ),
                    FixtureType.HEAD to objectSetOf(FixtureType.BLOCK, FixtureType.BOUNCER),
                    FixtureType.PROJECTILE to objectSetOf(
                        FixtureType.BODY,
                        FixtureType.BLOCK,
                        FixtureType.WATER,
                        FixtureType.SHIELD,
                        FixtureType.SAND,
                        FixtureType.PROJECTILE
                    ),
                    FixtureType.LASER to objectSetOf(FixtureType.BLOCK),
                    FixtureType.TELEPORTER to objectSetOf(FixtureType.TELEPORTER_LISTENER)
                ),
                debug = true
            ),
            CullablesSystem(),
            MotionSystem(),
            PathfindingSystem(
                pathfinderFactory = { Pathfinder(getGraphMap()!!, it.params) },
                timeout = 10,
                timeoutUnit = TimeUnit.MILLISECONDS
            ),
            PointsSystem(),
            UpdatablesSystem(),
            FontsSystem { font -> drawables.get(font.priority.section).add(font) },
            SpritesSystem { sprite -> drawables.get(sprite.priority.section).add(sprite) },
            DrawableShapesSystem({ shapes.add(it) }, params.debug),
            AudioSystem({ audioMan.playSound(it.source, it.loop) },
                { audioMan.playMusic(it.source, it.loop) },
                { audioMan.stopSound(it) },
                { audioMan.stopMusic(it) })
        )

        val systems = ObjectMap<String, IGameSystem>()
        engine.systems.forEach { systems.put(it::class.simpleName, it) }
        properties.put(ConstKeys.SYSTEMS, systems)

        return engine
    }
}
