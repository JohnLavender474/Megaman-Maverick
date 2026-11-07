package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsSystem
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraShake
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.objects.InsertionOrderPriorityQueue
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sorting.IComparableDrawable
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.direction
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.EventsManager
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.motion.MotionSystem
import com.maverick.game.engine.pathfinding.AsyncPathfindingSystem
import com.maverick.game.engine.pathfinding.SimplePathfindingSystem
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.players.playerSlot
import com.maverick.game.engine.screens.levels.tiledmap.TiledMapLevelScreen
import com.maverick.game.engine.screens.levels.tiledmap.TiledMapLoader
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.container.IWorldContainer
import com.maverick.game.engine.world.container.LooseQuadtreeWorldContainer
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.drawables.backgrounds.Background
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.depleteHealth
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.focus
import com.megaman.maverick.game.entities.hazards.ViewEdgeHazard
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.fights.FightDirector
import com.megaman.maverick.game.fights.FightPlans
import com.megaman.maverick.game.input.SystemButton
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.camera.CameraEdgeBlocks
import com.megaman.maverick.game.screens.levels.camera.CameraLimitStage
import com.megaman.maverick.game.screens.levels.camera.CameraRoomBounds
import com.megaman.maverick.game.screens.levels.camera.CameraRoomClamp
import com.megaman.maverick.game.screens.levels.camera.CameraRoomScan
import com.megaman.maverick.game.screens.levels.camera.CameraScroll
import com.megaman.maverick.game.screens.levels.camera.CameraScrollSection
import com.megaman.maverick.game.screens.levels.camera.CameraRoomExits
import com.megaman.maverick.game.screens.levels.camera.CameraViewEdges
import com.megaman.maverick.game.screens.levels.camera.CameraZone
import com.megaman.maverick.game.screens.levels.camera.CameraZones
import com.megaman.maverick.game.screens.levels.camera.PlayersFocus
import com.megaman.maverick.game.screens.levels.camera.isPastCameraEdge
import com.megaman.maverick.game.screens.levels.events.BossSpawnEventHandler
import com.megaman.maverick.game.screens.levels.events.EndLevelEventHandler
import com.megaman.maverick.game.screens.levels.respawn.BeamInView
import com.megaman.maverick.game.screens.levels.respawn.DeathFadeOut
import com.megaman.maverick.game.screens.levels.respawn.RespawnContext
import com.megaman.maverick.game.screens.levels.respawn.RespawnRoutine
import com.megaman.maverick.game.screens.levels.spawns.PlayerCheckpointManager
import com.megaman.maverick.game.screens.levels.spawns.slotSpawnProps
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler
import com.megaman.maverick.game.screens.levels.stats.PlayerStatsHandler
import com.megaman.maverick.game.screens.levels.tiled.layers.MegaMapLayerBuilders
import com.megaman.maverick.game.screens.levels.tiled.layers.MegaMapLayerBuildersParams
import com.megaman.maverick.game.screens.menus.level.LevelPauseScreen
import com.megaman.maverick.game.spawns.ISpawner
import com.megaman.maverick.game.spawns.SpawnsManager
import com.megaman.maverick.game.utils.extensions.getRotatedBounds
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.entities.megamanx.MegamanX
import com.megaman.maverick.game.controllers.SelectButtonAction

