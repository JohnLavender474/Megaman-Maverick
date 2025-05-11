package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
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
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.epsilonEquals
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
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable
import com.mega.game.engine.drawables.sprites.SpritesSystem
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.EventsManager
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionSystem
import com.mega.game.engine.pathfinding.SimplePathfindingSystem
import com.mega.game.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.mega.game.engine.screens.levels.tiledmap.TiledMapLoader
import com.mega.game.engine.world.WorldSystem
import com.mega.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.drawables.backgrounds.Background
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IBossListener
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.camera.CameraManagerForRooms
import com.megaman.maverick.game.screens.levels.camera.CameraShaker
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera
import com.megaman.maverick.game.screens.levels.events.BossSpawnEventHandler
import com.megaman.maverick.game.screens.levels.events.EndLevelEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerDeathEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerSpawnEventHandler
import com.megaman.maverick.game.screens.levels.spawns.PlayerSpawnsManager
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler
import com.megaman.maverick.game.screens.levels.stats.PlayerStatsHandler
import com.megaman.maverick.game.screens.levels.tiled.layers.MegaMapLayerBuilders
import com.megaman.maverick.game.screens.levels.tiled.layers.MegaMapLayerBuildersParams
import com.megaman.maverick.game.screens.menus.level.LevelPauseScreen
import com.megaman.maverick.game.spawns.ISpawner
import com.megaman.maverick.game.spawns.Spawn
import com.megaman.maverick.game.spawns.SpawnsManager
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable
import com.megaman.maverick.game.utils.misc.HealthFillType
import com.megaman.maverick.game.world.body.getCenter
import java.util.*

