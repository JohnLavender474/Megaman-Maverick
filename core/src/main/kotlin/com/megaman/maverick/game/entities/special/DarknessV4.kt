package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedSetOf
import com.mega.game.engine.common.objects.*
import com.mega.game.engine.common.shapes.*
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.PicketJoe
import com.megaman.maverick.game.entities.enemies.ShieldAttacker
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.items.Life
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KClass

class DarknessV4(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener,
    IGameShapeOverlappable {

    companion object {
        const val TAG = "DarknessV4"

        const val MIN_ALPHA = 0f
        const val MAX_ALPHA = 1f

        private const val DARKEN_STEP_SCALAR = 2f
        private const val LIGHTEN_STEP_SCALAR = 1f

        private const val CAM_BOUNDS_BUFFER = 2f

        private const val DEFAULT_PPM_DIVISOR = 2

        private const val MEGAMAN_HALF_CHARGING_RADIUS = 3
        private const val MEGAMAN_HALF_CHARGING_RADIANCE = 1.25f
        private const val MEGAMAN_FULL_CHARGING_RADIUS = 4
        private const val MEGAMAN_FULL_CHARGING_RADIANCE = 1.5f

        private var region: TextureRegion? = null

        private val STANDARD_LIGHT_SOURCE = GamePair.of(2, 1.5f)
        private val BRIGHTER_LIGHT_SOURCE = GamePair.of(3, 2f)
        private val BRIGHTEST_LIGHT_SOURCE = GamePair.of(4, 2.5f)

        private val LIGHT_UP_ENTITIES = objectMapOf<KClass<out IBodyEntity>, (IBodyEntity) -> GamePair<Int, Float>>(
            Megaman::class pairTo { STANDARD_LIGHT_SOURCE },
            Bullet::class pairTo { STANDARD_LIGHT_SOURCE },
            ChargedShot::class pairTo {
                it as ChargedShot
                if (it.fullyCharged) BRIGHTEST_LIGHT_SOURCE else BRIGHTER_LIGHT_SOURCE
            },
            ChargedShotExplosion::class pairTo {
                it as ChargedShotExplosion
                if (it.fullyCharged) BRIGHTEST_LIGHT_SOURCE else BRIGHTER_LIGHT_SOURCE
            },
            MoonScythe::class pairTo { BRIGHTEST_LIGHT_SOURCE },
            MagmaWave::class pairTo { BRIGHTEST_LIGHT_SOURCE },
            MagmaFlame::class pairTo { BRIGHTEST_LIGHT_SOURCE },
            Fireball::class pairTo { BRIGHTER_LIGHT_SOURCE },
            DuoBall::class pairTo { STANDARD_LIGHT_SOURCE },
            ArigockBall::class pairTo { STANDARD_LIGHT_SOURCE },
            CactusMissile::class pairTo { BRIGHTER_LIGHT_SOURCE },
            Explosion::class pairTo { BRIGHTER_LIGHT_SOURCE },
            ExplosionOrb::class pairTo { STANDARD_LIGHT_SOURCE },
            ShieldAttacker::class pairTo { BRIGHTER_LIGHT_SOURCE },
            SpreadExplosion::class pairTo { BRIGHTEST_LIGHT_SOURCE },
            PicketJoe::class pairTo { BRIGHTER_LIGHT_SOURCE },
            GreenPelletBlast::class pairTo { STANDARD_LIGHT_SOURCE },
            SlashWave::class pairTo { STANDARD_LIGHT_SOURCE },
            Life::class pairTo { STANDARD_LIGHT_SOURCE }
        )
        private val LIGHT_UP_ENTITY_TYPES = orderedSetOf(
            EntityType.PROJECTILE, EntityType.EXPLOSION, EntityType.ENEMY, EntityType.HAZARD
        )

        private const val DEBUG_THRESHOLD_SECS = 0.025f

        private fun debugTime(start: Long, messageOnTooLong: (Float) -> String) {
            val end = System.currentTimeMillis()
            val totalTime = (end - start) / 1000f
            if (totalTime > DEBUG_THRESHOLD_SECS) GameLogger.debug(TAG, messageOnTooLong.invoke(totalTime))
        }

        private const val TIME_TO_UPDATE = 0.25f
    }

    // ========================================
    // Data classes for thread-safe processing
    // ========================================

    /**
     * Immutable input data for background thread processing.
     * Contains all data needed to calculate light without accessing game objects.
     */
    private data class LightProcessingInput(
        val lightSources: List<LightSourceData>,
        val camBoundsMinX: Int,
        val camBoundsMinY: Int,
        val camBoundsMaxX: Int,
        val camBoundsMaxY: Int,
        val boundsX: Float,
        val boundsY: Float,
        val boundsWidth: Float,
        val boundsHeight: Float,
        val dividedPPM: Float,
        val darkMode: Boolean,
        val rows: Int,
        val columns: Int,
        val previousTileCoords: Set<Pair<Int, Int>>,
        val delta: Float
    )

    /**
     * Immutable light source data extracted from game entities.
     */
    private data class LightSourceData(
        val centerX: Float,
        val centerY: Float,
        val radius: Int,
        val radiance: Float
    )

    /**
     * Result of tile alpha calculation from background thread.
     */
    private data class TileAlphaResult(
        val x: Int,
        val y: Int,
        val alpha: Float,
        val wasInPrevious: Boolean
    )

    /**
     * Complete result set from background thread processing.
     */
    private data class LightProcessingResult(
        val tileAlphas: List<TileAlphaResult>,
        val darkMode: Boolean,
        val delta: Float
    )

    // ========================================
    // Tile class for main thread
    // ========================================

    private class BlackTile(
        val bounds: GameRectangle,
        var currentAlpha: Float = 1f,
        var darkenStepScalar: Float = DARKEN_STEP_SCALAR,
        var lightenStepScalar: Float = LIGHTEN_STEP_SCALAR
    ) {
        fun update(delta: Float, darken: Boolean) {
            if (darken) currentAlpha += abs(darkenStepScalar) * delta
            else currentAlpha -= abs(lightenStepScalar) * delta
            currentAlpha = currentAlpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
        }

        fun reset(dark: Boolean) {
            currentAlpha = if (dark) MAX_ALPHA else MIN_ALPHA
        }
    }

    private data class LightSourceDef(var center: Vector2, var radius: Int, var radiance: Float)

    // ========================================
    // Event listener
    // ========================================

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_READY,
        EventType.BEGIN_ROOM_TRANS,
        EventType.SET_TO_ROOM_NO_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.ADD_LIGHT_SOURCE
    )

    // ========================================
    // Instance fields
    // ========================================

    var key = -1
        private set

    private val rooms = ObjectSet<String>()
    private val lightSourceQueue = OrderedMap<MegaGameEntity, LightSourceDef>()
    private val previousTiles = OrderedSet<BlackTile>()
    private val currentTiles = OrderedSet<BlackTile>()

    private lateinit var allTiles: Matrix<BlackTile>
    private lateinit var tileSpritesPool: Pool<GameSprite>
    private lateinit var lightSourcePool: Pool<LightSourceDef>

    private val bounds = GameRectangle()

    private var dividedPPM = 0f
    private var darkMode = false

    private val reusableCircle = GameCircle()

    private val reusableEntitiesSet = ObjectSet<MegaGameEntity>()
    private val reusableMnMs = MinsAndMaxes()

    private var timeSinceLastUpdate = 1f

    // ========================================
    // Threading fields
    // ========================================

    @Volatile
    private var isProcessing = false

    private var pendingResult: LightProcessingResult? = null
    private val resultLock = Any()

    // ========================================
    // Lifecycle methods
    // ========================================

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        super.init()
        addComponent(SpritesComponent())
        addComponent(defineUpdatablesComponent())
        tileSpritesPool = Pool(
            startAmount = 0,
            supplier = { GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND, 5)) },
            onFree = { sprite -> sprite.hidden = true },
            onFetch = { sprite -> sprite.hidden = false }
        )
        lightSourcePool = Pool(startAmount = 0, supplier = { LightSourceDef(Vector2(), 0, 0f) })
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        key = spawnProps.getOrDefault(ConstKeys.KEY, -1, Int::class)
        spawnProps.get(ConstKeys.ROOM, String::class)!!.split(",").forEach { t -> rooms.add(t) }
        GameLogger.debug(TAG, "onSpawn(): key=$key, rooms=$rooms")

        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)

        val ppmDivisor =
            spawnProps.getOrDefault("${ConstKeys.PPM}_${ConstKeys.DIVISOR}", DEFAULT_PPM_DIVISOR, Int::class)
        dividedPPM = ConstVals.PPM.toFloat() / ppmDivisor

        val rows = (bounds.getHeight() / dividedPPM).toInt()
        val columns = (bounds.getWidth() / dividedPPM).toInt()
        GameLogger.debug(TAG, "onSpawn(): rows=$rows, columns=$columns")
        allTiles = Matrix(rows, columns)

        darkMode = false
        isProcessing = false
        pendingResult = null

        timeSinceLastUpdate = 1f
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        rooms.clear()
        allTiles.clear()
        currentTiles.clear()
        previousTiles.clear()
        tileSpritesPool.clear()
        lightSourcePool.clear()
        lightSourceQueue.clear()

        synchronized(resultLock) {
            pendingResult = null
        }
    }

    // ========================================
    // Event handling
    // ========================================

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")

        when (event.key) {
            EventType.PLAYER_READY -> {
                val room = game.getCurrentRoom()?.name
                darkMode = if (room == null) false else rooms.contains(room)
                GameLogger.debug(TAG, "onEvent(): PLAYER_READY: darkMode=$darkMode")
            }

            EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)?.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)?.name

                if ((priorRoom == null || rooms.contains(priorRoom)) && newRoom != null && !rooms.contains(newRoom)) {
                    GameLogger.debug(
                        TAG,
                        "onEvent(): BEGIN_ROOM_TRANS/SET_TO_ROOM_NO_TRANS: light up all: " +
                            "event=$event, rooms=$rooms, newRoom=$newRoom"
                    )
                    darkMode = false
                }
            }

            EventType.END_ROOM_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)?.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)?.name

                if ((priorRoom == null || !rooms.contains(priorRoom)) && newRoom != null && rooms.contains(newRoom)) {
                    GameLogger.debug(
                        TAG,
                        "onEvent(): END_ROOM_TRANS: darken all: event=$event, rooms=$rooms, newRoom=$newRoom"
                    )
                    darkMode = true
                }
            }

            EventType.ADD_LIGHT_SOURCE -> {
                val keys = event.getProperty(ConstKeys.KEYS) as ObjectSet<Int>
                if (keys.contains(key)) {
                    GameLogger.debug(TAG, "onEvent(): ADD_LIGHT_SOURCE: keys=$key")

                    val center = event.getProperty(ConstKeys.CENTER, Vector2::class)!!
                    val radius = event.getProperty(ConstKeys.RADIUS, Int::class)!!

                    reusableCircle.setRadius(radius.toFloat()).setCenter(center)
                    if (!reusableCircle.overlaps(game.getGameCamera().getRotatedBounds())) return

                    val radiance = event.getProperty(ConstKeys.RADIANCE, Float::class)!!

                    val lightSourceDef = lightSourcePool.fetch()
                    lightSourceDef.center = center
                    lightSourceDef.radius = radius
                    lightSourceDef.radiance = radiance

                    val owner = event.getProperty(ConstKeys.OWNER, MegaGameEntity::class)

                    if (!lightSourceQueue.containsKey(owner)) lightSourceQueue.put(owner, lightSourceDef)
                }
            }
        }
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun getMinsAndMaxes(rect: GameRectangle): MinsAndMaxes {
        val minX = ((rect.getX() - bounds.getX()) / dividedPPM).toInt().coerceIn(0, allTiles.columns - 1)
        val minY = ((rect.getY() - bounds.getY()) / dividedPPM).toInt().coerceIn(0, allTiles.rows - 1)
        val maxX = (ceil((rect.getMaxX() - bounds.getX()) / dividedPPM)).toInt().coerceIn(0, allTiles.columns - 1)
        val maxY = (ceil((rect.getMaxY() - bounds.getY()) / dividedPPM)).toInt().coerceIn(0, allTiles.rows - 1)
        return reusableMnMs.set(minX, minY, maxX, maxY)
    }

    private fun getTile(x: Int, y: Int): BlackTile {
        if (allTiles[x, y] == null) {
            val posX = bounds.getX() + (x * dividedPPM)
            val posY = bounds.getY() + (y * dividedPPM)
            val tileBounds = GameRectangle(posX, posY, dividedPPM, dividedPPM)
            allTiles[x, y] = BlackTile(tileBounds)
        }
        return allTiles[x, y]!!
    }

    private fun tryToLightUp(entity: MegaGameEntity) {
        if (entity is IBodyEntity &&
            entity.body.getBounds().overlaps(bounds) &&
            LIGHT_UP_ENTITIES.containsKey(entity::class)
        ) {
            val lightSourceDef = lightSourcePool.fetch()

            LIGHT_UP_ENTITIES[entity::class].invoke(entity).let { (first, second) ->
                lightSourceDef.center = entity.body.getCenter(false)
                lightSourceDef.radius = first * ConstVals.PPM
                lightSourceDef.radiance = second
            }

            lightSourceQueue.put(entity, lightSourceDef)
        }
    }

    private fun shouldUpdate() = darkMode && timeSinceLastUpdate >= TIME_TO_UPDATE

    // ========================================
    // Background thread: Pure calculation
    // ========================================

    /**
     * Pure function that calculates tile alphas based on light sources.
     * This runs on the background thread and has NO access to game objects, sprites, or Megaman.
     * All required data is passed in via [input].
     */
    private fun calculateLightAlphas(input: LightProcessingInput): LightProcessingResult {
        val startTime = System.currentTimeMillis()

        // Map to track minimum alpha for each tile coordinate
        val tileAlphaMap = mutableMapOf<Pair<Int, Int>, Float>()

        // Initialize all visible tiles to max alpha (fully dark)
        for (x in input.camBoundsMinX..input.camBoundsMaxX) {
            for (y in input.camBoundsMinY..input.camBoundsMaxY) {
                tileAlphaMap[x to y] = MAX_ALPHA
            }
        }

        // Reusable shapes for calculations (thread-local, no shared state)
        val circle = GameCircle()
        val rect = GameRectangle()

        // Process each light source
        for (lightSource in input.lightSources) {
            val center = Vector2(lightSource.centerX, lightSource.centerY)
            val radius = lightSource.radius
            val radiance = lightSource.radiance

            circle.setRadius(radius.toFloat()).setCenter(center)
            rect.setSize(2f * radius).setCenter(center)

            // Calculate tile bounds affected by this light source
            val minX = ((rect.getX() - input.boundsX) / input.dividedPPM).toInt()
                .coerceIn(0, input.columns - 1)
            val minY = ((rect.getY() - input.boundsY) / input.dividedPPM).toInt()
                .coerceIn(0, input.rows - 1)
            val maxX = ceil((rect.getMaxX() - input.boundsX) / input.dividedPPM).toInt()
                .coerceIn(0, input.columns - 1)
            val maxY = ceil((rect.getMaxY() - input.boundsY) / input.dividedPPM).toInt()
                .coerceIn(0, input.rows - 1)

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    // Calculate tile bounds
                    val tileX = input.boundsX + (x * input.dividedPPM)
                    val tileY = input.boundsY + (y * input.dividedPPM)
                    val tileBounds = GameRectangle(tileX, tileY, input.dividedPPM, input.dividedPPM)

                    if (circle.overlaps(tileBounds)) {
                        val tileCenter = tileBounds.getCenter()
                        val alpha = ((tileCenter.dst(center) / radius) / radiance)
                            .coerceIn(MIN_ALPHA, MAX_ALPHA)

                        val currentAlpha = tileAlphaMap[x to y] ?: MAX_ALPHA
                        tileAlphaMap[x to y] = min(alpha, currentAlpha)
                    }
                }
            }
        }

        // Convert map to result list
        val results = mutableListOf<TileAlphaResult>()
        for ((coords, alpha) in tileAlphaMap) {
            val (x, y) = coords
            val wasInPrevious = input.previousTileCoords.contains(coords)
            results.add(TileAlphaResult(x, y, alpha, wasInPrevious))
        }

        debugTime(startTime) { "calculateLightAlphas(): took $it seconds" }

        return LightProcessingResult(results, input.darkMode, input.delta)
    }

    /**
     * Starts background processing of light calculations.
     * Collects all required data on the main thread, then dispatches to background thread.
     */
    private fun startBackgroundProcessing(delta: Float) {
        if (isProcessing) {
            GameLogger.debug(TAG, "startBackgroundProcessing(): already processing, skipping")
            return
        }

        val startTime = System.currentTimeMillis()

        // ========================================
        // Collect all data on main thread
        // ========================================

        // Collect light sources from entities (main thread only)
        val entities = MegaGameEntities.getOfTypes(reusableEntitiesSet, LIGHT_UP_ENTITY_TYPES)
        entities.forEach { entity -> tryToLightUp(entity) }
        entities.clear()

        // Collect Megaman light source on main thread
        if (megaman.ready && megaman.body.getBounds().overlaps(bounds)) {
            val lightSourceDef = lightSourcePool.fetch()
            lightSourceDef.center = megaman.body.getCenter()

            if (megaman.charging) {
                val fullCharged = megaman.fullyCharged
                lightSourceDef.radius =
                    (if (fullCharged) MEGAMAN_FULL_CHARGING_RADIUS else MEGAMAN_HALF_CHARGING_RADIUS) * ConstVals.PPM
                lightSourceDef.radiance =
                    if (fullCharged) MEGAMAN_FULL_CHARGING_RADIANCE else MEGAMAN_HALF_CHARGING_RADIANCE
            } else if (megaman.isBehaviorActive(BehaviorType.JETPACKING)) {
                lightSourceDef.radius = MEGAMAN_HALF_CHARGING_RADIUS * ConstVals.PPM
                lightSourceDef.radiance = MEGAMAN_HALF_CHARGING_RADIANCE
            } else {
                lightSourceDef.radius = STANDARD_LIGHT_SOURCE.first * ConstVals.PPM
                lightSourceDef.radiance = STANDARD_LIGHT_SOURCE.second
            }

            lightSourceQueue.put(megaman, lightSourceDef)
        }

        // Collect beam light source
        val beaming = game.isProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)
        if (beaming) {
            val beamCenter = game.getProperty("${Megaman.TAG}_${ConstKeys.BEAM}_${ConstKeys.CENTER}", Vector2::class)
            if (beamCenter != null) {
                val lightSourceDef = lightSourcePool.fetch()
                lightSourceDef.center = beamCenter
                lightSourceDef.radius = MEGAMAN_FULL_CHARGING_RADIUS * ConstVals.PPM
                lightSourceDef.radiance = MEGAMAN_FULL_CHARGING_RADIANCE
                lightSourceQueue.put(megaman, lightSourceDef)
            }
        }

        // Convert light source queue to immutable list of data
        val lightSourceDataList = mutableListOf<LightSourceData>()

        val lightSourceQueueIter = lightSourceQueue.entries().iterator()
        while (!lightSourceQueueIter.hasNext()) {
            val entry = lightSourceQueueIter.next()

            val lightSourceDef = entry.value

            if (lightSourceDef != null) {
                lightSourceDataList.add(
                    LightSourceData(
                        centerX = lightSourceDef.center.x,
                        centerY = lightSourceDef.center.y,
                        radius = lightSourceDef.radius,
                        radiance = lightSourceDef.radiance
                    )
                )
                lightSourcePool.free(lightSourceDef)
            }
        }
        lightSourceQueue.clear()

        // Get camera bounds
        val camBounds = game.getGameCamera().getRotatedBounds()
        camBounds.translate(-CAM_BOUNDS_BUFFER * ConstVals.PPM, -CAM_BOUNDS_BUFFER * ConstVals.PPM)
        camBounds.translateSize(2f * CAM_BOUNDS_BUFFER * ConstVals.PPM, 2f * CAM_BOUNDS_BUFFER * ConstVals.PPM)

        val (minX, minY, maxX, maxY) = getMinsAndMaxes(camBounds)

        // Collect previous tile coordinates
        val previousTileCoords = mutableSetOf<Pair<Int, Int>>()
        previousTiles.forEach { tile ->
            val tileX = ((tile.bounds.getX() - bounds.getX()) / dividedPPM).toInt()
            val tileY = ((tile.bounds.getY() - bounds.getY()) / dividedPPM).toInt()
            previousTileCoords.add(tileX to tileY)
        }

        // Create immutable input
        val input = LightProcessingInput(
            lightSources = lightSourceDataList.toList(),
            camBoundsMinX = minX,
            camBoundsMinY = minY,
            camBoundsMaxX = maxX,
            camBoundsMaxY = maxY,
            boundsX = bounds.getX(),
            boundsY = bounds.getY(),
            boundsWidth = bounds.getWidth(),
            boundsHeight = bounds.getHeight(),
            dividedPPM = dividedPPM,
            darkMode = darkMode,
            rows = allTiles.rows,
            columns = allTiles.columns,
            previousTileCoords = previousTileCoords,
            delta = delta
        )

        debugTime(startTime) { "startBackgroundProcessing(): data collection took $it seconds" }

        // ========================================
        // Dispatch to background thread
        // ========================================

        isProcessing = true

        Thread {
            try {
                val result = calculateLightAlphas(input)

                // Post result back to main thread
                Gdx.app.postRunnable {
                    synchronized(resultLock) {
                        pendingResult = result
                    }
                    isProcessing = false
                }
            } catch (e: Exception) {
                GameLogger.error(TAG, "Background processing failed: ${e.message}")
                Gdx.app.postRunnable {
                    isProcessing = false
                }
            }
        }.start()
    }

    /**
     * Applies pending results from background thread to sprites.
     * This runs on the main thread and handles all sprite manipulation.
     */
    private fun applyPendingResults() {
        val result: LightProcessingResult?

        synchronized(resultLock) {
            result = pendingResult
            pendingResult = null
        }

        if (result == null) return

        val startTime = System.currentTimeMillis()

        // Reset tiles that are no longer in camera bounds
        previousTiles.forEach { tile ->
            val tileX = ((tile.bounds.getX() - bounds.getX()) / dividedPPM).toInt()
            val tileY = ((tile.bounds.getY() - bounds.getY()) / dividedPPM).toInt()

            val stillVisible = result.tileAlphas.any { it.x == tileX && it.y == tileY }
            if (!stillVisible) {
                tile.reset(result.darkMode)
            }
        }

        previousTiles.clear()
        previousTiles.addAll(currentTiles)
        currentTiles.clear()

        // Free all existing sprites
        sprites.values().forEach { sprite -> tileSpritesPool.free(sprite) }
        sprites.clear()

        // Apply results to tiles and create sprites
        for (tileResult in result.tileAlphas) {
            val tile = getTile(tileResult.x, tileResult.y)
            currentTiles.add(tile)

            // Apply the calculated alpha, then update for animation
            if (!tileResult.wasInPrevious) {
                tile.reset(result.darkMode)
            }

            // Set the alpha from background calculation
            tile.currentAlpha = min(tileResult.alpha, tile.currentAlpha)

            // Then apply the darken/lighten animation step
            tile.update(result.delta, result.darkMode)

            // Create sprite
            val sprite = tileSpritesPool.fetch()
            sprite.setBounds(
                bounds.getX() + tileResult.x * dividedPPM,
                bounds.getY() + tileResult.y * dividedPPM,
                dividedPPM,
                dividedPPM
            )
            sprite.setAlpha(tile.currentAlpha)

            sprites.put("${tileResult.x}_${tileResult.y}", sprite)
        }

        debugTime(startTime) { "applyPendingResults(): took $it seconds" }
    }

    // ========================================
    // Update component
    // ========================================

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timeSinceLastUpdate += delta

        // Always try to apply any pending results from background thread
        applyPendingResults()

        // Check if we should start new processing
        if (!shouldUpdate()) return@UpdatablesComponent
        if (isProcessing) return@UpdatablesComponent

        timeSinceLastUpdate = 0f

        // Start background processing
        startBackgroundProcessing(delta)
    })

    // ========================================
    // Interface implementations
    // ========================================

    override fun overlaps(shape: IGameShape2D) = this.bounds.overlaps(shape)

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}

