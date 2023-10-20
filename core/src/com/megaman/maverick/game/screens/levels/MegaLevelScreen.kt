package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.AnimationsSystem
import com.engine.audio.AudioSystem
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.Initializable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.controller.ControllerSystem
import com.engine.controller.buttons.ButtonStatus
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.ISprite
import com.engine.events.Event
import com.engine.graph.QuadTreeGraphMap
import com.engine.motion.MotionSystem
import com.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.engine.spawns.SpawnsManager
import com.engine.systems.IGameSystem
import com.engine.updatables.UpdatablesSystem
import com.engine.world.WorldSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.drawables.sprites.Background
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.CameraManagerForRooms
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuilders
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuildersParams
import com.megaman.maverick.game.screens.levels.spawns.PlayerSpawnsManager
import java.util.*

/**
 * This class is a level screen that is used for the entire game. It is a tiled map level screen
 * that uses a tiled map to build the level. The fields [tmxMapSource] and [music] should be set
 * before [show] is called.
 *
 * @param game the game instance
 */
class MegaLevelScreen(game: MegamanMaverickGame) :
    TiledMapLevelScreen(game, props()), Initializable {

  // empty set means this class listens to all events
  override val eventKeyMask: ObjectSet<Any> = objectSetOf()

  var music: String? = null

  private lateinit var spawnsMan: SpawnsManager
  private lateinit var playerSpawnsMan: PlayerSpawnsManager
  private lateinit var levelStateHandler: LevelStateHandler
  private lateinit var cameraManagerForRooms: CameraManagerForRooms

  private lateinit var sprites: TreeSet<ISprite>
  private lateinit var shapes: Array<IDrawableShape>
  private lateinit var backgrounds: Array<Background>

  private lateinit var gameCamera: Camera
  private lateinit var uiCamera: Camera

  private var initialized = false

  /**
   * This method initializes this level screen. It is called lazily when the level screen is shown.
   * It initializes the spawns manager, the level state handler, the backgrounds array, the player
   * spawns manager, and the camera manager for rooms.
   */
  override fun init() {
    if (initialized) return
    initialized = true

    spawnsMan = SpawnsManager()
    levelStateHandler = LevelStateHandler(game)

    @Suppress("UNCHECKED_CAST")
    sprites = game.properties.get(ConstKeys.SPRITES) as TreeSet<ISprite>
    @Suppress("UNCHECKED_CAST")
    shapes = game.properties.get(ConstKeys.SHAPES) as Array<IDrawableShape>

    uiCamera = game.viewports.get(ConstKeys.UI).camera
    gameCamera = game.viewports.get(ConstKeys.GAME).camera

    playerSpawnsMan = PlayerSpawnsManager(gameCamera)

    // array of systems that should be switched off and back on during room transitions
    @Suppress("UNCHECKED_CAST")
    val systems = game.properties.get(ConstKeys.SYSTEMS) as ObjectMap<String, IGameSystem>
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

    // set begin transition logic for camera manager
    cameraManagerForRooms.beginTransition = {
      systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = false } }

      game.eventsMan.submitEvent(
          Event(
              EventType.BEGIN_ROOM_TRANS,
              props(
                  ConstKeys.POSITION to cameraManagerForRooms.transitionInterpolation,
                  ConstKeys.CURRENT to cameraManagerForRooms.currentGameRoom,
                  ConstKeys.PRIOR to cameraManagerForRooms.priorGameRoom)))

      // TODO: set megaman's body to transition interpolation, either here or in player
    }

    // set continue transition logic for camera manager
    cameraManagerForRooms.continueTransition = { _ ->
      if (cameraManagerForRooms.delayJustFinished) {
        systems.get(AnimationsSystem::class.simpleName)?.on = true
      }

      game.eventsMan.submitEvent(
          Event(
              EventType.CONTINUE_ROOM_TRANS,
              props(ConstKeys.POSITION to cameraManagerForRooms.transitionInterpolation)))

      // TODO: set megaman's body to transition interpolation, either here or in player
    }

    // set end transition logic for camera manager
    cameraManagerForRooms.endTransition = {
      systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = true } }

      game.eventsMan.submitEvent(
          Event(
              EventType.END_ROOM_TRANS,
              props(ConstKeys.ROOM to cameraManagerForRooms.currentGameRoom)))

      if (cameraManagerForRooms.currentGameRoom?.name.equals(ConstKeys.BOSS)) {
        game.eventsMan.submitEvent(
            Event(
                EventType.ENTER_BOSS_ROOM,
                props(ConstKeys.ROOM to cameraManagerForRooms.currentGameRoom)))
      }
    }
  }

  /**
   * This method shows this level screen. If the screen has not yet been initialized, then [init] is
   * called. It is called when the level screen is shown. It sets the level music, sets the camera
   * positions, sets the systems to on, and sets the world graph map in the game properties. If the
   * tiled map load result is null, it throws an illegal state exception. It also sets the world
   * graph map in the game properties and throws an illegal state exception if it is null.
   */
  override fun show() {
    // lazily initialize this level screen
    if (!initialized) init()

    super.show()
    // add this screen as an event listener
    game.eventsMan.addListener(this)
    // start playing the level music
    music?.let { game.audioMan.playMusic(it, true) }

    // reset positions of cameras
    uiCamera.position.set(com.megaman.maverick.game.ConstFuncs.getCamInitPos())
    gameCamera.position.set(com.megaman.maverick.game.ConstFuncs.getCamInitPos())

    // set all systems to on
    game.gameEngine.systems.forEach { it.on = true }

    // set the world graph map using the tiled map load result
    tiledMapLoadResult?.let {
      val depth =
          (it.worldWidth / ConstVals.VIEW_WIDTH)
              .coerceAtLeast(it.worldHeight / ConstVals.VIEW_HEIGHT)
              .toInt()

      val worldGraphMap =
          QuadTreeGraphMap(
              0, 0, it.worldWidth, it.worldHeight, ConstVals.PPM, depth)

      properties.put(ConstKeys.WORLD_GRAPH_MAP, worldGraphMap)
    } ?: throw IllegalStateException("No tiled map load result found in game props")
  }

  override fun getLayerBuilders() =
      MegaMapLayerBuilders(MegaMapLayerBuildersParams(game, spawnsMan))

  @Suppress("UNCHECKED_CAST")
  override fun buildLevel(result: Properties) {
    // set the backgrounds array
    backgrounds = result.get(ConstKeys.BACKGROUNDS) as Array<Background>

    // set the player spawns
    val playerSpawns =
        result.get("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}") as Array<RectangleMapObject>
    playerSpawnsMan.set(playerSpawns)

    // set the game rooms for the camera manager
    cameraManagerForRooms.gameRooms = result.get(ConstKeys.GAME_ROOMS) as Array<RectangleMapObject>
  }

  override fun onEvent(event: Event) {
    when (event.key) {
      EventType.GAME_PAUSE -> game.pause()
      EventType.GAME_RESUME -> game.resume()
      EventType.PLAYER_SPAWN -> {
        cameraManagerForRooms.reset()
        // TODO: spawn megaman
      }
      EventType.PLAYER_JUST_DIED -> {
        game.audioMan.stopAllMusic()
        // TODO: init player death event handler
      }
      EventType.PLAYER_DONE_DYIN -> {
        music?.let { game.audioMan.playMusic(it, true) }
        // TODO: init player spawn event handler
      }
      // TODO: add health to megaman here?
      // TODO: add heart tank to megaman here?
      EventType.GATE_INIT_OPENING -> {
        // TODO: init gate opening event handler
      }
      EventType.NEXT_ROOM_REQ -> {
        // TODO: init next room request event handler
      }
      EventType.GATE_INIT_CLOSING -> {
        // TODO: init gate closing event handler
      }
      EventType.REQ_SHAKE_CAM -> {
        // TODO: request camera shake
      }
      else -> {}
    }
  }

  override fun render(delta: Float) {
    // game can only be paused if neither spawn nor death event handlers are running
    if (game.controllerPoller.getButtonStatus(ConstKeys.START) == ButtonStatus.JUST_PRESSED
    /* && player spawn and death event handlers are finished */ ) {
      if (game.paused) {
        game.resume()
      } else {
        game.pause()
      }
    }

    // illegal for game to be paused when spawn or death event handlers are running
    // force game resume
    /*
    if (game.paused /* && player spawn or death event handlers are running */) {
      game.resume()
    }
    */

    // things to run only when game is NOT paused
    if (!game.paused) {
      // update backgrounds
      backgrounds.forEach { it.update(delta) }

      // update the camera manager for rooms
      cameraManagerForRooms.update(delta)

      // spawns do not update when player is first spawning if there is a room transition underway
      /*
      if (playerSpawnEventHandler.isFinished() && !levelCamMan.isTransitioning()) {
              playerSpawnMan.run();
              spawnMan.update(delta);
      }
      */

      // only update one handler at a time
      /*
      if (!playerSpawnEventHandler.isFinished()) {
          playerSpawnEventHandler.update(delta)
      } else if (!playerDeathEventHandler.isFinished()) {
          playerDeathEventHandler.update(delta)
      } else if (!playerStatsHandler.isFinished()) {
          playerStatsHandler.update(delta)
      }
       */
    }

    // update the game engine
    game.gameEngine.update(delta)

    // render the level
    val batch = game.batch
    batch.projectionMatrix = gameCamera.combined
    batch.begin()

    backgrounds.forEach { it.spriteMatrix.draw(batch) }
    tiledMapLevelRenderer?.render()

    sprites.forEach { it.draw(batch) }
    sprites.clear()

    batch.end()

    // render the ui
    batch.projectionMatrix = uiCamera.combined
    batch.begin()
    // TODO: render ui
    batch.end()

    // render the shapes
    val shapeRenderer = game.shapeRenderer
    shapeRenderer.projectionMatrix = gameCamera.combined

    shapeRenderer.begin()
    shapes.forEach { it.draw(shapeRenderer) }
    shapes.clear()
    shapeRenderer.end()
  }

  override fun hide() = dispose()

  override fun dispose() {
    super.dispose()
    game.gameEngine.reset()
    game.audioMan.stopAllMusic()
    game.eventsMan.removeListener(this)
    spawnsMan.reset()
    playerSpawnsMan.reset()
  }

  override fun pause() = levelStateHandler.pause()

  override fun resume() = levelStateHandler.resume()
}