package com.megaman.maverick.game

import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.viewport.FitViewport
import com.engine.Game2D
import com.engine.GameEngine
import com.engine.animations.AnimationsSystem
import com.engine.audio.AudioSystem
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.extensions.loadAssetsInDirectory
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
import com.engine.points.PointsSystem
import com.engine.systems.IGameSystem
import com.engine.updatables.UpdatablesSystem
import com.engine.world.WorldSystem
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.world.ContactListener
import com.megaman.maverick.game.world.MegaCollisionHandler
import java.util.*

class MegamanMaverickGame : Game2D() {

  override fun create() {
    // put drawawble collections into props
    val sprites = TreeSet<ISprite>()
    properties.put(ConstKeys.SPRITES, sprites)
    val shapes = OrderedMap<ShapeRenderer.ShapeType, Array<IDrawableShape>>()
    properties.put(ConstKeys.SHAPES, shapes)

    // set viewports
    val screenWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
    val screenHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
    val gameViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.GAME, gameViewport)
    val uiViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.UI, uiViewport)

    // TODO: set screens
    super.create()

    val systems = ObjectMap<String, IGameSystem>()
    gameEngine.systems.forEach { systems.put(it::class.simpleName, it) }
    properties.put(ConstKeys.SYSTEMS, systems)

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

  @Suppress("UNCHECKED_CAST")
  override fun createGameEngine() =
      GameEngine(
          ControllerSystem(controllerPoller),
          AnimationsSystem(),
          BehaviorsSystem(),
          WorldSystem(
              contactListener = ContactListener(),
              worldGraphSupplier = {
                val graph = properties.get(ConstKeys.WORLD_GRAPH_MAP) as IGraphMap?
                graph ?: throw IllegalStateException("No world graph map found in game props")
              },
              fixedStep = ConstVals.FIXED_TIME_STEP,
              collisionHandler = MegaCollisionHandler()
              // TODO: set contact filter map
              ),
          CullablesSystem(),
          MotionSystem(),
          // TODO: pathfinding system,
          PointsSystem(),
          UpdatablesSystem(),
          SpriteSystem(properties.get(ConstKeys.SPRITES) as TreeSet<ISprite>),
          DrawableShapeSystem(
              properties.get(ConstKeys.SHAPES)
                  as OrderedMap<ShapeRenderer.ShapeType, Array<IDrawableShape>>),
          AudioSystem(assMan))

  override fun loadAssets(assMan: AssetManager) {
    assMan.loadAssetsInDirectory(ConstKeys.MUSIC, Music::class.java)
    assMan.loadAssetsInDirectory(ConstKeys.SOUNDS, Sound::class.java)
    assMan.loadAssetsInDirectory(
        "${ConstKeys.SPRITES}/${ConstKeys.SPRITE_SHEETS}", TextureAtlas::class.java)
  }
}
