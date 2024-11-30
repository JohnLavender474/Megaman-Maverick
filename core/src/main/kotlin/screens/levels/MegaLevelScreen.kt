package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.GameEngine
import com.mega.game.engine.animations.AnimationsSystem
import com.mega.game.engine.behaviors.BehaviorsSystem
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.polling.IControllerPoller
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable
import com.mega.game.engine.drawables.sprites.SpritesSystem
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.EventsManager
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionSystem
import com.mega.game.engine.pathfinding.AsyncPathfindingSystem
import com.mega.game.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.mega.game.engine.world.WorldSystem
import com.mega.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.drawables.backgrounds.Background
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.CameraManagerForRooms
import com.megaman.maverick.game.screens.levels.camera.CameraShaker
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.screens.levels.events.BossSpawnEventHandler
import com.megaman.maverick.game.screens.levels.events.EndLevelEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerDeathEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerSpawnEventHandler
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuilders
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuildersParams
import com.megaman.maverick.game.screens.levels.spawns.PlayerSpawnsManager
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler
import com.megaman.maverick.game.screens.levels.stats.PlayerStatsHandler
import com.megaman.maverick.game.spawns.ISpawner
import com.megaman.maverick.game.spawns.Spawn
import com.megaman.maverick.game.spawns.SpawnsManager
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.world.body.getCenter
import java.util.*

