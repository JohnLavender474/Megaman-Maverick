package com.megaman.maverick.game

import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.viewport.FitViewport
import com.engine.Game2D
import com.engine.GameEngine
import com.engine.IGameEngine
import com.engine.animations.AnimationsSystem
import com.engine.audio.AudioSystem
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.extensions.loadAssetsInDirectory
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
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.MegaLevelScreen
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.MegaCollisionHandler
import com.megaman.maverick.game.world.MegaContactListener
import java.util.*

class MegamanMaverickGame : Game2D() {

  lateinit var megaman: Megaman

  fun startLevelScreen(tmxMapSource: String, music: String) {
    val levelScreen = screens.get(ScreenEnum.LEVEL.name) as MegaLevelScreen
    levelScreen.tmxMapSource = tmxMapSource
    levelScreen.music = music
    setScreen(levelScreen)
  }

  override fun create() {
    // create Megaman
    megaman = Megaman(this)

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

    super.create()
    EntityFactories.initialize(this)
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

  override fun createGameEngine(): IGameEngine {
    val sprites = TreeSet<ISprite>()
    properties.put(ConstKeys.SPRITES, sprites)
    val shapes = OrderedMap<ShapeRenderer.ShapeType, Array<IDrawableShape>>()
    properties.put(ConstKeys.SHAPES, shapes)

    val getGraphMap = { properties.get(ConstKeys.WORLD_GRAPH_MAP) as IGraphMap }

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
                getGraphMap,
                ConstVals.FIXED_TIME_STEP,
                MegaCollisionHandler(),
                contactFilterMap),
            CullablesSystem(),
            MotionSystem(),
            PathfindingSystem { Pathfinder(getGraphMap(), it.params) },
            PointsSystem(),
            UpdatablesSystem(),
            SpriteSystem { sprites },
            DrawableShapeSystem { shapes },
            AudioSystem(assMan))

    val systems = ObjectMap<String, IGameSystem>()
    engine.systems.forEach { systems.put(it::class.simpleName, it) }
    properties.put(ConstKeys.SYSTEMS, systems)

    return engine
  }

  override fun loadAssets(assMan: AssetManager) {
    assMan.loadAssetsInDirectory(ConstKeys.MUSIC, Music::class.java)
    assMan.loadAssetsInDirectory(ConstKeys.SOUNDS, Sound::class.java)
    assMan.loadAssetsInDirectory(
        "${ConstKeys.SPRITES}/${ConstKeys.SPRITE_SHEETS}", TextureAtlas::class.java)
  }
}