class MegaLevelScreen(private val game: MegamanMaverickGame) :
    TiledMapLevelScreen(game.batch, TiledMapLoader(game.assets)), Initializable, IEventListener, IShapeDebuggable,
    Resettable {

    companion object {
        const val TAG = "MegaLevelScreen"
        const val MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG = "MegaLevelScreenEventListener"
        private const val DRAW_CAMERA_SECTIONS = false
        private const val ROOM_DISTANCE_ON_TRANSITION = 2f
        private const val TRANSITION_SCANNER_SIZE = 5f
        private const val CHECKPOINT_TIMER = 2f
        private const val CHECKPOINT_ALPHA_DELAY = 1.5f
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.GAME_PAUSE,
        EventType.GAME_RESUME,

        EventType.PLAYER_JUST_DIED,

        EventType.ADD_PLAYER_HEALTH,
        EventType.ADD_WEAPON_ENERGY,
        EventType.ADD_CURRENCY,
        EventType.ATTAIN_HEART_TANK,
        EventType.ATTAIN_HEALTH_TANK,

        EventType.NEXT_ROOM_REQ,
        EventType.GATE_INIT_OPENING,
        EventType.GATE_INIT_CLOSING,

        EventType.ENTER_BOSS_ROOM,

        EventType.VICTORY_EVENT,
        EventType.END_LEVEL,
        EventType.GAME_COMPLETE,

        EventType.EDIT_TILED_MAP,

        EventType.SHOW_BACKGROUNDS,
        EventType.HIDE_BACKGROUNDS,

        EventType.START_GAME_CAM_ROTATION,
        EventType.SHAKE_CAM,
    )

    val engine: GameEngine
        get() = game.engine
    // TODO: the level rules act on the lead player in slot 0: the checkpoint, the spawn, the camera focus and
    //  snap-back, the gravity aura, the boss-defeat dead checks, pause and the forced resume, and the death handler;
    //  with more players, the lead slot is a session policy and each rule needs a co-op version (item 047)
    private val player: MegaGameEntity
        get() = game.players.get(PlayerSlot(0))!!
    val eventsMan: EventsManager
        get() = game.events
    val audioMan: MegaAudioManager
        get() = game.audio

    var music: MusicAsset? = null

    lateinit var screenOnCompletion: (LevelState) -> ScreenEnum

    private lateinit var spawnsMan: SpawnsManager
    private lateinit var playerCheckpointMan: PlayerCheckpointManager

    private lateinit var playerStatsHandler: PlayerStatsHandler
    private lateinit var bossHealthHandler: BossHealthHandler

    private lateinit var levelStateHandler: LevelStateHandler

    private lateinit var endLevelEventHandler: EndLevelEventHandler

    private lateinit var beamIn: BeamInView
    private lateinit var deathFade: DeathFadeOut
    private lateinit var respawn: RespawnRoutine

    private lateinit var bossSpawnEventHandler: BossSpawnEventHandler

    internal val fights = FightDirector()

    private lateinit var drawables: ObjectMap<DrawingSection, InsertionOrderPriorityQueue<IComparableDrawable<Batch>>>
    private lateinit var shapes: Array<IDrawableShape>
    private lateinit var backgrounds: Array<Background>
    private lateinit var backgroundsToHide: ObjectSet<String>

    private lateinit var checkpointText: MegaFontHandle
    private val checkpointTimer = Timer(CHECKPOINT_TIMER)

    private lateinit var uiCamera: OrthographicCamera
    private lateinit var gameCamera: OrthographicCamera
    private lateinit var cameraFollow: CameraFollow
    private lateinit var roomTransition: CameraRoomTransition
    private lateinit var roomBounds: CameraRoomBounds
    private lateinit var cameraZones: CameraZones
    private lateinit var cameraScroll: CameraScroll
    private var viewEdges: CameraViewEdges? = null
    private var playersFocus: PlayersFocus? = null
    private var roomExits: CameraRoomExits? = null
    private lateinit var cameraShake: CameraShake
    private lateinit var roomHandler: LevelRoomHandler

    private lateinit var levelPointsText: MegaFontHandle
    private lateinit var coinsText: MegaFontHandle

    private val gameCameraPriorPosition = Vector3()
    private var camerasSetToGameCamera = false

    private val spawnCameraBounds = GameRectangle()

    private val pauseScreen = LevelPauseScreen(game)

    private val disposables = Array<Disposable>()

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        levelStateHandler = LevelStateHandler(game)

        endLevelEventHandler = EndLevelEventHandler(game)

        drawables = game.drawables
        shapes = game.shapes

        gameCamera = game.gameCamera
        uiCamera = game.getUiCamera()

        checkpointText = MegaFontHandle(
            ConstVals.EMPTY_STRING,
            positionX = (ConstVals.VIEW_WIDTH - 0.5f) * ConstVals.PPM,
            positionY = ConstVals.PPM.toFloat(),
            attachment = Position.BOTTOM_RIGHT
        )
        checkpointTimer.setToEnd()

        spawnsMan = SpawnsManager { entity, props -> engine.spawn(entity, props) }
        playerCheckpointMan = PlayerCheckpointManager(gameCamera) { current, old ->
            if (current != null && player.inPlay) {
                GameLogger.debug(TAG, "playerCheckpointMan(): onChangeSpawn(): current=${current.name}, old=${old?.name}")

                checkpointText.setText("CHECKPOINT ${current.name}")
                checkpointTimer.reset()

                val duplicate = current.properties.get(ConstKeys.DUPLICATE, false, Boolean::class.java)
                if (duplicate) playerCheckpointMan.remove(current.name)
            } else {
                checkpointText.setText("")
                checkpointTimer.setToEnd()
            }
        }

        beamIn = BeamInView(game)
        deathFade = DeathFadeOut(game)
        respawn = RespawnRoutine(
            RespawnContext(game, deathFade, beamIn, resetLevel = { resetLevelForRespawn() }, levelMusic = { music })
        )
        bossSpawnEventHandler = BossSpawnEventHandler(game)

        playerStatsHandler = PlayerStatsHandler(game)
        playerStatsHandler.init()

        bossHealthHandler = BossHealthHandler(game)

        cameraFollow = CameraFollow(0.25f * ConstVals.PPM, { 20f * Gdx.graphics.deltaTime })
        roomTransition = CameraRoomTransition(
            cameraFollow,
            ROOM_DISTANCE_ON_TRANSITION * ConstVals.PPM,
            ConstVals.ROOM_TRANS_DELAY_DURATION,
            ConstVals.ROOM_TRANS_DURATION
        )
        roomBounds = CameraRoomBounds(
            cameraFollow,
            roomTransition,
            Vector2(TRANSITION_SCANNER_SIZE * ConstVals.PPM, TRANSITION_SCANNER_SIZE * ConstVals.PPM)
        )
        cameraShake = CameraShake(ConstVals.PPM.toFloat())
        game.cameraController.addMover(roomBounds)
        game.cameraController.addMover(roomTransition)
        game.cameraController.addMover(cameraFollow)
        val cameraHold = {
            game.cameraView.rotating || game.getSystem(WorldSystem::class)?.on == false || !player.inPlay
        }
        val limitStage = CameraLimitStage(
            game.cameraView,
            cameraFollow,
            roomBounds,
            game.cameraController.settledPosition,
            ConstVals.CAMERA_LIMIT_EASE_SPEED * ConstVals.PPM,
            cameraHold
        )
        cameraZones = CameraZones(cameraFollow)
        limitStage.addRule(cameraZones)
        cameraScroll = CameraScroll(
            game.cameraView,
            cameraFollow,
            roomBounds,
            game.cameraController.settledPosition,
            { game.gravity.direction.getOpposite() },
            { cameraHold() || !player.existenceState.liveAfterUpdate }
        )
        limitStage.addRule(cameraScroll)
        game.cameraController.addMover(cameraScroll)
        game.cameraController.addMover(limitStage)
        game.cameraController.addMover(CameraRoomClamp(game.cameraView, limitStage))
        game.cameraController.addMover(CameraRoomScan(roomBounds))
        game.cameraController.addEffect(game.cameraRotation)
        game.cameraController.addEffect(cameraShake)
        game.cameraController.addEffect(cameraFollow.stopRule)
        game.cameraController.addEffect(
            CameraEdgeBlocks(limitStage, { player.inPlay && !roomTransition.transitioning }) { direction, edge ->
                if (isPastCameraEdge(player.body.getBounds(), direction, edge)) player.depleteHealth()
            }
        )

        // TODO: the view edge rules are off in a turned room and in a room with a camera zone or scroll, because a
        //  section can move the view into a player; phase 5 of the shared screen sets the rule there (item 047,
        //  "Decisions", 2026-11-06)
        val viewEdgesOn = {
            player.inPlay && !roomTransition.transitioning && !game.cameraView.rotating &&
                game.cameraRotation.direction == Direction.UP && !roomHasCameraSection()
        }
        val viewEdges = if (game.players.get(PlayerSlot(1)) != null) CameraViewEdges(
            game.cameraView,
            game.cameraController.settledPosition,
            game.players,
            game.services.rules.viewEdges,
            ViewEdgeHazard(game.services).also { it.damage = game.services.rules.viewEdges.damage },
            { roomBounds.currentGameRoom?.rectangle },
            viewEdgesOn
        ) else null
        this.viewEdges = viewEdges
        val playersFocus = viewEdges?.let { edges ->
            PlayersFocus(game.players, game.cameraView, game.services.rules.viewEdges, viewEdgesOn) { edges.isHeld(it) }
        }
        // TODO: the room exits hold the players only in an upright room; in a turned room a side exit is not a left or
        //  right room edge (item 047, "Decisions", 2026-11-06)
        val roomExits = viewEdges?.let { edges ->
            CameraRoomExits(game.players, roomBounds, {
                player.inPlay && !roomTransition.transitioning && !game.cameraView.rotating &&
                    game.cameraRotation.direction == Direction.UP
            }) { edges.isHeld(it) }
        }
        if (playersFocus != null) {
            game.cameraController.addEffect(playersFocus)
            game.cameraController.addEffect(viewEdges)
            game.cameraController.addEffect(roomExits!!)
        }
        this.playersFocus = playersFocus
        this.roomExits = roomExits
        game.cameraController.reset()

        roomHandler = LevelRoomHandler(game.services, roomBounds, roomTransition, { player }) {
            game.getSystem(BehaviorsSystem::class)?.on = it
        }

        pauseScreen.init()

        EntityFactories.init()

        levelPointsText = MegaFontHandle(
            { "POINTS: " + game.levelState.levelPoints.toString() },
            positionX = (ConstVals.VIEW_WIDTH - 0.5f) * ConstVals.PPM,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )
        levelPointsText.attachment = Position.TOP_RIGHT

        coinsText = MegaFontHandle(
            { "COINS: " + game.levelState.coins.toString() },
            positionX = 0.5f * ConstVals.PPM,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )
        coinsText.attachment = Position.TOP_LEFT
    }

    override fun start(tmxMapSource: String) {
        if (!initialized) throw IllegalStateException("must call init() before start()")
        cameraFollow.setFocus(gameCamera, playersFocus ?: player.focus)
        super.start(tmxMapSource)

        game.resetAutoPerfGracePeriod()

        eventsMan.addListener(this)
        engine.systems.forEach { it.on = true }
        music?.let { audioMan.playMusic(it, it.loop) }

        game.levelState.roomsSupplier = { roomBounds.gameRooms }
        game.levelState.currentRoomSupplier = { roomBounds.currentGameRoom }

        if (tiledMapLoadResult == null) throw IllegalStateException("no tiled map load result found in level screen")
        game.levelState.tiledMapLoadResult = tiledMapLoadResult!!

        val worldContainer: IWorldContainer

        when (game.params.worldContainer) {
            "simple" -> worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
            "quadtree" -> {
                val tmr = tiledMapLoadResult!!
                val worldWidthPx = (tmr.worldWidth * tmr.tileWidth).toFloat()
                val worldHeightPx = (tmr.worldHeight * tmr.tileHeight).toFloat()

                // Each quadtree subdivision halves both dimensions simultaneously, so after d levels the
                // tight node size is (worldWidth / 2^d) x (worldHeight / 2^d) in tiles. We want the
                // smallest of those two dimensions to be >= 1 tile, which gives:
                //   min(worldWidth, worldHeight) / 2^maxDepth >= 1
                //   maxDepth <= log2(min(worldWidth, worldHeight))
                // We take the floor via the bit trick: floor(log2(n)) = 31 - numberOfLeadingZeros(n).
                // Example: a 600x150 tile map gives floor(log2(150)) = 7, so leaf tight nodes are
                // ~4.7 x 1.2 tiles and, with loosenessFactor=2, loose nodes are ~9.4 x 2.3 tiles.
                val minDimTiles = minOf(tmr.worldWidth, tmr.worldHeight)
                val maxDepth = 31 - Integer.numberOfLeadingZeros(minDimTiles)

                worldContainer = LooseQuadtreeWorldContainer(
                    ppm = tmr.tileWidth,
                    worldMinX = 0f,
                    worldMinY = 0f,
                    worldMaxX = worldWidthPx,
                    worldMaxY = worldHeightPx,
                    maxDepth = maxDepth,
                    loosenessFactor = 1.5f
                )
            }

            else -> throw IllegalArgumentException("Invalid world container type: ${game.params.worldContainer}")
        }

        game.levelState.worldContainer = worldContainer

        respawn.startAtRespawn()
        deathFade.setToEnd()

        endLevelEventHandler.reset()

        cameraFollow.stopSmoothFollow()
        camerasSetToGameCamera = false
        gameCameraPriorPosition.setZero()
        gameCamera.position.set(ConstFuncs.getGameCamInitPos())
        uiCamera.position.set(ConstFuncs.getGameCamInitPos())

        cameraFollow.minX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
        cameraFollow.maxX = tiledMapLoadResult!!.worldWidth * ConstVals.PPM.toFloat()

        GameLogger.debug(
            TAG,
            "start(): set camera follow min and max: " +
                "${cameraFollow.minX}, ${cameraFollow.maxX}"
        )

        /*
        if (DISPLAY_ROOMS_DEBUG_TEXT) {
            val roomsTextSupplier: () -> String = {
                "current=${roomBounds.currentGameRoom?.name} / " +
                    "prior=${roomBounds.priorGameRoom?.name}"
            }
            game.setDebugTextSupplier(roomsTextSupplier)
        }
         */

        game.levelState.coins = 0
        game.levelState.levelPoints = 0
        game.levelState.timeSinceLastFloatingPoints = 0f
    }

    override fun getLayerBuilders() =
        MegaMapLayerBuilders(MegaMapLayerBuildersParams(game, spawnCameraBounds))

    override fun buildLevel(result: Properties) {
        backgrounds = result.get(ConstKeys.BACKGROUNDS) as Array<Background>? ?: Array()
        backgrounds.forEach {
            game.events.addListener(it)
            disposables.add { game.events.removeListener(it) }
        }
        backgroundsToHide =
            result.get("${ConstKeys.HIDDEN}_${ConstKeys.BACKGROUNDS}") as ObjectSet<String>? ?: ObjectSet()

        val playerSpawns =
            result.get("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}") as Array<RectangleMapObject>? ?: Array()
        playerCheckpointMan.set(playerSpawns)

        val gameRooms = result.get(ConstKeys.GAME_ROOMS) as Array<RectangleMapObject>? ?: Array()
        roomBounds.gameRooms = gameRooms
        roomExits?.gates?.let { gates ->
            gates.clear()
            tiledMapLoadResult!!.map.layers.get(ConstKeys.SENSORS)?.objects?.forEach {
                if (it.name == Gate.TAG && it is RectangleMapObject) gates.add(Rectangle(it.rectangle))
            }
        }
        @Suppress("UNCHECKED_CAST")
        val zones = result.get(ConstKeys.CAMERA_SECTIONS) as Array<CameraZone>?
        cameraZones.zones = zones ?: Array()
        @Suppress("UNCHECKED_CAST")
        val scrolls = result.get(ConstKeys.CAMERA_SCROLLS) as Array<CameraScrollSection>?
        cameraScroll.sections = scrolls ?: Array()

        game.consumeLaunchRoom()?.let { startAtLaunchRoom(it, playerSpawns, gameRooms) }

        val spawners = result.get(ConstKeys.SPAWNERS) as Array<ISpawner>? ?: Array()
        spawnsMan.setSpawners(spawners)

        val disposables = result.get(ConstKeys.DISPOSABLES) as Array<Disposable>? ?: Array()
        this.disposables.addAll(disposables)
    }

    private fun startAtLaunchRoom(
        roomName: String,
        playerSpawns: Array<RectangleMapObject>,
        gameRooms: Array<RectangleMapObject>
    ) {
        val center = Vector2()
        fun spawnsIn(room: RectangleMapObject) = playerSpawns
            .filter { room.rectangle.contains(it.rectangle.getCenter(center)) }
            .sortedWith { o1, o2 -> o1.name.compareTo(o2.name) }

        val room = gameRooms.firstOrNull { it.name == roomName }
        val spawn = room?.let { spawnsIn(it).firstOrNull() }
        if (spawn == null) {
            val roomsWithSpawns = gameRooms.filter { spawnsIn(it).isNotEmpty() }.map { it.name }
            throw IllegalStateException(
                "Launch option --room: no player spawn in room '$roomName'. " +
                    "Rooms with player spawns: ${roomsWithSpawns.joinToString(", ")}"
            )
        }

        GameLogger.log(TAG, "startAtLaunchRoom(): room=$roomName, spawn=${spawn.name}")
        playerCheckpointMan.startAt(spawn)
    }

    private fun roomHasCameraSection(): Boolean {
        val room = roomBounds.currentGameRoom?.rectangle ?: return false
        return cameraZones.zones.any { it.bounds.overlaps(room) } || cameraScroll.sections.any { it.bounds.overlaps(room) }
    }

    private fun resetLevelForRespawn() {
        GameLogger.debug(TAG, "resetLevelForRespawn(): reset the room bounds and spawn the players")

        backgrounds.forEach { background -> background.immediateRotation(Direction.UP) }
        game.cameraController.reset()

        fights.cancel()
        bossHealthHandler.unset()

        engine.systems.forEach { it.on = true }

        cameraFollow.stopSmoothFollow()
        game.levelState.roomTransition = false

        MegaGameEntities.getOfType(EntityType.ENEMY).forEach { it.destroy() }
        spawnsMan.rearm()

        val spawnProps =
            playerCheckpointMan.currentSpawnProps ?: throw IllegalStateException("Megaman spawn props are null")
        GameLogger.debug(TAG, "resetLevelForRespawn(): spawn Megaman: $spawnProps")
        engine.spawn(player, spawnProps)
        for (i in 1 until game.players.capacity) {
            val slot = PlayerSlot(i)
            game.players.get(slot)?.let { engine.spawn(it, slotSpawnProps(slot, spawnProps)) }
        }

        if (!cameraShake.isFinished) cameraShake.stop()
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

            EventType.PLAYER_JUST_DIED -> {
                GameLogger.debug(
                    MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): player just died --> start the respawn routine"
                )

                // TODO: the death of slot 0 resets the level for all players, and any other death changes nothing until
                //  that reset. This must become configurable with two more options: the level does not reset until all
                //  slots die; or players respawn at once until all lives are gone, with no level reset (co-op only).
                //  See "Decisions" in docs/mega-refactor/items/047-co-op/ITEM.md
                val slot = event.getProperty(PlayerEventKeys.SLOT, PlayerSlot::class)!!
                if (slot != PlayerSlot(0)) return

                respawn.startAtDeath()
            }

            EventType.ADD_WEAPON_ENERGY -> {
                val ammo = event.getProperty(ConstKeys.VALUE, Int::class)!!
                val slot = event.getProperty(PlayerEventKeys.SLOT, PlayerSlot::class)!!
                // TODO: only Megaman has weapons; another character (X, item 081) skips this until the loadout of item 024
                val player = game.players.get(slot)!!
                if (player is Megaman) playerStatsHandler.addWeaponEnergy(player, ammo)
            }

            EventType.ADD_PLAYER_HEALTH -> {
                val health = event.properties.get(ConstKeys.VALUE) as Int
                val slot = event.getProperty(PlayerEventKeys.SLOT, PlayerSlot::class)!!
                playerStatsHandler.addHealth(game.players.get(slot)!!, health)
            }

            EventType.ADD_CURRENCY -> {
                val currency = event.getProperty(ConstKeys.VALUE, Int::class)!!
                game.gameState.addCurrency(currency)
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
                val isTrans = roomBounds.transitionToRoom(gameCamera, roomName, transDirection)

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
                    AsyncPathfindingSystem::class,
                    SimplePathfindingSystem::class,
                )
                systemsToTurnOff.forEach { game.getSystem(it)?.on = false }
            }

            EventType.GATE_INIT_CLOSING -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): gate init closing")

                val systemsToTurnOn = gdxArrayOf(
                    WorldSystem::class,
                    MotionSystem::class,
                    BehaviorsSystem::class,
                    SimplePathfindingSystem::class,
                    AsyncPathfindingSystem::class
                )
                systemsToTurnOn.forEach { game.getSystem(it)?.on = true }
            }

            EventType.ENTER_BOSS_ROOM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): enter boss room")

                val bossRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!
                val bossMapObject = bossRoom.properties.get(ConstKeys.OBJECT, RectangleMapObject::class.java)
                val bossSpawnProps = bossMapObject.properties.toProps()

                val plan = FightPlans.find(bossMapObject.name, bossSpawnProps)

                // if this fight ends the level, then check if the victory event should be triggered (when the
                // boss is already defeated)
                if (plan.endsLevel && game.gameState.isLevelDefeated(game.levelState.currentLevel!!)) {
                    eventsMan.submitEvent(Event(EventType.VICTORY_EVENT))
                    return
                }

                bossSpawnProps.put(ConstKeys.BOUNDS, bossMapObject.rectangle.toGameRectangle())

                FightPlans.resolve(plan, bossSpawnProps)

                val boss = bossSpawnEventHandler.init(bossMapObject.name, bossSpawnProps)
                fights.start(boss, plan, game.services, bossHealthHandler) { music }
            }

            EventType.VICTORY_EVENT -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): victory event")

                endLevelEventHandler.init()
            }

            EventType.SHAKE_CAM -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): request to shake game cam")
                if (cameraShake.isFinished) {
                    val duration = event.properties.get(ConstKeys.DURATION, Float::class)!!
                    val interval = event.properties.get(ConstKeys.INTERVAL, Float::class)!!
                    val shakeX = event.properties.get(ConstKeys.X, Float::class)!!
                    val shakeY = event.properties.get(ConstKeys.Y, Float::class)!!
                    cameraShake.startShake(duration, interval, shakeX, shakeY)
                }
            }

            EventType.END_LEVEL -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): end level")

                eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))

                val level = game.levelState.currentLevel!!
                if (level.type != LevelType.EXTRA_LEVEL) game.gameState.addLevelDefeated(level)

                val nextScreen = screenOnCompletion.invoke(game.levelState)
                game.setCurrentScreen(nextScreen.name)
            }

            EventType.GAME_COMPLETE -> {
                GameLogger.debug(MEGA_LEVEL_SCREEN_EVENT_LISTENER_TAG, "onEvent(): game complete")

                game.setCurrentScreen(ScreenEnum.ENDING_SCREEN.name)
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
                game.cameraRotation.start(direction, ConstVals.GAME_CAM_ROTATE_TIME)
                backgrounds.forEach { background ->
                    background.startRotation(
                        direction,
                        ConstVals.GAME_CAM_ROTATE_TIME
                    )
                }
            }
        }
    }

    override fun render(delta: Float) {
        /*
        game.setDebugText(
            "${roomTransition.transitioning} / " +
                "${roomBounds.currentGameRoom?.name} / " +
                "${roomBounds.priorGameRoom?.name}"
        )
         */

        // do not allow pausing if Megaman is dead
        if (isPauseRequested(
                paused = game.clock.paused,
                alive = player.existenceState.liveAfterUpdate,
                inPlay = player.inPlay,
                locked = game.inputLocked,
                playerFrame = player.input,
                // TODO: Esc is pause in gameplay for now; consider another action for Esc in gameplay later (031
                //  decision of 2026-10-25)
                backStatus = game.systemInput.getStatus(SystemButton.BACK),
                // TODO: SELECT is always X's dash for now, so it never pauses for X; see the dash input TODO in
                //  MegamanXMoves.kt (item 081)
                selectAction = if (player is MegamanX) SelectButtonAction.NEXT_WEAPON
                else game.players.settings(player.playerSlot!!).selectButtonAction
            )
        ) {
            GameLogger.debug(TAG, "render(): pause game")
            queuePause(game.runQueue, { game.inputLocked }, { game.pause() })
        }

        // force resume game if megaman is dead
        if (game.clock.paused && !player.existenceState.liveAfterUpdate) game.runQueue.addLast { game.resume() }

        playerStatsHandler.update(delta)

        if (!game.clock.paused) {
            if (endLevelEventHandler.finished) respawn.update(delta)

            spawnCameraBounds.set(game.cameraView.getRotatedBounds())
            spawnsMan.update(delta)

            fights.update(delta)

            if (!endLevelEventHandler.finished) endLevelEventHandler.update(delta)

            if (player.existenceState.liveAfterUpdate) deathFade.setInactive()
        }

        engine.update(delta)

        if (!game.clock.paused) {
            playerCheckpointMan.run()

            backgrounds.forEach { it.update(delta) }
            game.cameraController.update(delta)

            val timeSinceLastFloatingPoints = game.levelState.timeSinceLastFloatingPoints
            game.levelState.timeSinceLastFloatingPoints = timeSinceLastFloatingPoints + delta
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

        if (game.clock.paused) pauseScreen.render(delta)

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
            val backgroundSprite = backgroundSprites.poll()!!
            backgroundSprite.draw(drawer)
        }

        tiledMapLevelRenderer?.render(gameCamera)

        val playgroundSprites = drawables.get(DrawingSection.PLAYGROUND)
        while (!playgroundSprites.isEmpty()) {
            val playgroundSprite = playgroundSprites.poll()!!
            playgroundSprite.draw(drawer)
        }

        val foregroundSprites = drawables.get(DrawingSection.FOREGROUND)
        while (!foregroundSprites.isEmpty()) {
            val foregroundSprite = foregroundSprites.poll()!!
            foregroundSprite.draw(drawer)
        }

        game.viewports.get(ConstKeys.UI).apply()
        drawer.projectionMatrix = uiCamera.combined

        if (game.levelState.currentLevel!!.type == LevelType.MARIO_LEVEL) {
            levelPointsText.draw(batch)
            coinsText.draw(batch)
        }

        bossHealthHandler.draw(drawer)
        playerStatsHandler.draw(drawer)

        if (!beamIn.finished) beamIn.draw(drawer)

        if (!player.existenceState.liveAfterUpdate) deathFade.draw(drawer)

        if (game.clock.paused) pauseScreen.draw(drawer)

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

        if (DRAW_CAMERA_SECTIONS) {
            renderer.set(ShapeType.Line)

            renderer.color = Color.GREEN
            cameraScroll.sections.forEach { renderer.rect(it.bounds.x, it.bounds.y, it.bounds.width, it.bounds.height) }

            renderer.color = Color.CYAN
            cameraZones.zones.forEach { renderer.rect(it.bounds.x, it.bounds.y, it.bounds.width, it.bounds.height) }

            renderer.color = Color.RED
            cameraScroll.sections.forEach { section ->
                section.crushSurfaces.forEach { renderer.rect(it.getX(), it.getY(), it.getWidth(), it.getHeight()) }
            }
            cameraZones.zones.forEach { zone ->
                zone.crushSurfaces.forEach { renderer.rect(it.getX(), it.getY(), it.getWidth(), it.getHeight()) }
            }
        }

        viewEdges?.let {
            renderer.set(ShapeType.Line)
            it.drawWarpGuides(renderer)
        }

        if (game.params.debugShapes) {
            val gameCamBounds = game.cameraView.getRotatedBounds()
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

        fights.cancel()
        engine.reset()

        spawnsMan.reset()
        playerCheckpointMan.reset()

        respawn.cancel()
        beamIn.reset()
        deathFade.setToEnd()

        endLevelEventHandler.reset()

        backgrounds.forEach { background -> background.reset() }

        game.cameraController.reset()
        game.cameraRotation.snap(Direction.UP)
        game.cameraController.applyOrientation()

        audioMan.unsetMusic()

        game.levelState.roomTransition = false
        eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))

        cameraFollow.stopSmoothFollow()
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
