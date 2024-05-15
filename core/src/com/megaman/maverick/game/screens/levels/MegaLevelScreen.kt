package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
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
import com.engine.behaviors.BehaviorsSystem
import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.Initializable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.toGameRectangle
import com.engine.controller.ControllerSystem
import com.engine.controller.polling.IControllerPoller
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sorting.IComparableDrawable
import com.engine.drawables.sprites.SpritesSystem
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
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.drawables.sprites.Background
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.CameraManagerForRooms
import com.megaman.maverick.game.screens.levels.camera.CameraShaker
import com.megaman.maverick.game.screens.levels.events.BossSpawnEventHandler
import com.megaman.maverick.game.screens.levels.events.EndLevelEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerDeathEventHandler
import com.megaman.maverick.game.screens.levels.events.PlayerSpawnEventHandler
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuilders
import com.megaman.maverick.game.screens.levels.map.layers.MegaMapLayerBuildersParams
import com.megaman.maverick.game.screens.levels.spawns.PlayerSpawnsManager
import com.megaman.maverick.game.screens.levels.stats.EntityStatsHandler
import com.megaman.maverick.game.screens.levels.stats.PlayerStatsHandler
import com.megaman.maverick.game.utils.toProps
import java.util.*

class MegaLevelScreen(game: MegamanMaverickGame) : TiledMapLevelScreen(game), Initializable {

    companion object {
        const val TAG = "MegaLevelScreen"
        const val MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG = "MegaLevelScreenEventListener"
        private const val INTERPOLATE_GAME_CAM = true
        private const val DEFAULT_BACKGROUND_PARALLAX_FACTOR = 0.5f
        private const val DEFAULT_FOREGROUND_PARALLAX_FACTOR = 2f
        private const val FADE_OUT_MUSIC_ON_BOSS_SPAWN = 1f
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
        EventType.GATE_INIT_OPENING,
        EventType.NEXT_ROOM_REQ,
        EventType.GATE_INIT_CLOSING,
        EventType.REQ_SHAKE_CAM,
        EventType.ENTER_BOSS_ROOM,
        EventType.BEGIN_BOSS_SPAWN,
        EventType.BOSS_DEFEATED,
        EventType.BOSS_DEAD,
        EventType.MINI_BOSS_DEAD,
        EventType.VICTORY_EVENT,
        EventType.END_LEVEL
    )

    val megamanGame: MegamanMaverickGame
        get() = super.game as MegamanMaverickGame
    val engine: IGameEngine
        get() = game.engine
    val megaman: Megaman
        get() = megamanGame.megaman
    val eventsMan: IEventsManager
        get() = megamanGame.eventsMan
    val audioMan: MegaAudioManager
        get() = megamanGame.audioMan
    val controllerPoller: IControllerPoller
        get() = megamanGame.controllerPoller

    var level: Level? = null
    var music: MusicAsset? = null

    private lateinit var spawnsMan: SpawnsManager
    private lateinit var playerSpawnsMan: PlayerSpawnsManager
    private lateinit var playerStatsHandler: PlayerStatsHandler
    private lateinit var entityStatsHandler: EntityStatsHandler

    private lateinit var levelStateHandler: LevelStateHandler
    private lateinit var endLevelEventHandler: EndLevelEventHandler
    private lateinit var cameraManagerForRooms: CameraManagerForRooms
    private lateinit var cameraShaker: CameraShaker

    private lateinit var playerSpawnEventHandler: PlayerSpawnEventHandler
    private lateinit var playerDeathEventHandler: PlayerDeathEventHandler

    private lateinit var bossSpawnEventHandler: BossSpawnEventHandler

    private lateinit var drawables: ObjectMap<DrawingSection, PriorityQueue<IComparableDrawable<Batch>>>
    private lateinit var shapes: PriorityQueue<IDrawableShape>
    private lateinit var backgrounds: Array<Background>

    private lateinit var backgroundCamera: OrthographicCamera
    private lateinit var gameCamera: OrthographicCamera
    private lateinit var foregroundCamera: OrthographicCamera
    private lateinit var uiCamera: OrthographicCamera

    private lateinit var disposables: Array<Disposable>

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

        entityStatsHandler = EntityStatsHandler(megamanGame)

        playerSpawnEventHandler = PlayerSpawnEventHandler(megamanGame)
        playerDeathEventHandler = PlayerDeathEventHandler(megamanGame)

