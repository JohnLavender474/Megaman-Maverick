package com.test.game

import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
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
import com.engine.drawables.sprites.SpriteSystem
import com.engine.graph.IGraphMap
import com.engine.motion.MotionSystem
import com.engine.points.PointsSystem
import com.engine.updatables.UpdatablesSystem
import com.engine.world.WorldSystem
import com.test.game.world.ContactListener

class Test2DGame : Game2D() {

  override fun createButtons(): Buttons {
    val buttons = Buttons()
    buttons.put(ConstKeys.LEFT, Button(Input.Keys.A))
    buttons.put(ConstKeys.RIGHT, Button(Input.Keys.D))
    buttons.put(ConstKeys.UP, Button(Input.Keys.W))
    buttons.put(ConstKeys.DOWN, Button(Input.Keys.S))
    buttons.put(ConstKeys.JUMP, Button(Input.Keys.J))
    buttons.put(ConstKeys.ATTACK, Button(Input.Keys.K))
    if (ControllerUtils.isControllerConnected()) {
      val mapping = ControllerUtils.getController()?.mapping
      if (mapping != null) {
        buttons.get(ConstKeys.LEFT)?.controllerCode = mapping.buttonDpadLeft
        buttons.get(ConstKeys.RIGHT)?.controllerCode = mapping.buttonDpadRight
        buttons.get(ConstKeys.UP)?.controllerCode = mapping.buttonDpadUp
        buttons.get(ConstKeys.DOWN)?.controllerCode = mapping.buttonDpadDown
        buttons.get(ConstKeys.JUMP)?.controllerCode = mapping.buttonA
        buttons.get(ConstKeys.ATTACK)?.controllerCode = mapping.buttonX
      }
    }
    return buttons
  }

  override fun createGameEngine() =
      GameEngine(
          ControllerSystem(controllerPoller),
          AnimationsSystem(),
          BehaviorsSystem(),
          WorldSystem(
              contactListener = ContactListener(),
              worldGraphSupplier = {
                val graph = properties.get("world_graph_map", IGraphMap::class)
                graph ?: throw IllegalStateException("No world graph map found in game props")
              },
              fixedStep = ConstVals.FIXED_TIME_STEP),
          CullablesSystem(),
          MotionSystem(),
          // TODO: pathfinding system,
          PointsSystem(),
          UpdatablesSystem(),
          SpriteSystem(viewports.get(ConstKeys.GAME).camera, batch),
          DrawableShapeSystem(shapeRenderer),
          AudioSystem(assMan))

  override fun loadAssets(assMan: AssetManager) {
    assMan.loadAssetsInDirectory("music", Music::class.java)
    assMan.loadAssetsInDirectory("sounds", Sound::class.java)
    assMan.loadAssetsInDirectory("sprites/sprite_sheets", TextureAtlas::class.java)
  }

  override fun create() {
    val screenWidth = ConstVals.VIEW_WIDTH * ConstVals.PPM
    val screenHeight = ConstVals.VIEW_HEIGHT * ConstVals.PPM
    val gameViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.GAME, gameViewport)
    val uiViewport = FitViewport(screenWidth, screenHeight)
    viewports.put(ConstKeys.UI, uiViewport)
    super.create()
  }
}
