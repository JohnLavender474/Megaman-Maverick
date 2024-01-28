package com.megaman.maverick.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
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
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.controller.ControllerSystem
import com.engine.controller.ControllerUtils
import com.engine.controller.buttons.Button
import com.engine.controller.buttons.Buttons
import com.engine.controller.polling.IControllerPoller
import com.engine.cullables.CullablesSystem
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.drawables.shapes.DrawableShapesSystem
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.ISprite
import com.engine.drawables.sprites.SpritesSystem
import com.engine.graph.IGraphMap
import com.engine.motion.MotionSystem
import com.engine.pathfinding.Pathfinder
import com.engine.pathfinding.PathfindingSystem
import com.engine.points.PointsSystem
import com.engine.systems.IGameSystem
import com.engine.updatables.UpdatablesSystem
import com.engine.world.WorldSystem
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerPoller
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.screens.menus.MainScreen
import com.megaman.maverick.game.screens.menus.bosses.BossIntroScreen
import com.megaman.maverick.game.screens.menus.bosses.BossSelectScreen
import com.megaman.maverick.game.screens.other.SimpleEndLevelScreen
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.getMusics
import com.megaman.maverick.game.utils.getSounds
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.MegaCollisionHandler
import com.megaman.maverick.game.world.MegaContactListener
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
class MegamanMaverickGame : Game2D() {

  companion object {
    const val TAG = "MegamanMaverickGame"
    const val DEBUG_FPS = false
    const val DEBUG_SHAPES = true
    const val DEFAULT_VOLUME = 0.5f
    val TAGS_TO_LOG: ObjectSet<String> = objectSetOf()
  }

  lateinit var megaman: Megaman
  lateinit var audioMan: MegaAudioManager

  private lateinit var fpsText: BitmapFontHandle

  fun startLevelScreen(level: Level) {
    val levelScreen = screens.get(ScreenEnum.LEVEL.name) as MegaLevelScreen
    levelScreen.tmxMapSource = level.tmxSourceFile
    levelScreen.music = level.musicAss
    setCurrentScreen(ScreenEnum.LEVEL.name)
  }

  fun getGameCamera() = viewports.get(ConstKeys.GAME).camera as OrthographicCamera

  fun getUiCamera() = viewports.get(ConstKeys.UI).camera as OrthographicCamera

  fun getSprites() = properties.get(ConstKeys.SPRITES) as MutableCollection<ISprite>

  fun getShapes() = properties.get(ConstKeys.SHAPES) as PriorityQueue<IDrawableShape>

  fun getSystems(): ObjectMap<String, IGameSystem> =
      properties.get(ConstKeys.SYSTEMS) as ObjectMap<String, IGameSystem>

  fun setGraphMap(graphMap: IGraphMap) = properties.put(ConstKeys.WORLD_GRAPH_MAP, graphMap)

  fun getGraphMap(): IGraphMap? = properties.get(ConstKeys.WORLD_GRAPH_MAP) as IGraphMap?

