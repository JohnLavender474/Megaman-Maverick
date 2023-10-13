package com.test.game.screens.levels

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.IGame2D
import com.engine.animations.AnimationsSystem
import com.engine.audio.AudioSystem
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.Initializable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.controller.ControllerSystem
import com.engine.events.Event
import com.engine.graph.QuadTreeGraphMap
import com.engine.motion.MotionSystem
import com.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.engine.spawns.SpawnsManager
import com.engine.systems.IGameSystem
import com.engine.updatables.UpdatablesSystem
import com.engine.world.WorldSystem
import com.test.game.ConstFuncs
import com.test.game.ConstKeys
import com.test.game.ConstVals
import com.test.game.drawables.sprites.Background
import com.test.game.events.EventType
import com.test.game.screens.levels.camera.CameraManagerForRooms
import com.test.game.screens.levels.map.MapLayerBuilders
import com.test.game.screens.levels.spawns.PlayerSpawnsManager

class MegaLevelScreen(private val game: IGame2D, override val properties: Properties) :
    TiledMapLevelScreen(game.batch, properties.get(ConstKeys.TMX_SRC, String::class)!!, properties),
    Initializable {

  override val eventKeyMask: ObjectSet<Any> = objectSetOf()

  private lateinit var spawnsMan: SpawnsManager
  private lateinit var levelStateHandler: LevelStateHandler
  private lateinit var backgrounds: Array<Background>
  private lateinit var playerSpawnsMan: PlayerSpawnsManager
  private lateinit var cameraManagerForRooms: CameraManagerForRooms

  private var initialized = false

  override fun init() {
    if (initialized) return
    initialized = true

    spawnsMan = SpawnsManager()
    levelStateHandler = LevelStateHandler(game)
    backgrounds = Array<Background>()

    val camera = game.viewports.get(ConstKeys.GAME).camera
    playerSpawnsMan = PlayerSpawnsManager(camera)

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

    cameraManagerForRooms = CameraManagerForRooms(camera)

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

      // TODO: set megaman's body to transition interpolation, either here or in player class
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

      // TODO: set megaman's body to transition interpolation, either here or in player class
    }

    // set end transition logic for camera manager
    cameraManagerForRooms.endTransition = {
      systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = true } }

      game.eventsMan.submitEvent(
          Event(
              EventType.END_ROOM_TRANS,
              props(ConstKeys.ROOM to cameraManagerForRooms.currentGameRoom)))

      if (cameraManagerForRooms.currentGameRoom?.name.equals(ConstKeys.BOSS)) {
        game.eventsMan.submitEvent(Event(EventType.ENTER_BOSS_ROOM))
      }
    }
  }

  override fun show() {
    if (!initialized) init()
    super.show()
    game.eventsMan.addListener(this)
    properties.get(ConstKeys.LEVEL_MUSIC, String::class)?.let { game.audioMan.playMusic(it, true) }

    val uiCam = game.viewports.get(ConstKeys.UI).camera
    uiCam.position.set(ConstFuncs.getCamInitPos())
    val gameCam = game.viewports.get(ConstKeys.GAME).camera
    gameCam.position.set(ConstFuncs.getCamInitPos())

    game.gameEngine.systems.forEach { it.on = true }

    val depth =
        (tiledMapLoadResult.worldWidth / ConstVals.VIEW_WIDTH)
            .coerceAtLeast(tiledMapLoadResult.worldHeight / ConstVals.VIEW_HEIGHT)
            .toInt()
    val worldGraphMap =
        QuadTreeGraphMap(
            0,
            0,
            tiledMapLoadResult.worldWidth,
            tiledMapLoadResult.worldHeight,
            ConstVals.PPM,
            depth)
    properties.put(ConstKeys.WORLD_GRAPH_MAP, worldGraphMap)
    // TODO: set graph map for pathfinding system
  }

  override fun getLayerBuilders() = MapLayerBuilders(game)

  override fun buildLevel(result: Properties) {
    val _backgrounds = result.get(ConstKeys.BACKGROUNDS, Array::class)
    _backgrounds?.forEach { if (it is Background) backgrounds.add(it) }
  }

  override fun render(delta: Float) {
    TODO("Not yet implemented")
  }

  override fun hide() {
    TODO("Not yet implemented")
  }

  override fun onEvent(event: Event) {
    TODO("Not yet implemented")
  }

  override fun pause() {
    TODO("Not yet implemented")
  }

  override fun resize(width: Int, height: Int) {
    TODO("Not yet implemented")
  }

  override fun resume() {
    TODO("Not yet implemented")
  }
}
