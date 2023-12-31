@file:Suppress("UNCHECKED_CAST")

package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGameEngine
import com.engine.animations.AnimationsSystem
import com.engine.audio.AudioSystem
import com.engine.audio.IAudioManager
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.interfaces.Initializable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.controller.ControllerSystem
import com.engine.controller.buttons.ButtonStatus
import com.engine.controller.polling.IControllerPoller
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.ISprite
import com.engine.events.Event
import com.engine.events.IEventsManager
import com.engine.graph.SimpleNodeGraphMap
import com.engine.motion.MotionSystem
import com.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.engine.spawns.ISpawner
import com.engine.spawns.SpawnsManager
import com.engine.systems.IGameSystem
import com.engine.updatables.UpdatablesSystem
import com.engine.world.WorldSystem
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.drawables.sprites.Background
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.CameraManagerForRooms
import com.megaman.maverick.game.screens.levels.events.PlayerDeathEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerSpawnEventHandler
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuilders
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuildersParams
import com.megaman.maverick.game.screens.levels.spawns.PlayerSpawnsManager
import com.megaman.maverick.game.screens.levels.stats.PlayerStatsHandler

class MegaLevelScreen(game: MegamanMaverickGame) :
    TiledMapLevelScreen(game, props()), Initializable {

  companion object {
    const val TAG = "MegaLevelScreen"
  }

  val megamanGame: MegamanMaverickGame
    get() = super.game as MegamanMaverickGame

  val engine: IGameEngine
    get() = game.gameEngine

  val megaman: Megaman
    get() = megamanGame.megaman

  val eventsMan: IEventsManager
    get() = megamanGame.eventsMan

  val audioMan: IAudioManager
    get() = megamanGame.audioMan

  val controllerPoller: IControllerPoller
    get() = megamanGame.controllerPoller

  var music: MusicAsset? = null

  private lateinit var spawnsMan: SpawnsManager
  private lateinit var playerSpawnsMan: PlayerSpawnsManager
  private lateinit var levelStateHandler: LevelStateHandler
  private lateinit var playerStatsHandler: PlayerStatsHandler
  private lateinit var cameraManagerForRooms: CameraManagerForRooms

  private lateinit var playerSpawnEventHandler: PlayerSpawnEventHandler
  private lateinit var playerDeathEventHandler: PlayerDeathEventHandler

  private lateinit var sprites: MutableCollection<ISprite>
  private lateinit var shapes: Array<IDrawableShape>
  private lateinit var backgrounds: Array<Background>

  private lateinit var gameCamera: OrthographicCamera
  private lateinit var uiCamera: OrthographicCamera

  private lateinit var disposables: Array<Disposable>

  private var initialized = false

  override fun init() {
    if (initialized) return
    initialized = true

    disposables = Array()

    spawnsMan = SpawnsManager()
    levelStateHandler = LevelStateHandler(megamanGame)

    sprites = megamanGame.getSprites()
    shapes = megamanGame.getShapes()
    gameCamera = megamanGame.getGameCamera()
    uiCamera = megamanGame.getUiCamera()

    playerSpawnsMan = PlayerSpawnsManager(gameCamera)

    playerStatsHandler = PlayerStatsHandler(megaman)
    playerStatsHandler.init()

    playerSpawnEventHandler = PlayerSpawnEventHandler(megamanGame)
    playerDeathEventHandler = PlayerDeathEventHandler(megamanGame)

    // array of systems that should be switched off and back on during room transitions
    @Suppress("UNCHECKED_CAST")
    val systems = megamanGame.properties.get(ConstKeys.SYSTEMS) as ObjectMap<String, IGameSystem>
    val systemsToSwitch =
        gdxArrayOf(
            AnimationsSystem::class,
            ControllerSystem::class,
            MotionSystem::class,
            UpdatablesSystem::class,
            BehaviorsSystem::class,
            WorldSystem::class,
            AudioSystem::class)

    cameraManagerForRooms = CameraManagerForRooms(gameCamera)
    cameraManagerForRooms.focus = megaman

    // set begin transition logic for camera manager
    cameraManagerForRooms.beginTransition = {
      systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = false } }

      megamanGame.eventsMan.submitEvent(
          Event(
              EventType.BEGIN_ROOM_TRANS,
              props(
                  ConstKeys.POSITION to cameraManagerForRooms.transitionInterpolation,
                  ConstKeys.CURRENT to cameraManagerForRooms.currentGameRoom,
                  ConstKeys.PRIOR to cameraManagerForRooms.priorGameRoom)))
    }

    // set continue transition logic for camera manager
    cameraManagerForRooms.continueTransition = { _ ->
      if (cameraManagerForRooms.delayJustFinished)
          systems.get(AnimationsSystem::class.simpleName)?.on = true

      megamanGame.eventsMan.submitEvent(
          Event(
              EventType.CONTINUE_ROOM_TRANS,
              props(ConstKeys.POSITION to cameraManagerForRooms.transitionInterpolation)))
    }

    // set end transition logic for camera manager
    cameraManagerForRooms.endTransition = {
      systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = true } }

      eventsMan.submitEvent(
          Event(
              EventType.END_ROOM_TRANS,
              props(ConstKeys.ROOM to cameraManagerForRooms.currentGameRoom)))

      if (cameraManagerForRooms.currentGameRoom?.name.equals(ConstKeys.BOSS))
          eventsMan.submitEvent(
              Event(
                  EventType.ENTER_BOSS_ROOM,
                  props(ConstKeys.ROOM to cameraManagerForRooms.currentGameRoom)))
    }
  }

  override fun show() {
    // lazily initialize this level screen
    if (!initialized) init()

    dispose()
    super.show()

    // add this screen as an event listener
    eventsMan.addListener(this)

    // reset positions of cameras
    uiCamera.position.set(ConstFuncs.getCamInitPos())
    gameCamera.position.set(ConstFuncs.getCamInitPos())

    // set all systems to on
    engine.systems.forEach { it.on = true }

    // start playing the level music
    music?.let { audioMan.playMusic(it, true) }

    // set the world graph map using the tiled map load result
    if (tiledMapLoadResult == null)
        throw IllegalStateException("No tiled map load result found in level screen")
    val (_, _, worldWidth, worldHeight) = tiledMapLoadResult!!

    // TODO: should use quad tree graph map instead of simple node graph map
    /*
    val depth = (worldWidth).coerceAtLeast(worldHeight) / ConstVals.PPM
    val worldGraphMap = QuadTreeGraphMap(0, 0, worldWidth, worldHeight, ConstVals.PPM, depth)
     */
    val worldGraphMap = SimpleNodeGraphMap(0, 0, worldWidth, worldHeight, ConstVals.PPM)
    megamanGame.setGraphMap(worldGraphMap)

    playerSpawnEventHandler.init()
  }

  override fun getLayerBuilders() =
      MegaMapLayerBuilders(MegaMapLayerBuildersParams(game as MegamanMaverickGame, spawnsMan))

  override fun buildLevel(result: Properties) {
    GameLogger.debug(TAG, "buildLevel(): Properties = $result")

    // set the backgrounds array
    backgrounds = result.get(ConstKeys.BACKGROUNDS) as Array<Background>? ?: Array()

    // set the player spawns
    val playerSpawns =
        result.get("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}") as Array<RectangleMapObject>?
            ?: Array()
    playerSpawnsMan.set(playerSpawns)

    // set the game rooms for the camera manager
    cameraManagerForRooms.gameRooms =
        result.get(ConstKeys.GAME_ROOMS) as Array<RectangleMapObject>? ?: Array()

    // set the spawners for blocks, enemies, items, etc.
    val spawners = result.get(ConstKeys.SPAWNERS) as Array<ISpawner>? ?: Array()
    spawnsMan.setSpawners(spawners)

    // add the level dispose logic to the disposables array
    val _disposables = result.get(ConstKeys.DISPOSABLES) as Array<Disposable>? ?: Array()
    disposables.addAll(_disposables)
  }

  override fun onEvent(event: Event) {
    when (event.key) {
      EventType.GAME_PAUSE -> {
        GameLogger.debug(TAG, "onEvent(): Game pause --> pause the game")
        megamanGame.pause()
      }
      EventType.GAME_RESUME -> {
        GameLogger.debug(TAG, "onEvent(): Game resume --> resume the game")
        megamanGame.resume()
      }
      EventType.PLAYER_SPAWN -> {
        GameLogger.debug(
            TAG, "onEvent(): Player spawn --> reset camera manager for rooms and spawn megaman")
        cameraManagerForRooms.reset()
        GameLogger.debug(
            TAG,
            "onEvent(): Player spawn --> spawn Megaman: ${playerSpawnsMan.currentSpawnProps!!}")
        megamanGame.gameEngine.spawn(megaman, playerSpawnsMan.currentSpawnProps!!)
      }
      EventType.PLAYER_READY -> {
        GameLogger.debug(TAG, "onEvent(): Player ready")
      }
      EventType.PLAYER_JUST_DIED -> {
        GameLogger.debug(TAG, "onEvent(): Player just died --> init death handler")
        audioMan.stopMusic()
        playerDeathEventHandler.init()
      }
      EventType.PLAYER_DONE_DYIN -> {
        GameLogger.debug(TAG, "onEvent(): Player done dying --> respawn player")
        music?.let { audioMan.playMusic(it, true) }
        playerSpawnEventHandler.init()
      }
      EventType.ADD_PLAYER_HEALTH -> {
        val healthNeeded = megaman.getMaxHealth() - megaman.getCurrentHealth()
        if (healthNeeded > 0) {
          val health = event.properties.get(ConstKeys.VALUE) as Int
          playerStatsHandler.addHealth(health)
        }
      }
      EventType.ADD_HEART_TANK -> {
        val heartTank = event.properties.get(ConstKeys.VALUE) as MegaHeartTank
        playerStatsHandler.attain(heartTank)
      }
      EventType.GATE_INIT_OPENING -> {
        GameLogger.debug(TAG, "onEvent(): Gate init opening --> start room transition")
      }
      EventType.NEXT_ROOM_REQ -> {
        GameLogger.debug(TAG, "onEvent(): Next room req --> start room transition")
      }
      EventType.GATE_INIT_CLOSING -> {
        GameLogger.debug(TAG, "onEvent(): Gate init closing --> start room transition")
      }
      EventType.REQ_SHAKE_CAM -> {
        GameLogger.debug(TAG, "onEvent(): Req shake cam --> shake the camera")
      }
      else -> {
        GameLogger.debug(TAG, "onEvent(): Unhandled event: $event")
      }
    }
  }

  override fun render(delta: Float) {
    // game can only be paused if neither spawn nor death event handlers are running
    if (controllerPoller.getButtonStatus(ConstKeys.START) == ButtonStatus.JUST_PRESSED &&
        playerStatsHandler.finished &&
        playerSpawnEventHandler.finished &&
        playerDeathEventHandler.finished)
        if (megamanGame.paused) megamanGame.resume() else megamanGame.pause()

    // illegal for game to be paused when spawn or death event handlers are running
    // force game resume
    if (game.paused &&
        (!playerStatsHandler.finished ||
            !playerSpawnEventHandler.finished ||
            !playerDeathEventHandler.finished))
        game.resume()

    // things to run only when game is NOT paused
    if (!game.paused) {
      // update backgrounds
      backgrounds.forEach { it.update(delta) }

      // update the camera manager for rooms
      cameraManagerForRooms.update(delta)

      // spawns do not update when player is first spawning if there is a room transition underway
      if (playerSpawnEventHandler.finished && !cameraManagerForRooms.transitioning) {
        playerSpawnsMan.run()
        spawnsMan.update(delta)
        val spawns = spawnsMan.getSpawnsAndClear()
        spawns.forEach { spawn -> engine.spawn(spawn.entity, spawn.properties) }
      }

      if (!playerSpawnEventHandler.finished) playerSpawnEventHandler.update(delta)
      else if (!playerDeathEventHandler.finished) playerDeathEventHandler.update(delta)
      else if (!playerStatsHandler.finished) playerStatsHandler.update(delta)
    }

    // update the game engine
    engine.update(delta)

    // render the level
    val batch = megamanGame.batch
    batch.projectionMatrix = gameCamera.combined
    batch.begin()

    backgrounds.forEach { it.spriteMatrix.draw(batch) }
    tiledMapLevelRenderer?.render(gameCamera)

    sprites.forEach { it.draw(batch) }
    sprites.clear()

    batch.end()

    // render the ui
    batch.projectionMatrix = uiCamera.combined
    batch.begin()
    playerStatsHandler.draw(batch)
    // TODO: render ui
    // TODO: render player stats
    if (!playerSpawnEventHandler.finished) playerSpawnEventHandler.draw(batch)
    batch.end()

    // render the shapes
    val shapeRenderer = megamanGame.shapeRenderer
    shapeRenderer.projectionMatrix = gameCamera.combined

    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    shapes.forEach { it.draw(shapeRenderer) }
    shapes.clear()
    shapeRenderer.end()
  }

  override fun hide() = dispose()

  override fun dispose() {
    GameLogger.debug(TAG, "dispose(): Disposing level screen")
    super.dispose()

    if (initialized) {
      disposables.forEach { it.dispose() }
      disposables.clear()

      engine.reset()
      audioMan.stopMusic()
      eventsMan.removeListener(this)

      spawnsMan.reset()
      playerSpawnsMan.reset()
      cameraManagerForRooms.reset()
    }
  }

  override fun pause() = levelStateHandler.pause()

  override fun resume() = levelStateHandler.resume()
}
