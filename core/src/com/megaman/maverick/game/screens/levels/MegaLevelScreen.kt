@file:Suppress("UNCHECKED_CAST")

package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector3
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
import com.engine.controller.polling.IControllerPoller
import com.engine.drawables.IDrawable
import com.engine.drawables.shapes.IDrawableShape
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
import com.megaman.maverick.game.*
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.drawables.sprites.Background
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.CameraManagerForRooms
import com.megaman.maverick.game.screens.levels.events.EndLevelEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerDeathEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerSpawnEventHandler
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuilders
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuildersParams
import com.megaman.maverick.game.screens.levels.spawns.PlayerSpawnsManager
import com.megaman.maverick.game.screens.levels.stats.PlayerStatsHandler
import com.megaman.maverick.game.utils.toProps
import java.util.*

class MegaLevelScreen(game: MegamanMaverickGame) :
    TiledMapLevelScreen(game, props()), Initializable {

    companion object {
        const val TAG = "MegaLevelScreen"
        const val MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG = "MegaLevelScreenEventListener"
        private const val INTERPOLATE_GAME_CAM = true
        private const val DEFAULT_BACKGROUND_PARALLAX_FACTOR = 0.5f
        private const val DEFAULT_FOREGROUND_PARALLAX_FACTOR = 2f
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
    private lateinit var playerStatsHandler: PlayerStatsHandler

    private lateinit var levelStateHandler: LevelStateHandler
    private lateinit var endLevelEventHandler: EndLevelEventHandler
    private lateinit var cameraManagerForRooms: CameraManagerForRooms

    private lateinit var playerSpawnEventHandler: PlayerSpawnEventHandler
    private lateinit var playerDeathEventHandler: PlayerDeathEventHandler

    private lateinit var drawables: MutableCollection<IDrawable<Batch>>
    private lateinit var shapes: PriorityQueue<IDrawableShape>
    private lateinit var backgrounds: Array<Background>

    private lateinit var backgroundCamera: OrthographicCamera
    private lateinit var gameCamera: OrthographicCamera
    private lateinit var foregroundCamera: OrthographicCamera
    private lateinit var uiCamera: OrthographicCamera

    private lateinit var disposables: Array<Disposable>

    private var initialized = false

    private val gameCameraPriorPosition = Vector3()
    private var backgroundParallaxFactor = DEFAULT_BACKGROUND_PARALLAX_FACTOR
    private var foregroundParallaxFactor = DEFAULT_FOREGROUND_PARALLAX_FACTOR
    private var camerasSetToGameCamera = false

    override fun init() {
        if (initialized) return
        initialized = true

        disposables = Array()

        spawnsMan = SpawnsManager()
        levelStateHandler = LevelStateHandler(megamanGame)
        endLevelEventHandler = EndLevelEventHandler(megamanGame)

        drawables = megamanGame.getDrawables()
        shapes = megamanGame.getShapes()

        backgroundCamera = megamanGame.getBackgroundCamera()
        gameCamera = megamanGame.getGameCamera()
        foregroundCamera = megamanGame.getForegroundCamera()
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
                AudioSystem::class
            )

        cameraManagerForRooms = CameraManagerForRooms(gameCamera)
        cameraManagerForRooms.interpolate = INTERPOLATE_GAME_CAM
        cameraManagerForRooms.interpolationScalar = 5f
        cameraManagerForRooms.focus = megaman

        // set begin transition logic for camera manager
        cameraManagerForRooms.beginTransition = {
            GameLogger.debug(TAG, "Begin transition logic for camera manager")
            systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = false } }
            megamanGame.eventsMan.submitEvent(
                Event(
                    EventType.BEGIN_ROOM_TRANS,
                    props(
                        ConstKeys.POSITION to cameraManagerForRooms.transitionInterpolation,
                        ConstKeys.CURRENT to cameraManagerForRooms.currentGameRoom,
                        ConstKeys.PRIOR to cameraManagerForRooms.priorGameRoom
                    )
                )
            )
        }

        // set continue transition logic for camera manager
        cameraManagerForRooms.continueTransition = { _ ->
            if (cameraManagerForRooms.delayJustFinished)
                systems.get(AnimationsSystem::class.simpleName)?.on = true

            megamanGame.eventsMan.submitEvent(
                Event(
                    EventType.CONTINUE_ROOM_TRANS,
                    props(ConstKeys.POSITION to cameraManagerForRooms.transitionInterpolation)
                )
            )
        }

        // set end transition logic for camera manager
        cameraManagerForRooms.endTransition = {
            GameLogger.debug(TAG, "End transition logic for camera manager")
            systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = true } }

            eventsMan.submitEvent(
                Event(
                    EventType.END_ROOM_TRANS,
                    props(ConstKeys.ROOM to cameraManagerForRooms.currentGameRoom)
                )
            )

            val currentRoom = cameraManagerForRooms.currentGameRoom
            if (currentRoom?.properties?.containsKey(ConstKeys.EVENT) == true) {
                val props = props(ConstKeys.ROOM to currentRoom)

                val roomEvent =
                    when (val roomEventString =
                        currentRoom.properties.get(ConstKeys.EVENT, String::class.java)) {
                        ConstKeys.BOSS -> EventType.ENTER_BOSS_ROOM
                        ConstKeys.SUCCESS -> EventType.END_LEVEL_SUCCESSFULLY
                        else -> throw IllegalStateException("Unknown room event: $roomEventString")
                    }

                eventsMan.submitEvent(Event(roomEvent, props))
            }
        }
    }

    override fun show() {
        // lazily initialize this level screen
        if (!initialized) init()

        dispose()
        super.show()

        // add this screen as an event listener
        eventsMan.addListener(this)

        // set all systems to on
        engine.systems.forEach { it.on = true }

        // start playing the level music
        music?.let { audioMan.playMusic(it, true) }

        // set the world graph map using the tiled map load result
        if (tiledMapLoadResult == null)
            throw IllegalStateException("No tiled map load result found in level screen")
        val (map, _, worldWidth, worldHeight) = tiledMapLoadResult!!

        // TODO: should use quad tree graph map instead of simple node graph map
        /*
        val depth = (worldWidth).coerceAtLeast(worldHeight) / ConstVals.PPM
        val worldGraphMap = QuadTreeGraphMap(0, 0, worldWidth, worldHeight, ConstVals.PPM, depth)
         */
        val worldGraphMap = SimpleNodeGraphMap(0, 0, worldWidth, worldHeight, ConstVals.PPM)
        megamanGame.setGraphMap(worldGraphMap)

        playerSpawnEventHandler.init()

        // set the parallax factor for background and foreground cameras
        val mapProps = map.properties.toProps()
        backgroundParallaxFactor = mapProps.getOrDefault(
            ConstKeys.BACKGROUND_PARALLAX_FACTOR,
            DEFAULT_BACKGROUND_PARALLAX_FACTOR, Float::class
        )
        foregroundParallaxFactor = mapProps.getOrDefault(
            ConstKeys.FOREGROUND_PARALLAX_FACTOR,
            DEFAULT_FOREGROUND_PARALLAX_FACTOR, Float::class
        )
        camerasSetToGameCamera = false
    }

    override fun getLayerBuilders() =
        MegaMapLayerBuilders(MegaMapLayerBuildersParams(game as MegamanMaverickGame, spawnsMan))

    override fun buildLevel(result: Properties) {
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

        // reset positions of cameras
        backgroundCamera.position.set(ConstFuncs.getCamInitPos())
        gameCamera.position.set(ConstFuncs.getCamInitPos())
        foregroundCamera.position.set(ConstFuncs.getCamInitPos())
        uiCamera.position.set(ConstFuncs.getCamInitPos())
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.GAME_PAUSE -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Game pause --> pause the game"
                )
                megamanGame.pause()
            }

            EventType.GAME_RESUME -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Game resume --> resume the game"
                )
                megamanGame.resume()
            }

            EventType.PLAYER_SPAWN -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Player spawn --> reset camera manager for rooms and spawn megaman"
                )
                cameraManagerForRooms.reset()
                GameLogger.debug(
                    TAG,
                    "onEvent(): Player spawn --> spawn Megaman: ${playerSpawnsMan.currentSpawnProps!!}"
                )
                megamanGame.gameEngine.spawn(megaman, playerSpawnsMan.currentSpawnProps!!)
            }

            EventType.PLAYER_READY -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Player ready")
            }

            EventType.PLAYER_JUST_DIED -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Player just died --> init death handler"
                )
                audioMan.stopMusic()
                playerDeathEventHandler.init()
            }

            EventType.PLAYER_DONE_DYIN -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Player done dying --> respawn player"
                )
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
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Gate init opening --> start room transition"
                )
                val systemsToTurnOff =
                    gdxArrayOf(
                        ControllerSystem::class.simpleName,
                        MotionSystem::class.simpleName,
                        BehaviorsSystem::class.simpleName,
                        WorldSystem::class.simpleName
                    )
                systemsToTurnOff.forEach { megamanGame.getSystems().get(it).on = false }
                megamanGame.megaman.body.physics.velocity.setZero()
            }

            EventType.NEXT_ROOM_REQ -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Next room req --> start room transition"
                )
                val roomName = event.properties.get(ConstKeys.ROOM) as String
                val isTrans = cameraManagerForRooms.transitionToRoom(roomName)
                if (isTrans)
                    GameLogger.debug(
                        MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                        "onEvent(): Next room req --> successfully starting transition to room: $roomName"
                    )
                else
                    GameLogger.error(
                        MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                        "onEvent(): Next room req --> could not start transition to room: $roomName"
                    )
            }

            EventType.GATE_INIT_CLOSING -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Gate init closing --> start room transition"
                )
                val systemsToTurnOff =
                    gdxArrayOf(
                        ControllerSystem::class.simpleName,
                        MotionSystem::class.simpleName,
                        BehaviorsSystem::class.simpleName,
                        WorldSystem::class.simpleName
                    )
                systemsToTurnOff.forEach { megamanGame.getSystems().get(it).on = true }
            }

            EventType.REQ_SHAKE_CAM -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Req shake cam --> shake the camera"
                )
                // TODO: shake camera
            }

            EventType.END_LEVEL_SUCCESSFULLY -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): End level successfully --> end level"
                )
                endLevelEventHandler.endLevelSuccessfully()
            }
        }
    }

    override fun render(delta: Float) {
        // game can only be paused if neither spawn nor death event handlers are running
        if (controllerPoller.isJustPressed(ControllerButton.START) &&
            playerStatsHandler.finished &&
            playerSpawnEventHandler.finished &&
            playerDeathEventHandler.finished
        )
            if (megamanGame.paused) megamanGame.resume() else megamanGame.pause()

        // illegal for game to be paused when spawn or death event handlers are running
        // force game resume
        if (game.paused &&
            (!playerStatsHandler.finished ||
                    !playerSpawnEventHandler.finished ||
                    !playerDeathEventHandler.finished)
        )
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

            playerStatsHandler.update(delta)
        }

        // update the background and foreground camera positions
        if (camerasSetToGameCamera) {
            val gameCamDeltaX = gameCamera.position.x - gameCameraPriorPosition.x
            backgroundCamera.position.x += gameCamDeltaX * backgroundParallaxFactor
        } else {
            backgroundCamera.position.set(gameCamera.position)
            foregroundCamera.position.set(gameCamera.position)
            camerasSetToGameCamera = true
        }
        gameCameraPriorPosition.set(gameCamera.position)

        // update the game engine
        engine.update(delta)

        // render the level
        val batch = megamanGame.batch
        batch.begin()

        // render backgrounds
        batch.projectionMatrix = backgroundCamera.combined
        backgrounds.forEach { it.draw(batch) }

        // render the game ground
        batch.projectionMatrix = gameCamera.combined
        tiledMapLevelRenderer?.render(gameCamera)
        drawables.forEach { it.draw(batch) }
        drawables.clear()

        // TODO: render foreground

        // render the ui
        batch.projectionMatrix = uiCamera.combined
        playerStatsHandler.draw(batch)
        if (!playerSpawnEventHandler.finished) playerSpawnEventHandler.draw(batch)

        batch.end()

        // render the shapes
        val shapeRenderer = megamanGame.shapeRenderer
        shapeRenderer.projectionMatrix = gameCamera.combined

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        while (!shapes.isEmpty()) {
            val shape = shapes.poll()
            shape.draw(shapeRenderer)
        }
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
