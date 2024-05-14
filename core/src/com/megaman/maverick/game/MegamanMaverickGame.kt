package com.megaman.maverick.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.viewport.FitViewport
import com.engine.Game2D
import com.engine.GameEngine
import com.engine.IGameEngine
import com.engine.animations.AnimationsSystem
import com.engine.audio.AudioSystem
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.GameLogLevel
import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.MultiCollectionIterable
import com.engine.controller.ControllerSystem
import com.engine.controller.ControllerUtils
import com.engine.controller.polling.IControllerPoller
import com.engine.cullables.CullablesSystem
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.drawables.fonts.FontsSystem
import com.engine.drawables.shapes.DrawableShapesSystem
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sorting.IComparableDrawable
import com.engine.drawables.sprites.SpritesSystem
import com.engine.events.EventsManager
import com.engine.graph.IGraphMap
import com.engine.motion.MotionSystem
import com.engine.pathfinding.Pathfinder
import com.engine.pathfinding.PathfindingSystem
import com.engine.points.PointsSystem
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
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.menus.ControllerSettingsScreen
import com.megaman.maverick.game.screens.menus.LoadPasswordScreen
import com.megaman.maverick.game.screens.menus.MainMenuScreen
import com.megaman.maverick.game.screens.menus.SaveScreen
import com.megaman.maverick.game.screens.menus.bosses.BossIntroScreen
import com.megaman.maverick.game.screens.menus.bosses.BossSelectScreen
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

class MegamanMaverickGame : Game2D() {

    companion object {
        const val TAG = "MegamanMaverickGame"
        const val DEBUG_TEXT = false
        const val DEBUG_SHAPES = false
        const val DEFAULT_VOLUME = 0.5f
        val TAGS_TO_LOG: ObjectSet<String> = objectSetOf(ControllerSettingsScreen.TAG)
        val CONTACT_LISTENER_DEBUG_FILTER: (Contact) -> Boolean = { contact ->
            contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)
        }
    }

    lateinit var state: GameState

    lateinit var megaman: Megaman
    lateinit var megamanUpgradeHandler: MegamanUpgradeHandler

    lateinit var audioMan: MegaAudioManager

    private lateinit var debugText: BitmapFontHandle

    fun startLevelScreen(level: Level) {
        val levelScreen = screens.get(ScreenEnum.LEVEL_SCREEN.name) as MegaLevelScreen
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
            { "VEL_X: ${(megaman.body.physics.velocity.x / ConstVals.PPM).toInt()}" }, getDefaultFontSize(), Vector2(
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

        screens.put(ScreenEnum.LEVEL_SCREEN.name, MegaLevelScreen(this))
        screens.put(ScreenEnum.MAIN_MENU_SCREEN.name, MainMenuScreen(this))
        screens.put(ScreenEnum.SAVE_SCREEN.name, SaveScreen(this))
        screens.put(ScreenEnum.LOAD_GAME_SCREEN.name, LoadPasswordScreen(this))
        screens.put(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, true))
        screens.put(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name, ControllerSettingsScreen(this, buttons, false))
        screens.put(ScreenEnum.BOSS_SELECT_SCREEN.name, BossSelectScreen(this))
        screens.put(ScreenEnum.BOSS_INTRO_SCREEN.name, BossIntroScreen(this))
        screens.put(ScreenEnum.SIMPLE_END_LEVEL_SUCCESSFULLY_SCREEN.name, SimpleEndLevelScreen(this))
        screens.put(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name, SimpleInitGameScreen(this))

        // startLevelScreen(Level.TEST1)
        // startLevelScreen(Level.TEST2)
        // startLevelScreen(Level.TEST3)
        // startLevelScreen(Level.TEST4)
        // startLevelScreen(Level.TEST5)
        // startLevelScreen(Level.TEST6)
        // startLevelScreen(Level.TEST7)
        // startLevelScreen(Level.MAGNET_MAN)
        // startLevelScreen(Level.TIMBER_WOMAN
        // startLevelScreen(Level.CREW_MAN)
        // startLevelScreen(Level.FREEZE_MAN)
        // startLevelScreen(Level.GALAXY_MAN)
        // startLevelScreen(Level.WILY_STAGE_1)
        // startLevelScreen(Level.WILY_STAGE_2)
        // startLevelScreen(Level.WILY_STAGE_3)
        // setCurrentScreen(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name)
        // setCurrentScreen(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name)
        // setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name)
        // setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
    }

    override fun render() {
        super.render()
        val delta = Gdx.graphics.deltaTime
        audioMan.update(delta)
        if (DEBUG_TEXT) {
            batch.projectionMatrix = getUiCamera().combined
            batch.begin()
            debugText.draw(batch)
            batch.end()
        }
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

    fun loadSavedState() {
        val saveFile = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE)
        if (saveFile.contains(ConstKeys.PASSWORD)) {
            val password = saveFile.getString(ConstKeys.PASSWORD)
            GamePasswords.loadGamePassword(state, password.toCharArray().map { it.toString().toInt() }.toIntArray())
        }
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

    private fun createGameEngine(): IGameEngine {
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
                MegaContactListener(this, CONTACT_LISTENER_DEBUG_FILTER),
                { getGraphMap() },
                ConstVals.FIXED_TIME_STEP,
                MegaCollisionHandler(this),
                objectMapOf(
                    FixtureType.CONSUMER to objectSetOf(*FixtureType.values()),
                    FixtureType.PLAYER to objectSetOf(FixtureType.ITEM),
                    FixtureType.DAMAGEABLE to objectSetOf(FixtureType.DAMAGER),
                    FixtureType.BODY to objectSetOf(FixtureType.FORCE, FixtureType.GRAVITY_CHANGE),
                    FixtureType.DEATH to objectSetOf(
                        FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                    ),
                    FixtureType.WATER_LISTENER to objectSetOf(FixtureType.WATER),
                    FixtureType.LADDER to objectSetOf(FixtureType.HEAD, FixtureType.FEET),
                    FixtureType.SIDE to objectSetOf(
                        FixtureType.ICE, FixtureType.GATE, FixtureType.BLOCK, FixtureType.BOUNCER
                    ),
                    FixtureType.FEET to objectSetOf(FixtureType.ICE, FixtureType.BLOCK, FixtureType.BOUNCER),
                    FixtureType.HEAD to objectSetOf(FixtureType.BLOCK, FixtureType.BOUNCER),
                    FixtureType.PROJECTILE to objectSetOf(
                        FixtureType.BODY,
                        FixtureType.BLOCK,
                        FixtureType.SHIELD,
                        FixtureType.WATER,
                        FixtureType.PROJECTILE
                    ),
                    FixtureType.LASER to objectSetOf(FixtureType.BLOCK)
                ),
                debug = true
            ),
            CullablesSystem(),
            MotionSystem(),
            PathfindingSystem(
                { Pathfinder(getGraphMap()!!, it.params) }, timeout = 10, timeoutUnit = TimeUnit.MILLISECONDS
            ),
            PointsSystem(),
            UpdatablesSystem(),
            FontsSystem { font -> drawables.get(font.priority.section).add(font) },
            SpritesSystem { sprite -> drawables.get(sprite.priority.section).add(sprite) },
            DrawableShapesSystem({ shapes.add(it) }, DEBUG_SHAPES),
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