class MegaLevelScreen(
    private val game: MegamanMaverickGame,
    private val onEscapePressed: () -> Unit = {},
) : TiledMapLevelScreen(game.batch), Initializable, IEventListener, Resettable {

    companion object {
        const val TAG = "MegaLevelScreen"
        const val MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG = "MegaLevelScreenEventListener"
        private const val ROOM_DISTANCE_ON_TRANSITION = 2f
        private const val TRANSITION_SCANNER_SIZE = 5f
        private const val FADE_OUT_MUSIC_ON_BOSS_SPAWN = 1f
        private const val DEBUG_PRINT_DELAY = 2f
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.GAME_PAUSE,
        EventType.GAME_RESUME,

        EventType.PLAYER_SPAWN,
        EventType.PLAYER_READY,
        EventType.PLAYER_JUST_DIED,
        EventType.PLAYER_DONE_DYIN,

        EventType.ADD_PLAYER_HEALTH,
        EventType.ADD_HEART_TANK,

        EventType.NEXT_ROOM_REQ,
        EventType.GATE_INIT_OPENING,
        EventType.GATE_INIT_CLOSING,

        EventType.ENTER_BOSS_ROOM,
        EventType.BOSS_READY,
        EventType.END_BOSS_SPAWN,
        EventType.BOSS_DEFEATED,
        EventType.BOSS_DEAD,
        EventType.MINI_BOSS_DEAD,

        EventType.VICTORY_EVENT,
        EventType.END_LEVEL,

        EventType.EDIT_TILED_MAP,

        EventType.SHOW_BACKGROUNDS,
        EventType.HIDE_BACKGROUNDS,

        EventType.SET_GAME_CAM_ROTATION,
        EventType.SHAKE_CAM,
    )

    val engine: GameEngine
        get() = game.engine
    val megaman: Megaman
        get() = game.megaman
    val eventsMan: EventsManager
        get() = game.eventsMan
    val audioMan: MegaAudioManager
        get() = game.audioMan
    val controllerPoller: IControllerPoller
        get() = game.controllerPoller
    var level: Level? = null
    var music: MusicAsset? = null

    private lateinit var spawnsMan: SpawnsManager
    private lateinit var playerSpawnsMan: PlayerSpawnsManager

    private lateinit var playerStatsHandler: PlayerStatsHandler
    private lateinit var bossHealthHandler: BossHealthHandler

    private lateinit var levelStateHandler: LevelStateHandler

    private lateinit var endLevelEventHandler: EndLevelEventHandler
    private lateinit var playerSpawnEventHandler: PlayerSpawnEventHandler
    private lateinit var playerDeathEventHandler: PlayerDeathEventHandler
    private lateinit var bossSpawnEventHandler: BossSpawnEventHandler

    private lateinit var drawables: ObjectMap<DrawingSection, PriorityQueue<IComparableDrawable<Batch>>>
    private lateinit var shapes: Array<IDrawableShape>
    private lateinit var backgrounds: Array<Background>
    private lateinit var backgroundsToHide: ObjectSet<String>

    private lateinit var gameCamera: RotatableCamera
    private lateinit var cameraManagerForRooms: CameraManagerForRooms
    private lateinit var gameCameraShaker: CameraShaker
    private lateinit var uiCamera: OrthographicCamera

    private val disposables = Array<Disposable>()

    private val gameCameraPriorPosition = Vector3()
    private var camerasSetToGameCamera = false

    private val spawns = Array<Spawn>()
    private val debugPrintTimer = Timer(DEBUG_PRINT_DELAY)

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        levelStateHandler = LevelStateHandler(game)
        endLevelEventHandler = EndLevelEventHandler(game)

        drawables = game.getDrawables()
        shapes = game.getShapes()

        gameCamera = game.getGameCamera()
        uiCamera = game.getUiCamera()

        spawnsMan = SpawnsManager()
        playerSpawnsMan = PlayerSpawnsManager(gameCamera)

        playerSpawnEventHandler = PlayerSpawnEventHandler(game)
        playerDeathEventHandler = PlayerDeathEventHandler(game)
        bossSpawnEventHandler = BossSpawnEventHandler(game)

        playerStatsHandler = PlayerStatsHandler(megaman)
        playerStatsHandler.init()

        bossHealthHandler = BossHealthHandler(game)

        cameraManagerForRooms = CameraManagerForRooms(
            gameCamera,
            distanceOnTransition = ROOM_DISTANCE_ON_TRANSITION * ConstVals.PPM,
            transitionScannerDimensions = Vector2(
                TRANSITION_SCANNER_SIZE * ConstVals.PPM,
                TRANSITION_SCANNER_SIZE * ConstVals.PPM
            ),
            transDelay = ConstVals.ROOM_TRANS_DELAY_DURATION,
            transDuration = ConstVals.ROOM_TRANS_DURATION,
        )
        cameraManagerForRooms.focus = megaman
        cameraManagerForRooms.beginTransition = {
            GameLogger.debug(TAG, "Begin transition logic for camera manager")

            val room = cameraManagerForRooms.currentGameRoom

            eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
            eventsMan.submitEvent(
                Event(
                    EventType.BEGIN_ROOM_TRANS, props(
                        ConstKeys.ROOM pairTo room,
                        ConstKeys.PRIOR pairTo cameraManagerForRooms.priorGameRoom,
                        ConstKeys.POSITION pairTo cameraManagerForRooms.transitionInterpolation,
                    )
                )
            )

            MegaGameEntities.getEntitiesOfType(EntityType.ENEMY).forEach { it.destroy() }

            game.getSystem(BehaviorsSystem::class).on = false
            game.putProperty(ConstKeys.ROOM_TRANSITION, true)

            if (room?.name == ConstKeys.BOSS_ROOM) audioMan.fadeOutMusic(FADE_OUT_MUSIC_ON_BOSS_SPAWN)
        }
        cameraManagerForRooms.continueTransition = { _ ->
            if (cameraManagerForRooms.delayJustFinished) game.getSystem(AnimationsSystem::class).on = true

            eventsMan.submitEvent(
                Event(
                    EventType.CONTINUE_ROOM_TRANS,
                    props(
                        ConstKeys.ROOM pairTo cameraManagerForRooms.currentGameRoom,
                        ConstKeys.PRIOR pairTo cameraManagerForRooms.priorGameRoom,
                        ConstKeys.POSITION pairTo cameraManagerForRooms.transitionInterpolation
                    )
                )
            )
        }
        cameraManagerForRooms.endTransition = {
            GameLogger.debug(TAG, "End transition logic for camera manager")

            val currentRoom = cameraManagerForRooms.currentGameRoom
            val hasEvent = currentRoom?.properties?.containsKey(ConstKeys.EVENT) == true
            val event = currentRoom?.properties?.get(ConstKeys.EVENT, String::class.java)

            eventsMan.submitEvent(
                Event(
                    EventType.END_ROOM_TRANS, props(
                        ConstKeys.ROOM pairTo cameraManagerForRooms.currentGameRoom,
                        ConstKeys.PRIOR pairTo cameraManagerForRooms.priorGameRoom,
                        ConstKeys.VELOCITY pairTo !(hasEvent && event == ConstKeys.BOSS)
                    )
                )
            )

            if (hasEvent) {
                val props = props(ConstKeys.ROOM pairTo currentRoom)
                val roomEvent = when (event) {
                    ConstKeys.BOSS -> EventType.ENTER_BOSS_ROOM
                    ConstKeys.SUCCESS -> EventType.VICTORY_EVENT
                    else -> throw IllegalStateException("Unknown room event: $event")
                }
                eventsMan.submitEvent(Event(roomEvent, props))
            } else eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))

            game.getSystem(BehaviorsSystem::class).on = true

            game.putProperty(ConstKeys.ROOM_TRANSITION, false)
        }
        cameraManagerForRooms.onSetToRoomNoTrans = {
            GameLogger.debug(TAG, "On set to room no trans")
            eventsMan.submitEvent(
                Event(
                    EventType.SET_TO_ROOM_NO_TRANS, props(ConstKeys.ROOM pairTo cameraManagerForRooms.currentGameRoom)
                )
            )
        }

        gameCameraShaker = CameraShaker(gameCamera)
    }

    override fun show() {
        EntityFactories.init()
        if (!initialized) init()
        super.show()

        eventsMan.addListener(this)
        engine.systems.forEach { it.on = true }
        music?.let { audioMan.playMusic(it, true) }

        game.setCameraRotating(false)
        game.setRoomsSupplier { cameraManagerForRooms.gameRooms }
        game.setCurrentRoomSupplier { cameraManagerForRooms.currentGameRoom }

        if (tiledMapLoadResult == null) throw IllegalStateException("No tiled map load result found in level screen")
        game.setTiledMapLoadResult(tiledMapLoadResult!!)

        val worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        game.setWorldContainer(worldContainer)

        playerSpawnEventHandler.init()
        playerDeathEventHandler.reset()
        endLevelEventHandler.reset()

        camerasSetToGameCamera = false
        gameCameraPriorPosition.setZero()
        gameCamera.position.set(ConstFuncs.getCamInitPos())
        uiCamera.position.set(ConstFuncs.getCamInitPos())
    }

    override fun getLayerBuilders() = MegaMapLayerBuilders(MegaMapLayerBuildersParams(game, spawnsMan))

    override fun buildLevel(result: Properties) {
        backgrounds = result.get(ConstKeys.BACKGROUNDS) as Array<Background>
        backgroundsToHide = result.get("${ConstKeys.HIDDEN}_${ConstKeys.BACKGROUNDS}") as ObjectSet<String>

        val playerSpawns =
            result.get("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}") as Array<RectangleMapObject>? ?: Array()
        playerSpawnsMan.set(playerSpawns)

        cameraManagerForRooms.gameRooms = result.get(ConstKeys.GAME_ROOMS) as Array<RectangleMapObject>? ?: Array()

        val spawners = result.get(ConstKeys.SPAWNERS) as Array<ISpawner>? ?: Array()
        spawnsMan.setSpawners(spawners)

        val disposables = result.get(ConstKeys.DISPOSABLES) as Array<Disposable>? ?: Array()
        this.disposables.addAll(disposables)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.GAME_PAUSE -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Game pause --> pause the game"
                )

                game.pause()
            }

            EventType.GAME_RESUME -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Game resume --> resume the game"
                )

                game.resume()
            }

            EventType.PLAYER_SPAWN -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Player spawn --> reset camera manager for rooms and spawn megaman"
                )

                backgrounds.forEach { background -> background.immediateRotation(Direction.UP) }
                cameraManagerForRooms.reset()
                // gameCamera.immediateRotation(Direction.UP)

                bossHealthHandler.unset()

                engine.systems.forEach { it.on = true }
                game.putProperty(ConstKeys.ROOM_TRANSITION, false)

                MegaGameEntities.getEntitiesOfType(EntityType.ENEMY).forEach { game.engine.destroy(it) }

                GameLogger.debug(
                    TAG, "onEvent(): Player spawn --> spawn Megaman: ${playerSpawnsMan.currentSpawnProps!!}"
                )
                megaman.spawn(playerSpawnsMan.currentSpawnProps!!)
            }

            EventType.PLAYER_READY -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Player ready")
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            }

            EventType.PLAYER_JUST_DIED -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Player just died --> init death handler"
                )

                audioMan.unsetMusic()
                audioMan.stopAllLoopingSounds()

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

            EventType.NEXT_ROOM_REQ -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Next room req --> start room transition"
                )

                val roomName = event.properties.get(ConstKeys.ROOM) as String
                val isTrans = cameraManagerForRooms.transitionToRoom(roomName)
                if (isTrans) GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Next room req --> successfully starting transition to room: $roomName"
                ) else GameLogger.error(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): Next room req --> could not start transition to room: $roomName"
                )
            }

            EventType.GATE_INIT_OPENING -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Gate init opening --> start room transition"
                )

                val systemsToTurnOff = gdxArrayOf(
                    AsyncPathfindingSystem::class,
                    MotionSystem::class,
                    BehaviorsSystem::class,
                    WorldSystem::class,
                )
                systemsToTurnOff.forEach { game.getSystem(it).on = false }

                megaman.body.physics.velocity.setZero()

                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
            }

            EventType.GATE_INIT_CLOSING -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Gate init closing")

                val systemsToTurnOn = gdxArrayOf(
                    AsyncPathfindingSystem::class,
                    MotionSystem::class,
                    BehaviorsSystem::class,
                    WorldSystem::class
                )
                systemsToTurnOn.forEach { game.getSystem(it).on = true }

                val roomName = cameraManagerForRooms.currentGameRoom?.name
                if (roomName != null && roomName != ConstKeys.BOSS_ROOM && !roomName.contains(ConstKeys.MINI))
                    eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            }

            EventType.ENTER_BOSS_ROOM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Enter boss room")

                val bossRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!
                val bossMapObject = bossRoom.properties.get(ConstKeys.OBJECT, RectangleMapObject::class.java)
                val bossName = bossMapObject.name

                val bossSpawnProps = bossMapObject.properties.toProps()
                bossSpawnProps.put(ConstKeys.BOUNDS, bossMapObject.rectangle.toGameRectangle())
                bossSpawnEventHandler.init(bossName, bossSpawnProps)

                megaman.running = false
            }

            EventType.BOSS_READY -> {
                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                if (boss.mini) game.eventsMan.submitEvent(Event(EventType.END_BOSS_SPAWN))
                else {
                    val runOnFirstUpdate = { game.audioMan.pauseMusic() }
                    val runOnFinished = {
                        game.eventsMan.submitEvent(Event(EventType.END_BOSS_SPAWN))
                        game.audioMan.playMusic()
                    }
                    bossHealthHandler.set(boss, runOnFirstUpdate, runOnFinished)

                    game.engine.systems.forEach {
                        if (!it.isAny(SpritesSystem::class, AnimationsSystem::class)) it.on = false
                    }
                }
            }

            EventType.END_BOSS_SPAWN -> {
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))

                game.engine.systems.forEach { it.on = true }
            }

            EventType.BOSS_DEFEATED -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Boss defeated")

                if (megaman.dead) {
                    GameLogger.debug(TAG, "onEvent(): Megaman dead when boss defeated, doing nothing...")
                    return
                }

                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                if (!boss.mini) audioMan.unsetMusic()

                MegaGameEntities.forEachEntity {
                    if (it.isAny(IDamager::class, IHazard::class) && it != boss) it.destroy()
                }

                eventsMan.submitEvent(
                    Event(
                        EventType.TURN_CONTROLLER_OFF, props(
                            "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}" pairTo false
                        )
                    )
                )
                megaman.canBeDamaged = false
                bossHealthHandler.unset()
            }

            EventType.BOSS_DEAD -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Boss dead")

                if (megaman.dead) {
                    GameLogger.debug(TAG, "onEvent(): Megaman dead when boss dead, doing nothing...")
                    return
                }

                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                val eventType = if (boss.mini) EventType.MINI_BOSS_DEAD else EventType.VICTORY_EVENT
                eventsMan.submitEvent(Event(eventType, props(ConstKeys.BOSS pairTo boss)))
            }

            EventType.MINI_BOSS_DEAD -> {
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
                megaman.canBeDamaged = true
            }

            EventType.VICTORY_EVENT -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Victory event")
                endLevelEventHandler.init()
            }

            EventType.SHAKE_CAM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Req shake cam")
                if (gameCameraShaker.isFinished) {
                    val duration = event.properties.get(ConstKeys.DURATION, Float::class)!!
                    val interval = event.properties.get(ConstKeys.INTERVAL, Float::class)!!
                    val shakeX = event.properties.get(ConstKeys.X, Float::class)!!
                    val shakeY = event.properties.get(ConstKeys.Y, Float::class)!!
                    gameCameraShaker.startShake(duration, interval, shakeX, shakeY)
                }
            }

            EventType.END_LEVEL -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): End level")
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
                val nextScreen = LevelCompletionMap.getNextScreen(level!!, game.params.startScreen)
                game.setCurrentScreen(nextScreen.name)
            }

            EventType.EDIT_TILED_MAP -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Edit tiled map")
                val editor = event.getProperty(ConstKeys.EDIT)!! as (TiledMap) -> Unit
                tiledMap?.let {
                    GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Invoking editor")
                    editor.invoke(it)
                }
            }

            EventType.SHOW_BACKGROUNDS -> {
                val backgroundKeys = event.getProperty(ConstKeys.BACKGROUNDS, String::class)!!.split(",")
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): show backgrounds: $backgroundKeys")
                backgroundKeys.forEach { backgroundsToHide.remove(it) }
            }

            EventType.HIDE_BACKGROUNDS -> {
                val backgroundKeys = event.getProperty(ConstKeys.BACKGROUNDS, String::class)!!.split(",")
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): hide backgrounds: $backgroundKeys")
                backgroundKeys.forEach { backgroundsToHide.add(it) }
            }

            EventType.SET_GAME_CAM_ROTATION -> {
                val direction = event.getProperty(ConstKeys.DIRECTION, Direction::class)!!
                gameCamera.startRotation(direction, ConstVals.GAME_CAM_ROTATE_TIME)
                backgrounds.forEach { background ->
                    background.startRotation(
                        direction,
                        ConstVals.GAME_CAM_ROTATE_TIME
                    )
                }
                game.setCameraRotating(true)
            }
        }
    }

    override fun render(delta: Float) {
        if (controllerPoller.isJustPressed(MegaControllerButton.START) &&
            playerStatsHandler.finished &&
            playerSpawnEventHandler.finished &&
            playerDeathEventHandler.finished
        ) {
            if (game.paused) game.resume() else game.pause()
        }

        if (game.paused && (
                !playerStatsHandler.finished ||
                    !playerSpawnEventHandler.finished ||
                    !playerDeathEventHandler.finished ||
                    !bossHealthHandler.finished
                )
        ) game.resume()

        if (!game.paused) {
            spawnsMan.update(delta / 2f)
            spawns.addAll(spawnsMan.getSpawnsAndClear())
            if (playerSpawnEventHandler.finished && !cameraManagerForRooms.transitioning) {
                spawns.forEach { spawn -> engine.spawn(spawn.entity, spawn.properties) }
                spawns.clear()
            }
        }

        engine.update(delta)

        if (!game.paused) {
            spawnsMan.update(delta / 2f)
            spawns.addAll(spawnsMan.getSpawnsAndClear())
            if (/* TODO: playerSpawnEventHandler.finished && */ !cameraManagerForRooms.transitioning) {
                playerSpawnsMan.run()
                spawns.forEach { spawn -> engine.spawn(spawn.entity, spawn.properties) }
                spawns.clear()
            }

            backgrounds.forEach { it.update(delta) }
            cameraManagerForRooms.update(delta)
            gameCamera.update(delta)

            if (!gameCameraShaker.isFinished) gameCameraShaker.update(delta)
            when {
                !playerSpawnEventHandler.finished -> playerSpawnEventHandler.update(delta)
                !playerDeathEventHandler.finished -> playerDeathEventHandler.update(delta)
                !bossHealthHandler.finished -> bossHealthHandler.update(delta)
                !endLevelEventHandler.finished -> endLevelEventHandler.update(delta)
            }
            playerStatsHandler.update(delta)
        }

        val gameCamDeltaX = gameCamera.position.x - gameCameraPriorPosition.x
        val gameCamDeltaY = gameCamera.position.y - gameCameraPriorPosition.y
        backgrounds.forEach { it.move(gameCamDeltaX, gameCamDeltaY) }
        gameCameraPriorPosition.set(gameCamera.position)

        val batch = game.batch
        batch.begin()

        backgrounds.sort()
        backgrounds.forEach { if (!backgroundsToHide.contains(it.key)) it.draw(batch) }

        game.viewports.get(ConstKeys.GAME).apply()
        batch.projectionMatrix = gameCamera.combined
        val backgroundSprites = drawables.get(DrawingSection.BACKGROUND)
        while (!backgroundSprites.isEmpty()) {
            val backgroundSprite = backgroundSprites.poll()
            backgroundSprite.draw(batch)
        }
        tiledMapLevelRenderer?.render(gameCamera)
        val gameGroundSprites = drawables.get(DrawingSection.PLAYGROUND)
        while (!gameGroundSprites.isEmpty()) {
            val gameGroundSprite = gameGroundSprites.poll()
            gameGroundSprite.draw(batch)
        }
        val foregroundSprites = drawables.get(DrawingSection.FOREGROUND)
        while (!foregroundSprites.isEmpty()) {
            val foregroundSprite = foregroundSprites.poll()
            foregroundSprite.draw(batch)
        }

        game.viewports.get(ConstKeys.UI).apply()
        batch.projectionMatrix = uiCamera.combined
        bossHealthHandler.draw(batch)
        playerStatsHandler.draw(batch)
        batch.end()

        when {
            !playerSpawnEventHandler.finished -> playerSpawnEventHandler.draw(batch)
            !endLevelEventHandler.finished -> endLevelEventHandler.draw(batch)
        }

        val shapeRenderer = game.shapeRenderer
        shapeRenderer.projectionMatrix = gameCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        while (!shapes.isEmpty) {
            val shape = shapes.pop()
            shape.draw(shapeRenderer)
        }
        if (game.params.debugShapes) {
            val gameCamBounds = gameCamera.getRotatedBounds()
            gameCamBounds.translate(0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM)
            gameCamBounds.translateSize(-0.2f * ConstVals.PPM, -0.2f * ConstVals.PPM)
            shapeRenderer.color = Color.BLUE
            shapeRenderer.set(ShapeRenderer.ShapeType.Line)
            shapeRenderer.rect(
                gameCamBounds.getX(),
                gameCamBounds.getY(),
                gameCamBounds.getWidth(),
                gameCamBounds.getHeight()
            )
        }
        shapeRenderer.end()

        debugPrintTimer.update(delta)
        if (debugPrintTimer.isFinished()) {
            debugPrintTimer.reset()
            GameLogger.debug(TAG, "Game cam rotated bounds: ${gameCamera.getRotatedBounds()}")
            GameLogger.debug(TAG, "Megaman center: ${megaman.body.getCenter()}")
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) onEscapePressed.invoke()
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset(): Resetting level screen")

        eventsMan.removeListener(this)

        EntityFactories.clear()
        engine.reset()

        spawns.clear()
        spawnsMan.reset()
        playerSpawnsMan.reset()

        playerSpawnEventHandler.reset()
        playerDeathEventHandler.reset()
        endLevelEventHandler.reset()

        backgrounds.forEach { background -> background.reset() }

        cameraManagerForRooms.reset()
        gameCamera.immediateRotation(Direction.UP)

        audioMan.unsetMusic()

        game.putProperty(ConstKeys.ROOM_TRANSITION, false)
        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
    }

    override fun dispose() {
        GameLogger.debug(TAG, "dispose(): Disposing level screen")
        super.dispose()

        disposables.forEach { it.dispose() }
        disposables.clear()
    }

    override fun pause() {
        audioMan.pauseMusic()
        levelStateHandler.pause()
    }

    override fun resume() {
        audioMan.playMusic()
        levelStateHandler.resume()
    }

    override fun resize(width: Int, height: Int) =
        backgrounds.forEach { background -> background.updateViewportSize(width, height) }
}