class MegaLevelScreen(private val game: MegamanMaverickGame) :
    TiledMapLevelScreen(game.batch, TiledMapLoader(game.assMan)), Initializable, IEventListener, IShapeDebuggable,
    Resettable {

    companion object {
        const val TAG = "MegaLevelScreen"
        const val MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG = "MegaLevelScreenEventListener"
        private const val ROOM_DISTANCE_ON_TRANSITION = 2f
        private const val TRANSITION_SCANNER_SIZE = 5f
        private const val FADE_OUT_MUSIC_DUR = 1f
        private const val DISPLAY_ROOMS_DEBUG_TEXT = false
        private const val CHECKPOINT_TIMER = 2f
        private const val CHECKPOINT_ALPHA_DELAY = 1.5f
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.GAME_PAUSE,
        EventType.GAME_RESUME,

        EventType.PLAYER_SPAWN,
        EventType.PLAYER_READY,
        EventType.PLAYER_JUST_DIED,
        EventType.PLAYER_DONE_DYIN,

        EventType.ADD_PLAYER_HEALTH,
        EventType.ADD_WEAPON_ENERGY,
        EventType.ADD_CURRENCY,
        EventType.ATTAIN_HEART_TANK,
        EventType.ATTAIN_HEALTH_TANK,

        EventType.NEXT_ROOM_REQ,
        EventType.GATE_INIT_OPENING,
        EventType.GATE_INIT_CLOSING,

        EventType.ENTER_BOSS_ROOM,
        EventType.BOSS_READY,
        EventType.END_BOSS_SPAWN,
        EventType.BOSS_DEFEATED,
        EventType.BOSS_DEAD,
        EventType.INTERMEDIATE_BOSS_DEAD,

        EventType.VICTORY_EVENT,
        EventType.END_LEVEL,

        EventType.EDIT_TILED_MAP,

        EventType.SHOW_BACKGROUNDS,
        EventType.HIDE_BACKGROUNDS,

        EventType.START_GAME_CAM_ROTATION,
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

    var music: MusicAsset? = null

    lateinit var screenOnCompletion: (MegamanMaverickGame) -> ScreenEnum

    private lateinit var spawnsMan: SpawnsManager
    private lateinit var playerSpawnsMan: PlayerSpawnsManager

    private lateinit var playerStatsHandler: PlayerStatsHandler
    private lateinit var bossHealthHandler: BossHealthHandler

    private lateinit var levelStateHandler: LevelStateHandler

    private lateinit var endLevelEventHandler: EndLevelEventHandler

    private lateinit var playerSpawnEventHandler: PlayerSpawnEventHandler
    private lateinit var playerDeathEventHandler: PlayerDeathEventHandler

    private lateinit var bossSpawnEventHandler: BossSpawnEventHandler

    private lateinit var drawables: ObjectMap<DrawingSection, Queue<IComparableDrawable<Batch>>>
    private lateinit var shapes: Array<IDrawableShape>
    private lateinit var backgrounds: Array<Background>
    private lateinit var backgroundsToHide: ObjectSet<String>

    private lateinit var checkpointText: MegaFontHandle
    private val checkpointTimer = Timer(CHECKPOINT_TIMER)

    private lateinit var uiCamera: OrthographicCamera
    private lateinit var gameCamera: RotatableCamera
    private lateinit var gameCameraShaker: CameraShaker
    private lateinit var cameraManagerForRooms: CameraManagerForRooms

    private val gameCameraPriorPosition = Vector3()
    private var camerasSetToGameCamera = false

    private val spawns = Array<Spawn>()

    private val pauseScreen = LevelPauseScreen(game)

    private val disposables = Array<Disposable>()

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

        checkpointText = MegaFontHandle(
            ConstVals.EMPTY_STRING,
            positionX = (ConstVals.VIEW_WIDTH - 0.5f) * ConstVals.PPM,
            positionY = ConstVals.PPM.toFloat(),
            attachment = Position.BOTTOM_RIGHT
        )
        checkpointTimer.setToEnd()

        spawnsMan = SpawnsManager(spawns)
        playerSpawnsMan = PlayerSpawnsManager(gameCamera) { current, old ->
            if (current != null) {
                GameLogger.debug(TAG, "playerSpawnsMan(): onChangeSpawn(): current=${current.name}, old=${old?.name}")

                checkpointText.setText("CHECKPOINT ${current.name}")
                checkpointTimer.reset()

                val duplicate = current.properties.get(ConstKeys.DUPLICATE, false, Boolean::class.java)
                if (duplicate) playerSpawnsMan.remove(current.name)
            }
        }

        playerSpawnEventHandler = PlayerSpawnEventHandler(game)
        playerDeathEventHandler = PlayerDeathEventHandler(game)
        bossSpawnEventHandler = BossSpawnEventHandler(game)

        playerStatsHandler = PlayerStatsHandler(megaman)
        playerStatsHandler.init()

        bossHealthHandler = BossHealthHandler(game)

        cameraManagerForRooms = CameraManagerForRooms(
            camera = gameCamera,
            distanceOnTransition = ROOM_DISTANCE_ON_TRANSITION * ConstVals.PPM,
            transitionScannerDimensions = Vector2(
                TRANSITION_SCANNER_SIZE * ConstVals.PPM,
                TRANSITION_SCANNER_SIZE * ConstVals.PPM
            ),
            transDelay = ConstVals.ROOM_TRANS_DELAY_DURATION,
            transDuration = ConstVals.ROOM_TRANS_DURATION,
            shouldInterpolate = { game.isFocusSnappedAway() },
            interpolationValue = { 10f * Gdx.graphics.deltaTime }
        )
        cameraManagerForRooms.focus = megaman
        cameraManagerForRooms.beginTransition = {
            GameLogger.debug(
                TAG,
                "begin transition logic for rooms manager: " +
                    "current=${cameraManagerForRooms.currentGameRoom?.name}, " +
                    "prior=${cameraManagerForRooms.priorGameRoom?.name}"
            )

            eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))

            val current = cameraManagerForRooms.currentGameRoom!!

            if (current.properties.get(ConstKeys.FADE_OUT_MUSIC, Boolean::class.java) == true)
                audioMan.fadeOutMusic(FADE_OUT_MUSIC_DUR)

            val prior = cameraManagerForRooms.priorGameRoom

            eventsMan.submitEvent(
                Event(
                    EventType.BEGIN_ROOM_TRANS, props(
                        ConstKeys.PRIOR pairTo prior,
                        ConstKeys.ROOM pairTo current,
                        ConstKeys.NAME pairTo current.name,
                        "${ConstKeys.PRIOR}_${ConstKeys.NAME}" pairTo prior?.name,
                        ConstKeys.POSITION pairTo cameraManagerForRooms.transitionInterpolation,
                    )
                )
            )

            MegaGameEntities.getOfType(EntityType.ENEMY).forEach { it.destroy() }

            game.getSystem(BehaviorsSystem::class).on = false
            game.putProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        cameraManagerForRooms.continueTransition = {
            eventsMan.submitEvent(
                Event(
                    EventType.CONTINUE_ROOM_TRANS,
                    props(
                        ConstKeys.PRIOR pairTo cameraManagerForRooms.priorGameRoom,
                        ConstKeys.ROOM pairTo cameraManagerForRooms.currentGameRoom,
                        ConstKeys.POSITION pairTo cameraManagerForRooms.transitionInterpolation
                    )
                )
            )

            GameLogger.debug(
                TAG, "continue transition logic for rooms manager: " +
                    "transitionInterpolation=${cameraManagerForRooms.transitionInterpolation} " +
                    "megaman.body.getCenter=${megaman.body.getCenter()}"
            )

            game.putProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        cameraManagerForRooms.endTransition = {
            GameLogger.debug(
                TAG, "end transition logic for rooms manager: " +
                    "current=${cameraManagerForRooms.currentGameRoom?.name}, " +
                    "prior=${cameraManagerForRooms.priorGameRoom?.name}"
            )

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

            when {
                hasEvent -> {
                    val props = props(ConstKeys.ROOM pairTo currentRoom)
                    val roomEvent = when (event) {
                        ConstKeys.BOSS -> EventType.ENTER_BOSS_ROOM
                        ConstKeys.SUCCESS -> EventType.VICTORY_EVENT
                        else -> throw IllegalStateException("Unknown room event: $event")
                    }
                    eventsMan.submitEvent(Event(roomEvent, props))
                }

                else -> eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            }

            game.getSystem(BehaviorsSystem::class).on = true

            game.putProperty(ConstKeys.ROOM_TRANSITION, false)

            megaman.running = false
        }
        cameraManagerForRooms.onSetToRoomNoTrans = {
            GameLogger.debug(TAG, "on set to room no trans")
            eventsMan.submitEvent(
                Event(
                    EventType.SET_TO_ROOM_NO_TRANS,
                    props(ConstKeys.ROOM pairTo cameraManagerForRooms.currentGameRoom)
                )
            )
        }

        gameCameraShaker = CameraShaker(gameCamera)

        pauseScreen.init()

        EntityFactories.init()
    }

    override fun start(tmxMapSource: String) {
        if (!initialized) throw IllegalStateException("must call init() before start()")

        super.start(tmxMapSource)

        eventsMan.addListener(this)
        engine.systems.forEach { it.on = true }
        music?.let { audioMan.playMusic(it, it.loop) }

        game.setCameraRotating(false)
        game.setRoomsSupplier { cameraManagerForRooms.gameRooms }
        game.setCurrentRoomSupplier { cameraManagerForRooms.currentGameRoom }

        if (tiledMapLoadResult == null) throw IllegalStateException("no tiled map load result found in level screen")
        game.setTiledMapLoadResult(tiledMapLoadResult!!)

        val worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        game.setWorldContainer(worldContainer)

        playerSpawnEventHandler.init()
        playerDeathEventHandler.setToEnd()

        endLevelEventHandler.reset()

        game.setFocusSnappedAway(false)
        camerasSetToGameCamera = false
        gameCameraPriorPosition.setZero()
        gameCamera.position.set(ConstFuncs.getGameCamInitPos())
        uiCamera.position.set(ConstFuncs.getGameCamInitPos())

        if (DISPLAY_ROOMS_DEBUG_TEXT) {
            val roomsTextSupplier: () -> String = {
                "current=${cameraManagerForRooms.currentGameRoom?.name} / " +
                    "prior=${cameraManagerForRooms.priorGameRoom?.name}"
            }
            game.setDebugTextSupplier(roomsTextSupplier)
        }
    }

    override fun getLayerBuilders() = MegaMapLayerBuilders(MegaMapLayerBuildersParams(game, spawnsMan))

    override fun buildLevel(result: Properties) {
        backgrounds = result.get(ConstKeys.BACKGROUNDS) as Array<Background>? ?: Array()
        backgroundsToHide =
            result.get("${ConstKeys.HIDDEN}_${ConstKeys.BACKGROUNDS}") as ObjectSet<String>? ?: ObjectSet()

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
        GameLogger.log(TAG, "event(): event=$event")

        when (event.key) {
            EventType.GAME_PAUSE -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): game pause --> pause the game"
                )

                game.pause()
            }

            EventType.GAME_RESUME -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): game resume --> resume the game"
                )

                game.resume()
            }

            EventType.PLAYER_SPAWN -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): player spawn --> reset camera manager for rooms and spawn megaman"
                )

                backgrounds.forEach { background -> background.immediateRotation(Direction.UP) }
                cameraManagerForRooms.reset()

                bossHealthHandler.unset()

                engine.systems.forEach { it.on = true }
                game.putProperty(ConstKeys.ROOM_TRANSITION, false)

                MegaGameEntities.getOfType(EntityType.ENEMY).forEach { it.destroy() }

                val spawnProps = playerSpawnsMan.currentSpawnProps
                if (spawnProps == null) throw IllegalStateException("Megaman spawn props are null")

                GameLogger.debug(TAG, "onEvent(): player spawn --> spawn Megaman: $spawnProps")

                megaman.spawn(spawnProps)
            }

            EventType.PLAYER_READY -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): player ready")
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            }

            EventType.PLAYER_JUST_DIED -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): player just died --> init death handler"
                )

                audioMan.unsetMusic()
                audioMan.stopAllLoopingSounds()

                megaman.removeOneLife()

                playerDeathEventHandler.init()

                if (game.paused) {
                    GameLogger.debug(
                        MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                        "onEvent(): force resume game when Megaman dies"
                    )
                    game.resume()
                }
            }

            EventType.PLAYER_DONE_DYIN -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): player done dying --> respawn player"
                )

                music?.let { audioMan.playMusic(it, it.loop) }

                if (megaman.isAtMinLives()) {
                    game.setCurrentScreen(ScreenEnum.GAME_OVER_SCREEN.name)
                    megaman.resetLives()
                    return
                }

                playerSpawnEventHandler.init()
                playerDeathEventHandler.setToEnd()
            }

            EventType.ADD_WEAPON_ENERGY -> {
                val ammo = event.getProperty(ConstKeys.VALUE, Int::class)!!
                playerStatsHandler.addWeaponEnergy(ammo)
            }

            EventType.ADD_PLAYER_HEALTH -> {
                val health = event.properties.get(ConstKeys.VALUE) as Int
                playerStatsHandler.addHealth(health)
            }

            EventType.ADD_CURRENCY -> {
                val currency = event.getProperty(ConstKeys.VALUE, Int::class)!!
                game.state.addCurrency(currency)
            }

            EventType.ATTAIN_HEART_TANK -> {
                val heartTank = event.getProperty(ConstKeys.VALUE, MegaHeartTank::class)!!
                playerStatsHandler.attain(heartTank)
            }

            EventType.ATTAIN_HEALTH_TANK -> {
                val healthTank = event.getProperty(ConstKeys.VALUE, MegaHealthTank::class)!!
                playerStatsHandler.attain(healthTank)
            }

            EventType.NEXT_ROOM_REQ -> {
                val roomName = event.properties.get(ConstKeys.ROOM) as String

                val transDirection = event.getProperty("${ConstKeys.TRANS}_${ConstKeys.DIRECTION}", Direction::class)
                val isTrans = cameraManagerForRooms.transitionToRoom(roomName, transDirection)

                if (isTrans) GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): next room req --> starting transition to room: $roomName"
                ) else GameLogger.error(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG,
                    "onEvent(): next room req --> could not start transition to room: $roomName"
                )
            }

            EventType.GATE_INIT_OPENING -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): gate init opening --> start room transition"
                )

                val systemsToTurnOff = gdxArrayOf(
                    WorldSystem::class,
                    MotionSystem::class,
                    BehaviorsSystem::class,
                    SimplePathfindingSystem::class
                )
                systemsToTurnOff.forEach { game.getSystem(it).on = false }
            }

            EventType.GATE_INIT_CLOSING -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): gate init closing")

                val systemsToTurnOn = gdxArrayOf(
                    WorldSystem::class,
                    MotionSystem::class,
                    BehaviorsSystem::class,
                    SimplePathfindingSystem::class
                )
                systemsToTurnOn.forEach { game.getSystem(it).on = true }
            }

            EventType.ENTER_BOSS_ROOM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): enter boss room")

                val bossRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!
                val bossMapObject = bossRoom.properties.get(ConstKeys.OBJECT, RectangleMapObject::class.java)
                val bossSpawnProps = bossMapObject.properties.toProps()

                // if this is a mini-boss
                val mini = bossSpawnProps.getOrDefault(ConstKeys.MINI, false, Boolean::class)
                // if this boss is at the end of the level
                val end = bossSpawnProps.getOrDefault(ConstKeys.END, true, Boolean::class)
                // if this is NOT a mini-boss AND this is the end of the level, then check if the
                // victory event should be triggered (when the boss is already defeated)
                if (!mini && end) {
                    val levelDef = game.getCurrentLevel()
                    if (game.state.isLevelDefeated(levelDef)) {
                        eventsMan.submitEvent(Event(EventType.VICTORY_EVENT))
                        return
                    }
                }

                bossSpawnProps.put(ConstKeys.BOUNDS, bossMapObject.rectangle.toGameRectangle())
                bossSpawnEventHandler.init(bossMapObject.name, bossSpawnProps)
            }

            EventType.BOSS_READY -> {
                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!

                when {
                    boss.mini -> eventsMan.submitEvent(Event(EventType.END_BOSS_SPAWN))
                    else -> {
                        val type = event.getOrDefaultProperty(
                            ConstKeys.HEALTH_FILL_TYPE,
                            HealthFillType.BIT_BY_BIT,
                            HealthFillType::class
                        )

                        val runOnFirstUpdate: (() -> Unit)? = when (type) {
                            HealthFillType.BIT_BY_BIT -> {
                                { game.audioMan.pauseMusic() }
                            }

                            else -> null
                        }

                        val runOnFinished = {
                            eventsMan.submitEvent(Event(EventType.END_BOSS_SPAWN))
                            if (type == HealthFillType.BIT_BY_BIT) game.audioMan.playMusic()
                        }

                        bossHealthHandler.set(boss, type, runOnFirstUpdate, runOnFinished)

                        game.engine.systems.forEach {
                            if (!it.isAny(SpritesSystem::class, AnimationsSystem::class)) it.on = false
                        }
                    }
                }
            }

            EventType.END_BOSS_SPAWN -> {
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))

                game.engine.systems.forEach { it.on = true }
            }

            EventType.BOSS_DEFEATED -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): boss defeated")

                if (megaman.dead) {
                    GameLogger.debug(TAG, "onEvent(): Megaman dead when boss defeated, doing nothing...")
                    return
                }

                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!

                if (!boss.mini) {
                    eventsMan.submitEvent(
                        Event(
                            EventType.TURN_CONTROLLER_OFF, props(
                                "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}" pairTo false
                            )
                        )
                    )

                    megaman.canBeDamaged = false

                    audioMan.unsetMusic()
                }

                MegaGameEntities.forEach {
                    if (it is IBossListener && it.getType() != EntityType.BOSS) it.onBossDefeated(boss)
                }

                bossHealthHandler.unset()
            }

            EventType.BOSS_DEAD -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): boss dead")

                if (megaman.dead) {
                    GameLogger.debug(TAG, "onEvent(): Megaman dead when boss dead, doing nothing...")
                    return
                }

                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                val eventType = if (boss.isEndLevelBoss()) EventType.VICTORY_EVENT else EventType.INTERMEDIATE_BOSS_DEAD
                eventsMan.submitEvent(Event(eventType, props(ConstKeys.BOSS pairTo boss)))
            }

            EventType.INTERMEDIATE_BOSS_DEAD -> {
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
                megaman.canBeDamaged = true

                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                if (!boss.mini) audioMan.playMusic(music, true)
            }

            EventType.VICTORY_EVENT -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): victory event")
                endLevelEventHandler.init()
            }

            EventType.SHAKE_CAM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): request to shake game cam")
                if (gameCameraShaker.isFinished) {
                    val duration = event.properties.get(ConstKeys.DURATION, Float::class)!!
                    val interval = event.properties.get(ConstKeys.INTERVAL, Float::class)!!
                    val shakeX = event.properties.get(ConstKeys.X, Float::class)!!
                    val shakeY = event.properties.get(ConstKeys.Y, Float::class)!!
                    gameCameraShaker.startShake(duration, interval, shakeX, shakeY)
                }
            }

            EventType.END_LEVEL -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): end level")

                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))

                val level = game.getCurrentLevel()
                game.state.addLevelDefeated(level)

                val nextScreen = screenOnCompletion.invoke(game)
                game.setCurrentScreen(nextScreen.name)
            }

            EventType.EDIT_TILED_MAP -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): edit tiled map")
                val editor = event.getProperty(ConstKeys.EDIT)!! as (TiledMap) -> Unit
                tiledMapLoadResult?.map?.let {
                    GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): invoking editor")
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

            EventType.START_GAME_CAM_ROTATION -> {
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

    private fun isPauseButtonRequested() = controllerPoller.isJustReleased(MegaControllerButton.START) ||
        (game.selectButtonAction == SelectButtonAction.START && controllerPoller.isJustPressed(MegaControllerButton.SELECT))

    override fun render(delta: Float) {
        // do not allow pausing if Megaman is dead
        if (!game.paused && !megaman.dead && isPauseButtonRequested()) {
            GameLogger.debug(TAG, "render(): pause game")
            game.runQueue.addLast { game.pause() }
        }

        // force resume game if megaman is dead
        if (game.paused && megaman.dead) game.runQueue.addLast { game.resume() }

        playerStatsHandler.update(delta)

        if (!game.paused) {
            spawnsMan.update(delta / 2f)

            val spawnsIter = spawns.iterator()
            while (spawnsIter.hasNext()) {
                val spawn = spawnsIter.next()
                engine.spawn(spawn.entity, spawn.properties)
                spawnsIter.remove()
            }

            // because I'm not good at software design, there's a case tight coupling in this block
            // essentially, the order in which these handlers are updated must not be modified or
            // else the flow of events in the game might become broken
            when {
                !bossHealthHandler.finished -> bossHealthHandler.update(delta)
                !endLevelEventHandler.finished -> endLevelEventHandler.update(delta)
                !playerSpawnEventHandler.isFinished() -> playerSpawnEventHandler.update(delta)
                !playerDeathEventHandler.finished -> playerDeathEventHandler.update(delta)
            }

            if (!megaman.dead) playerDeathEventHandler.setInactive()
        }

        engine.update(delta)

        if (!game.paused) {
            spawnsMan.update(delta / 2f)

            playerSpawnsMan.run()

            val spawnsIter = spawns.iterator()
            while (spawnsIter.hasNext()) {
                val spawn = spawnsIter.next()
                engine.spawn(spawn.entity, spawn.properties)
                spawnsIter.remove()
            }

            backgrounds.forEach { it.update(delta) }
            cameraManagerForRooms.update(delta)
            gameCamera.update(delta)

            if (!gameCameraShaker.isFinished) gameCameraShaker.update(delta)

            if (game.isFocusSnappedAway()) {
                val focus = megaman.getFocusPosition()
                if (focus.x.epsilonEquals(gameCamera.position.x, 0.25f * ConstVals.PPM) &&
                    focus.y.epsilonEquals(gameCamera.position.y, 0.25f * ConstVals.PPM)
                ) game.setFocusSnappedAway(false)
            }
        }

        val gameCamDeltaX = gameCamera.position.x - gameCameraPriorPosition.x
        val gameCamDeltaY = gameCamera.position.y - gameCameraPriorPosition.y
        backgrounds.forEach { it.move(gameCamDeltaX, gameCamDeltaY) }
        gameCameraPriorPosition.set(gameCamera.position)

        // perform this on all cameras to reduce floating point rounding errors
        game.viewports.values().forEach { viewport ->
            val camera = viewport.camera
            camera.position.x = (camera.position.x * ConstVals.PPM) / ConstVals.PPM
            camera.position.y = (camera.position.y * ConstVals.PPM) / ConstVals.PPM
        }

        if (game.paused) pauseScreen.render(delta)

        // sort backgrounds in drawing order before calling draw()
        backgrounds.sort()

        checkpointTimer.update(delta)
    }

    override fun draw(drawer: Batch) {
        drawer.begin()

        // each background has its own viewport instance which is applied when the background is drawn,
        // so wait until after all backgrounds are drawn before applying the game viewport
        backgrounds.forEach { if (!backgroundsToHide.contains(it.key)) it.draw(batch) }

        game.viewports.get(ConstKeys.GAME).apply()
        drawer.projectionMatrix = gameCamera.combined

        val backgroundSprites = drawables.get(DrawingSection.BACKGROUND)
        while (!backgroundSprites.isEmpty()) {
            val backgroundSprite = backgroundSprites.poll()
            backgroundSprite.draw(drawer)
        }

        tiledMapLevelRenderer?.render(gameCamera)

        val playgroundSprites = drawables.get(DrawingSection.PLAYGROUND)
        while (!playgroundSprites.isEmpty()) {
            val playgroundSprite = playgroundSprites.poll()
            playgroundSprite.draw(drawer)
        }

        val foregroundSprites = drawables.get(DrawingSection.FOREGROUND)
        while (!foregroundSprites.isEmpty()) {
            val foregroundSprite = foregroundSprites.poll()
            foregroundSprite.draw(drawer)
        }

        game.viewports.get(ConstKeys.UI).apply()
        drawer.projectionMatrix = uiCamera.combined

        bossHealthHandler.draw(drawer)
        playerStatsHandler.draw(drawer)

        if (!playerSpawnEventHandler.isFinished()) playerSpawnEventHandler.draw(drawer)

        if (megaman.dead) playerDeathEventHandler.draw(drawer)

        if (game.paused) pauseScreen.draw(drawer)

        if (!checkpointTimer.isFinished()) {
            val alpha = when {
                checkpointTimer.time < CHECKPOINT_ALPHA_DELAY -> 1f
                else -> ConstVals.ONE.minus(
                    (checkpointTimer.time.minus(CHECKPOINT_ALPHA_DELAY))
                        .div(checkpointTimer.duration.minus(CHECKPOINT_ALPHA_DELAY))
                )
            }
            checkpointText.setAlpha(alpha)
            checkpointText.draw(drawer)
        }

        drawer.end()
    }

    override fun draw(renderer: ShapeRenderer) {
        renderer.projectionMatrix = gameCamera.combined

        while (!shapes.isEmpty) {
            val shape = shapes.pop()
            shape.draw(renderer)
        }

        if (game.params.debugShapes) {
            val gameCamBounds = gameCamera.getRotatedBounds()
            gameCamBounds.translate(0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM)
            gameCamBounds.translateSize(-0.2f * ConstVals.PPM, -0.2f * ConstVals.PPM)

            renderer.color = Color.BLUE
            renderer.set(ShapeType.Line)

            renderer.rect(
                gameCamBounds.getX(),
                gameCamBounds.getY(),
                gameCamBounds.getWidth(),
                gameCamBounds.getHeight()
            )
        }
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset(): Resetting level screen")

        eventsMan.removeListener(this)

        // TODO: disable clearing entity factories as an optimization
        // MegaEntityFactory.reset()

        // TODO: should replace EntityFactories with MegaEntityFactory
        // EntityFactories.clear()

        engine.reset()

        spawns.clear()
        spawnsMan.reset()
        playerSpawnsMan.reset()

        playerSpawnEventHandler.reset()
        playerDeathEventHandler.setToEnd()

        endLevelEventHandler.reset()

        backgrounds.forEach { background -> background.reset() }

        cameraManagerForRooms.reset()
        gameCamera.immediateRotation(Direction.UP)

        audioMan.unsetMusic()

        game.putProperty(ConstKeys.ROOM_TRANSITION, false)
        eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))

        game.setFocusSnappedAway(false)
    }

    override fun dispose() {
        GameLogger.debug(TAG, "dispose(): Disposing level screen")
        super.dispose()

        disposables.forEach { it.dispose() }
        disposables.clear()

        pauseScreen.dispose()
    }

    override fun pause() {
        levelStateHandler.pause()
        pauseScreen.show()
    }

    override fun resume() {
        levelStateHandler.resume()
        pauseScreen.reset()
    }

    override fun resize(width: Int, height: Int) =
        backgrounds.forEach { background -> background.updateViewportSize(width, height) }
}