  override fun create() {
    GameLogger.set(GameLogLevel.ERROR)
    GameLogger.filterByTag = true
    GameLogger.tagsToLog.addAll(TAGS_TO_LOG)

    super.create()

    val screenWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
    val screenHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
    val gameViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.GAME, gameViewport)
    val uiViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.UI, uiViewport)

    fpsText =
        BitmapFontHandle(
            { "FPS: ${Gdx.graphics.framesPerSecond}" },
            getDefaultFontSize(),
            Vector2(
                (ConstVals.VIEW_WIDTH - 2) * ConstVals.PPM,
                (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM),
            centerX = true,
            centerY = true,
            fontSource = ConstVals.MEGAMAN_MAVERICK_FONT)

    screens.put(ScreenEnum.LEVEL.name, MegaLevelScreen(this))
    screens.put(ScreenEnum.MAIN.name, MainScreen(this))
    screens.put(ScreenEnum.BOSS_SELECT.name, BossSelectScreen(this))
    screens.put(ScreenEnum.BOSS_INTRO.name, BossIntroScreen(this))
    screens.put(ScreenEnum.SIMPLE_END_LEVEL_SUCCESSFULLY.name, SimpleEndLevelScreen(this))

    audioMan = MegaAudioManager(assMan.getSounds(), assMan.getMusics())
    audioMan.musicVolume = DEFAULT_VOLUME
    audioMan.soundVolume = DEFAULT_VOLUME

    EntityFactories.initialize(this)

    megaman = Megaman(this)
    megaman.init()
    megaman.initialized = true

    // startLevelScreen(Level.TEST1)
    // startLevelScreen(Level.TEST2)
    startLevelScreen(Level.TEST5)
    // setCurrentScreen(ScreenEnum.MAIN.name)
    // startLevelScreen(Level.TIMBER_WOMAN)
    // startLevelScreen(Level.RODENT_MAN)
    // startLevelScreen(Level.FREEZER_MAN)
  }

  override fun render() {
    super.render()
    if (DEBUG_FPS) {
      batch.projectionMatrix = getUiCamera().combined
      batch.begin()
      fpsText.draw(batch)
      batch.end()
    }
  }

  override fun defineControllerPoller(): IControllerPoller {
    val buttons = Buttons()
    buttons.put(ControllerButton.LEFT, Button(Input.Keys.A))
    buttons.put(ControllerButton.RIGHT, Button(Input.Keys.D))
    buttons.put(ControllerButton.UP, Button(Input.Keys.W))
    buttons.put(ControllerButton.DOWN, Button(Input.Keys.S))
    buttons.put(ControllerButton.B, Button(Input.Keys.J))
    buttons.put(ControllerButton.A, Button(Input.Keys.K))
    buttons.put(ControllerButton.START, Button(Input.Keys.ENTER))
    if (ControllerUtils.isControllerConnected()) {
      val mapping = ControllerUtils.getController()?.mapping
      if (mapping != null) {
        buttons.get(ControllerButton.LEFT)?.controllerCode = mapping.buttonDpadLeft
        buttons.get(ControllerButton.RIGHT)?.controllerCode = mapping.buttonDpadRight
        buttons.get(ControllerButton.UP)?.controllerCode = mapping.buttonDpadUp
        buttons.get(ControllerButton.DOWN)?.controllerCode = mapping.buttonDpadDown
        buttons.get(ControllerButton.A)?.controllerCode = mapping.buttonB
        buttons.get(ControllerButton.B)?.controllerCode = mapping.buttonY
        buttons.get(ControllerButton.START)?.controllerCode = mapping.buttonStart
      }
    }
    return MegaControllerPoller(buttons)
  }

  override fun loadAssets(assMan: AssetManager) {
    MusicAsset.values().forEach {
      GameLogger.debug(TAG, "loadAssets(): Loading music asset: ${it.source}")
      assMan.load(it.source, Music::class.java)
    }
    SoundAsset.values().forEach {
      GameLogger.debug(TAG, "loadAssets(): Loading sound asset: ${it.source}")
      assMan.load(it.source, Sound::class.java)
    }
    TextureAsset.values().forEach {
      GameLogger.debug(TAG, "loadAssets(): Loading texture asset: ${it.source}")
      assMan.load(it.source, TextureAtlas::class.java)
    }
  }

  override fun createGameEngine(): IGameEngine {
    val sprites = PriorityQueue<ISprite>()
    properties.put(ConstKeys.SPRITES, sprites)
    val shapes =
        PriorityQueue<IDrawableShape> { s1, s2 -> s1.shapeType.ordinal - s2.shapeType.ordinal }
    properties.put(ConstKeys.SHAPES, shapes)

    val contactFilterMap =
        objectMapOf<Any, ObjectSet<Any>>(
            FixtureType.CONSUMER to objectSetOf(*FixtureType.values()),
            FixtureType.PLAYER to objectSetOf(FixtureType.ITEM),
            FixtureType.DAMAGEABLE to objectSetOf(FixtureType.DEATH, FixtureType.DAMAGER),
            FixtureType.BODY to objectSetOf(FixtureType.FORCE, FixtureType.GRAVITY_CHANGE),
            FixtureType.WATER_LISTENER to objectSetOf(FixtureType.WATER),
            FixtureType.LADDER to objectSetOf(FixtureType.HEAD, FixtureType.FEET),
            FixtureType.SIDE to
                objectSetOf(
                    FixtureType.ICE, FixtureType.GATE, FixtureType.BLOCK, FixtureType.BOUNCER),
            FixtureType.FEET to
                objectSetOf(FixtureType.ICE, FixtureType.BLOCK, FixtureType.BOUNCER),
            FixtureType.HEAD to objectSetOf(FixtureType.BLOCK, FixtureType.BOUNCER),
            FixtureType.PROJECTILE to
                objectSetOf(
                    FixtureType.BODY, FixtureType.BLOCK, FixtureType.SHIELD, FixtureType.WATER),
            FixtureType.LASER to objectSetOf(FixtureType.BLOCK))

    val engine =
        GameEngine(
            ControllerSystem(controllerPoller),
            AnimationsSystem(),
            BehaviorsSystem(),
            WorldSystem(
                MegaContactListener(this),
                { getGraphMap() },
                ConstVals.FIXED_TIME_STEP,
                MegaCollisionHandler(this),
                contactFilterMap),
            CullablesSystem(),
            MotionSystem(),
            PathfindingSystem(
                { Pathfinder(getGraphMap()!!, it.params) },
                timeout = 10,
                timeoutUnit = TimeUnit.MILLISECONDS),
            PointsSystem(),
            UpdatablesSystem(),
            SpritesSystem { sprites },
            DrawableShapesSystem({ shapes }, DEBUG_SHAPES),
            AudioSystem(
                { audioMan.playSound(it.source, it.loop) },
                { audioMan.playMusic(it.source, it.loop) },
                { audioMan.stopSound(it) },
                { audioMan.stopMusic(it) }))

    val systems = ObjectMap<String, IGameSystem>()
    engine.systems.forEach { systems.put(it::class.simpleName, it) }
    properties.put(ConstKeys.SYSTEMS, systems)

    return engine
  }
}