        bossSpawnEventHandler = BossSpawnEventHandler(megamanGame)

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

        cameraShaker = CameraShaker(gameCamera)

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

        cameraManagerForRooms.endTransition = {
            GameLogger.debug(TAG, "End transition logic for camera manager")
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
                        ConstKeys.SUCCESS -> EventType.VICTORY_EVENT
                        else -> throw IllegalStateException("Unknown room event: $roomEventString")
                    }

                eventsMan.submitEvent(Event(roomEvent, props))
            } else systemsToSwitch.forEach { systems.get(it.simpleName)?.let { system -> system.on = true } }
        }
    }

    override fun show() {
        if (!initialized) init()
        dispose()

        EntityFactories.init()
        super.show()

        eventsMan.addListener(this)
        engine.systems.forEach { it.on = true }

        music?.let { audioMan.playMusic(it, true) }

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
        backgrounds = result.get(ConstKeys.BACKGROUNDS) as Array<Background>? ?: Array()

        val playerSpawns =
            result.get("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}") as Array<RectangleMapObject>?
                ?: Array()
        playerSpawnsMan.set(playerSpawns)

        cameraManagerForRooms.gameRooms =
            result.get(ConstKeys.GAME_ROOMS) as Array<RectangleMapObject>? ?: Array()

        val spawners = result.get(ConstKeys.SPAWNERS) as Array<ISpawner>? ?: Array()
        spawnsMan.setSpawners(spawners)

        val _disposables = result.get(ConstKeys.DISPOSABLES) as Array<Disposable>? ?: Array()
        disposables.addAll(_disposables)

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
                game.engine.systems.forEach { it.on = true }
                game.engine.spawn(megaman, playerSpawnsMan.currentSpawnProps!!)
                entityStatsHandler.unset()
            }

            EventType.PLAYER_READY -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Player ready")
                game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
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
                val systemsToTurnOff = gdxArrayOf(
                    MotionSystem::class.simpleName,
                    BehaviorsSystem::class.simpleName,
                    WorldSystem::class.simpleName
                )
                systemsToTurnOff.forEach { megamanGame.getSystems().get(it).on = false }
                megamanGame.megaman.body.physics.velocity.setZero()
                game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
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
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Gate init closing")
                val systemsToTurnOff = gdxArrayOf(
                    MotionSystem::class.simpleName,
                    BehaviorsSystem::class.simpleName,
                    WorldSystem::class.simpleName
                )
                systemsToTurnOff.forEach { megamanGame.getSystems().get(it).on = true }
                game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            }

            EventType.REQ_SHAKE_CAM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Req shake cam")
                if (cameraShaker.isFinished) {
                    val duration = event.properties.get(ConstKeys.DURATION, Float::class)!!
                    val interval = event.properties.get(ConstKeys.INTERVAL, Float::class)!!
                    val shakeX = event.properties.get(ConstKeys.X, Float::class)!!
                    val shakeY = event.properties.get(ConstKeys.Y, Float::class)!!
                    cameraShaker.startShake(duration, interval, shakeX, shakeY)
                }
            }

            EventType.ENTER_BOSS_ROOM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Enter boss room")
                val bossRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!
                val bossMapObject = bossRoom.properties.get(ConstKeys.OBJECT, RectangleMapObject::class.java)
                val bossName = bossMapObject.name
                val bossSpawnProps = bossMapObject.properties.toProps()
                bossSpawnProps.put(ConstKeys.BOUNDS, bossMapObject.rectangle.toGameRectangle())

                val mini = bossSpawnProps.getOrDefault(ConstKeys.MINI, false, Boolean::class)
                if (!mini) audioMan.fadeOutMusic(FADE_OUT_MUSIC_ON_BOSS_SPAWN)

                bossSpawnEventHandler.init(bossName, bossSpawnProps, mini)

                megaman.running = false
                engine.systems.forEach { it.on = it is WorldSystem || it is SpritesSystem || it is AnimationsSystem }
            }

            EventType.BEGIN_BOSS_SPAWN -> {
                val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                entityStatsHandler.set(boss)
            }

            EventType.BOSS_DEFEATED -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Boss defeated")
                val mini = event.getProperty(ConstKeys.MINI, Boolean::class)!!
                if (!mini) audioMan.stopMusic()

                val systemsToSwitch = gdxArrayOf(MotionSystem::class, BehaviorsSystem::class)
                engine.systems.forEach { if (systemsToSwitch.contains(it::class)) it.on = false }
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
                megaman.canBeDamaged = false
                entityStatsHandler.unset()
            }

            EventType.BOSS_DEAD -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Boss dead")
                val mini = event.getProperty(ConstKeys.MINI, Boolean::class)!!
                val eventType = if (mini) EventType.MINI_BOSS_DEAD else EventType.VICTORY_EVENT
                eventsMan.submitEvent(Event(eventType))
            }

            EventType.MINI_BOSS_DEAD -> {
                val systemsToSwitch = gdxArrayOf(MotionSystem::class, BehaviorsSystem::class)
                engine.systems.forEach { if (systemsToSwitch.contains(it::class)) it.on = true }
                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
                megaman.canBeDamaged = true
            }

            EventType.VICTORY_EVENT -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): Victory event")
                endLevelEventHandler.init()
            }

            EventType.END_LEVEL -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): End level")
                game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
                val nextScreen = LevelCompletionMap.getNextScreen(level!!)
                game.setCurrentScreen(nextScreen.name)
            }
        }
    }

    override fun render(delta: Float) {
        if (controllerPoller.isJustPressed(ControllerButton.START) &&
            playerStatsHandler.finished &&
            playerSpawnEventHandler.finished &&
            playerDeathEventHandler.finished &&
            bossSpawnEventHandler.finished
        ) {
            if (megamanGame.paused) megamanGame.resume() else megamanGame.pause()
        }

        if (game.paused &&
            (!playerStatsHandler.finished ||
                    !playerSpawnEventHandler.finished ||
                    !playerDeathEventHandler.finished)
        ) game.resume()

        if (!game.paused) {
            backgrounds.forEach { it.update(delta) }
            cameraManagerForRooms.update(delta)

            if (playerSpawnEventHandler.finished && !cameraManagerForRooms.transitioning) {
                playerSpawnsMan.run()
                spawnsMan.update(delta)
                val spawns = spawnsMan.getSpawnsAndClear()
                spawns.forEach { spawn -> engine.spawn(spawn.entity, spawn.properties) }
            }

            if (!bossSpawnEventHandler.finished) bossSpawnEventHandler.update(delta)

            if (!playerSpawnEventHandler.finished) playerSpawnEventHandler.update(delta)
            else if (!playerDeathEventHandler.finished) playerDeathEventHandler.update(delta)
            else if (!endLevelEventHandler.finished) endLevelEventHandler.update(delta)

            playerStatsHandler.update(delta)
        }


        val gameCamDeltaX = gameCamera.position.x - gameCameraPriorPosition.x
        backgroundCamera.position.x += gameCamDeltaX * backgroundParallaxFactor
        gameCameraPriorPosition.set(gameCamera.position)

        engine.update(delta)

        val batch = megamanGame.batch
        batch.begin()

        batch.projectionMatrix = backgroundCamera.combined
        backgrounds.forEach { it.draw(batch) }

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

        batch.projectionMatrix = uiCamera.combined

        entityStatsHandler.draw(batch)
        playerStatsHandler.draw(batch)

        batch.end()

        if (!playerSpawnEventHandler.finished) playerSpawnEventHandler.draw(batch)
        else if (!endLevelEventHandler.finished) endLevelEventHandler.draw(batch)

        val shapeRenderer = megamanGame.shapeRenderer
        shapeRenderer.projectionMatrix = gameCamera.combined

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        while (!shapes.isEmpty()) {
            val shape = shapes.poll()
            shape.draw(shapeRenderer)
        }
        shapeRenderer.end()

        if (Gdx.app.input.isKeyJustPressed(Keys.G)) eventsMan.submitEvent(
            Event(
                EventType.REQ_SHAKE_CAM, props(
                    ConstKeys.DURATION to 1f,
                    ConstKeys.INTERVAL to 0.1f,
                    ConstKeys.X to 0.25f,
                    ConstKeys.Y to 0.25f
                )
            )
        )
        if (!cameraShaker.isFinished) cameraShaker.update(delta)
    }

    override fun hide() = dispose()

    override fun dispose() {
        GameLogger.debug(TAG, "dispose(): Disposing level screen")

        super.dispose()
        EntityFactories.clear()

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
