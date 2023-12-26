package com.megaman.maverick.game

import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
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
import com.engine.cullables.CullablesSystem
import com.engine.drawables.shapes.DrawableShapeSystem
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.ISprite
import com.engine.drawables.sprites.SpriteSystem
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
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_JUMP_BEHAVIOR_TAG
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_SWIM_BEHAVIOR_TAG
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.utils.getMusics
import com.megaman.maverick.game.utils.getSounds
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.MegaCollisionHandler
import com.megaman.maverick.game.world.MegaContactListener
import java.util.*

@Suppress("UNCHECKED_CAST")
class MegamanMaverickGame : Game2D() {

  companion object {
    const val TAG = "MegamanMaverickGame"
  }

  lateinit var megaman: Megaman
  lateinit var audioMan: MegaAudioManager

  fun startLevelScreen(level: Level) {
    val levelScreen = screens.get(ScreenEnum.LEVEL.name) as MegaLevelScreen
    levelScreen.tmxMapSource = level.tmxSourceFile
    levelScreen.music = level.musicAss
    setCurrentScreen(ScreenEnum.LEVEL.name)
  }

  fun getGameCamera() = viewports.get(ConstKeys.GAME).camera as OrthographicCamera

  fun getUiCamera() = viewports.get(ConstKeys.UI).camera as OrthographicCamera

  fun getSprites() = properties.get(ConstKeys.SPRITES) as MutableCollection<ISprite>

  fun getShapes() = properties.get(ConstKeys.SHAPES) as Array<IDrawableShape>

  fun getSystems(): ObjectMap<String, IGameSystem> =
      properties.get(ConstKeys.SYSTEMS) as ObjectMap<String, IGameSystem>

  fun setGraphMap(graphMap: IGraphMap) = properties.put(ConstKeys.WORLD_GRAPH_MAP, graphMap)

  fun getGraphMap(): IGraphMap? = properties.get(ConstKeys.WORLD_GRAPH_MAP) as IGraphMap?

  override fun create() {
    // set log level
    GameLogger.set(GameLogLevel.ERROR)
    // filter by tags
    GameLogger.filterByTag = true
    GameLogger.tagsToLog.addAll(
        MegaContactListener.TAG, MEGAMAN_JUMP_BEHAVIOR_TAG, MEGAMAN_SWIM_BEHAVIOR_TAG)

    // set viewports
    val screenWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
    val screenHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
    val gameViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.GAME, gameViewport)
    val uiViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.UI, uiViewport)

    // TODO: set screens
    /*
    screens.put(ScreenEnum.LEVEL, new LevelScreen(this));
    screens.put(ScreenEnum.MAIN, new MainScreen(this));
    screens.put(ScreenEnum.BOSS_SELECT, new BSelectScreen(this));
    screens.put(ScreenEnum.BOSS_INTRO, new BIntroScreen(this));
    */
    screens.put(ScreenEnum.LEVEL.name, MegaLevelScreen(this))

    // call to super
    super.create()

    // create Audio Manager
    audioMan = MegaAudioManager(assMan.getSounds(), assMan.getMusics())

    // initialize entity factories
    EntityFactories.initialize(this)

    // create Megaman
    megaman = Megaman(this)
    megaman.init()
    megaman.initialized = true

    // TODO: test level screen
    startLevelScreen(Level.TEST1)
  }

  override fun createButtons(): Buttons {
    val buttons = Buttons()
    buttons.put(ConstKeys.LEFT, Button(Input.Keys.A))
    buttons.put(ConstKeys.RIGHT, Button(Input.Keys.D))
    buttons.put(ConstKeys.UP, Button(Input.Keys.W))
    buttons.put(ConstKeys.DOWN, Button(Input.Keys.S))
    buttons.put(ConstKeys.A, Button(Input.Keys.J))
    buttons.put(ConstKeys.B, Button(Input.Keys.K))
    buttons.put(ConstKeys.START, Button(Input.Keys.ENTER))
    if (ControllerUtils.isControllerConnected()) {
      val mapping = ControllerUtils.getController()?.mapping
      if (mapping != null) {
        buttons.get(ConstKeys.LEFT)?.controllerCode = mapping.buttonDpadLeft
        buttons.get(ConstKeys.RIGHT)?.controllerCode = mapping.buttonDpadRight
        buttons.get(ConstKeys.UP)?.controllerCode = mapping.buttonDpadUp
        buttons.get(ConstKeys.DOWN)?.controllerCode = mapping.buttonDpadDown
        buttons.get(ConstKeys.A)?.controllerCode = mapping.buttonA
        buttons.get(ConstKeys.B)?.controllerCode = mapping.buttonX
        buttons.get(ConstKeys.START).controllerCode = mapping.buttonStart
      }
    }
    return buttons
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
    val shapes = Array<IDrawableShape>()
    properties.put(ConstKeys.SHAPES, shapes)

    val contactFilterMap =
        objectMapOf<Any, ObjectSet<Any>>(
            FixtureType.CONSUMER to objectSetOf(*FixtureType.values()),
            FixtureType.PLAYER to objectSetOf(FixtureType.ITEM),
            FixtureType.DAMAGEABLE to objectSetOf(FixtureType.DEATH, FixtureType.DAMAGER),
            FixtureType.BODY to objectSetOf(FixtureType.FORCE, FixtureType.UPSIDE_DOWN),
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
                MegaCollisionHandler(),
                contactFilterMap),
            CullablesSystem(),
            MotionSystem(),
            PathfindingSystem { Pathfinder(getGraphMap()!!, it.params) },
            PointsSystem(),
            UpdatablesSystem(),
            SpriteSystem { sprites },
            DrawableShapeSystem { shapes },
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
