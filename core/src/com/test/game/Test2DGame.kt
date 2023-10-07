package com.test.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.engine.GameEngine
import com.engine.IGameEngine
import com.engine.audio.AudioManager
import com.engine.audio.IAudioManager
import com.engine.common.extensions.loadAssetsInDirectory
import com.engine.common.objects.Properties
import com.engine.controller.ControllerSystem
import com.engine.controller.ControllerUtils
import com.engine.controller.buttons.Button
import com.engine.controller.buttons.Buttons
import com.engine.controller.polling.ControllerPoller
import com.engine.events.EventsManager
import com.engine.events.IEventsManager

class Test2DGame : Game() {

  lateinit var batch: SpriteBatch
  lateinit var shapeRenderer: ShapeRenderer
  lateinit var buttons: Buttons
  lateinit var controllerPoller: ControllerPoller
  lateinit var assMan: AssetManager
  lateinit var audioMan: IAudioManager
  lateinit var eventsMan: IEventsManager
  lateinit var gameEngine: IGameEngine

  val disposables = Array<Disposable>()
  val screens = ObjectMap<String, Screen>()
  val viewports = ObjectMap<String, Viewport>()
  val currentScreen: Screen?
    get() = currentScreenKey?.let { screens[it] }

  val properties = Properties()

  var paused = false
  var currentScreenKey: String? = null

  fun setCurrentScreen(key: String?) {
    currentScreenKey?.let { screens[it] }?.hide()
    currentScreenKey = key

    key?.let {
      screens[it]?.let { nextScreen ->
        nextScreen.show()
        nextScreen.resize(Gdx.graphics.width, Gdx.graphics.height)

        if (paused) {
          nextScreen.pause()
        }
      }
    }
  }

  override fun create() {
    batch = SpriteBatch()
    shapeRenderer = ShapeRenderer()

    buttons = Buttons()
    buttons.put("left", Button(Input.Keys.A))
    buttons.put("right", Button(Input.Keys.D))
    buttons.put("up", Button(Input.Keys.W))
    buttons.put("down", Button(Input.Keys.S))
    buttons.put("jump", Button(Input.Keys.J))
    buttons.put("attack", Button(Input.Keys.K))
    if (ControllerUtils.isControllerConnected()) {
      val mapping = ControllerUtils.getController()?.mapping
      if (mapping != null) {
        buttons.get("left")?.controllerCode = mapping.buttonDpadLeft
        buttons.get("right")?.controllerCode = mapping.buttonDpadRight
        buttons.get("up")?.controllerCode = mapping.buttonDpadUp
        buttons.get("down")?.controllerCode = mapping.buttonDpadDown
        buttons.get("jump")?.controllerCode = mapping.buttonA
        buttons.get("attack")?.controllerCode = mapping.buttonX
      }
    }
    controllerPoller = ControllerPoller(buttons)

    assMan = AssetManager()
    assMan.loadAssetsInDirectory("music", Music::class.java)
    assMan.loadAssetsInDirectory("sounds", Sound::class.java)
    assMan.loadAssetsInDirectory("sprites/sprite_sheets", TextureAtlas::class.java)
    assMan.finishLoading()

    audioMan = AudioManager(assMan)
    eventsMan = EventsManager()

    val screenWidth = ConstantVals.VIEW_WIDTH * ConstantVals.PPM
    val screenHeight = ConstantVals.VIEW_HEIGHT * ConstantVals.PPM
    val gameViewport = FitViewport(screenWidth, screenHeight)
    viewports.put("game", gameViewport)
    val uiViewport = FitViewport(screenWidth, screenHeight)
    viewports.put("ui", uiViewport)

    gameEngine = GameEngine(
      ControllerSystem(controllerPoller)
    )

    disposables.add(batch)
    disposables.add(shapeRenderer)
    disposables.add(assMan)
  }

  override fun resize(width: Int, height: Int) {
    viewports.values().forEach { it.update(width, height) }
    currentScreen?.resize(width, height)
  }

  override fun render() {
    val delta = Gdx.graphics.deltaTime

    audioMan.update(delta)
    controllerPoller.run()
    eventsMan.run()

    currentScreen?.render(delta)
    viewports.values().forEach { it.apply() }
  }

  override fun pause() {
    if (paused) {
      return
    }

    paused = true
    currentScreen?.pause()
  }

  override fun resume() {
    if (!paused) {
      return
    }

    paused = false
    currentScreen?.resume()
  }

  override fun dispose() {
    screens.values().forEach { it.dispose() }
    screens.clear()
    disposables.forEach { it.dispose() }
    disposables.clear()
  }
}
