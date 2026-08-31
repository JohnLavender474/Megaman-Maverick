package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
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
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.reflect.KClass

class DarknessV2(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener,
    IGameShapeOverlappable {

    companion object {
        const val TAG = "DarknessV2"

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
            Life::class pairTo { STANDARD_LIGHT_SOURCE },
            ReactorManProjectile::class pairTo { STANDARD_LIGHT_SOURCE }
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
    }

    // bounds is intentionally not stored here anymore: nothing outside handleLightSource ever needs a tile's
    // rectangle, and handleLightSource computes tile extents arithmetically from (x, y) instead. stamp is a frame
    // generation counter: a tile whose stamp is not exactly one behind the entity's current frame was not visited
    // last tick (it either just came on screen, or this is its first visit ever), so it should snap to darkMode
    // rather than smoothly fade from whatever currentAlpha it happens to hold.
    private class BlackTile(var currentAlpha: Float = MAX_ALPHA) {

        var stamp = -1

        fun update(delta: Float, darken: Boolean) {
            currentAlpha += (if (darken) abs(DARKEN_STEP_SCALAR) else -abs(LIGHTEN_STEP_SCALAR)) * delta
            currentAlpha = currentAlpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
        }

        fun reset(dark: Boolean) {
            currentAlpha = if (dark) MAX_ALPHA else MIN_ALPHA
        }
    }

    // a plain (identity-hashed) class rather than a data class: it is pooled, and a mutable data class's value-based
    // hashCode would move around as the instance's fields are mutated, which is exactly the hazard Pool's identity
    // map (see engine's Pool.kt) is meant to avoid. center is a fixed Vector2 that callers .set() into rather than
    // reassign, so this def never holds a reference into GameObjectPools' reclaimable Vector2 pool.
    private class LightSourceDef {
        val center = Vector2()
        var radius = 0
        var radiance = 0f

        override fun toString() = "LightSourceDef(center=$center, radius=$radius, radiance=$radiance)"
    }

    // The single sprite submitted to SpritesSystem each frame. It paints the whole visible tile window itself in
    // one draw() call instead of one GameSprite per tile, so SpritesSystem only ever has one drawable from this
    // entity to push through the priority queue. It stays inside the ordinary SpritesComponent (rather than the
    // entity submitting itself to the draw queue from its UpdatablesComponent) specifically so that it keeps
    // rendering, frozen at its last computed alphas, on the frames where UpdatablesSystem is turned off but
    // SpritesSystem is not - pause, heart/health tank pickups, health refills, and the boss-spawn health bar fill
    // all do this (see LevelStateHandler, PlayerStatsHandler, MegaLevelScreen). An inner class rather than a
    // standalone one so it can read allTiles/bounds/dividedPPM/region live off the enclosing entity without any
    // per-frame or per-spawn field syncing.
    private inner class DarknessTileGrid : GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 5)) {

        // the tile window to draw, recomputed once per updatable tick
        var minX = 0
        var maxX = -1
        var minY = 0
        var maxY = -1

        override fun draw(drawer: Batch) {
            if (hidden) return

            val prevColor = drawer.packedColor

            for (x in minX..maxX) for (y in minY..maxY) {
                val tile = allTiles[x, y] ?: continue
                val alpha = tile.currentAlpha
                if (alpha <= MIN_ALPHA) continue

                drawer.setColor(0f, 0f, 0f, alpha)
                drawer.draw(
                    region!!,
                    bounds.getX() + x * dividedPPM,
                    bounds.getY() + y * dividedPPM,
                    dividedPPM,
                    dividedPPM
                )
            }

            drawer.packedColor = prevColor
        }
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_READY,
        EventType.BEGIN_ROOM_TRANS,
        EventType.SET_TO_ROOM_NO_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.ADD_LIGHT_SOURCE
    )

    var key = -1
        private set

    private val rooms = ObjectSet<String>()
    private val lightSourceQueue = Array<LightSourceDef>()

    private lateinit var allTiles: Matrix<BlackTile>
    private lateinit var grid: DarknessTileGrid
    private lateinit var lightSourcePool: Pool<LightSourceDef>

    private val bounds = GameRectangle()

    private var dividedPPM = 0f
    private var darkMode = false

    // true if any tile in last frame's visible window was above MIN_ALPHA. Together with darkMode this drives the
    // "nothing to draw" early-out: while a room is not dark, tiles only ever trend toward MIN_ALPHA (see
    // BlackTile.update), so once every visible tile has reached it, no further per-frame work can change what's on
    // screen until darkMode flips true again - which is always re-checked at the top of every tick regardless of
    // whether this flag is skipping work.
    private var anyTileLit = false

    // frame generation counter for BlackTile.stamp; incremented only when the tile walk actually runs
    private var frame = 0

    private val reusableCircle = GameCircle()

    private val reusableEntitiesSet = ObjectSet<MegaGameEntity>()
    private val reusableMnMs = MinsAndMaxes()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        super.init()

        grid = DarknessTileGrid()
        grid.hidden = true
        addComponent(SpritesComponent(grid))

        addComponent(defineUpdatablesComponent())

        lightSourcePool = Pool(startAmount = 0, supplier = { LightSourceDef() })
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
        anyTileLit = false
        frame = 0

        grid.hidden = true
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        rooms.clear()
        allTiles.clear()

        drainLightSourceQueue()

        grid.hidden = true
    }

    override fun onEvent(event: Event) {
        if (GameLogger.tagsToLog.contains(TAG)) GameLogger.debug(TAG, "onEvent(): event=$event")

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
                    if (GameLogger.tagsToLog.contains(TAG))
                        GameLogger.debug(TAG, "onEvent(): ADD_LIGHT_SOURCE: keys=$key")

                    val center = event.getProperty(ConstKeys.CENTER, Vector2::class)!!
                    val radius = event.getProperty(ConstKeys.RADIUS, Int::class)!!

                    reusableCircle.setRadius(radius.toFloat()).setCenter(center)
                    if (!reusableCircle.overlaps(game.getGameCamera().getRotatedBounds())) return

                    val radiance = event.getProperty(ConstKeys.RADIANCE, Float::class)!!

                    val lightSourceDef = lightSourcePool.fetch()
                    lightSourceDef.center.set(center)
                    lightSourceDef.radius = radius
                    lightSourceDef.radiance = radiance

                    lightSourceQueue.add(lightSourceDef)

                    // TODO: If MAX is specified and adding new light source exceeds MAX,
                    //  then remove oldest element here...
                }
            }
        }
    }

    private fun getMinsAndMaxes(rect: GameRectangle): MinsAndMaxes {
        val minX = ((rect.getX() - bounds.getX()) / dividedPPM).toInt().coerceIn(0, allTiles.columns - 1)
        val minY = ((rect.getY() - bounds.getY()) / dividedPPM).toInt().coerceIn(0, allTiles.rows - 1)
        val maxX = (ceil((rect.getMaxX() - bounds.getX()) / dividedPPM)).toInt().coerceIn(0, allTiles.columns - 1)
        val maxY = (ceil((rect.getMaxY() - bounds.getY()) / dividedPPM)).toInt().coerceIn(0, allTiles.rows - 1)
        return reusableMnMs.set(minX, minY, maxX, maxY)
    }

    private fun getTile(x: Int, y: Int): BlackTile {
        var tile = allTiles[x, y]
        if (tile == null) {
            tile = BlackTile()
            allTiles[x, y] = tile
        }
        return tile
    }

    private fun tryToLightUp(entity: IGameEntity) {
        if (entity is IBodyEntity &&
            entity.body.getBounds().overlaps(bounds) &&
            LIGHT_UP_ENTITIES.containsKey(entity::class)
        ) {
            val lightSourceDef = lightSourcePool.fetch()

            LIGHT_UP_ENTITIES[entity::class].invoke(entity).let { (first, second) ->
                // .set() into the def's own Vector2 rather than storing the pooled reference directly, so the
                // pooled vector returned by getCenter() can be reclaimed normally instead of leaking
                lightSourceDef.center.set(entity.body.getCenter())
                lightSourceDef.radius = first * ConstVals.PPM
                lightSourceDef.radiance = second
            }

            lightSourceQueue.add(lightSourceDef)
        }
    }

    // rewritten to do plain arithmetic instead of allocating a GameCircle/GameRectangle overlap test and a pooled
    // Vector2 per candidate tile. The membership test is the same closest-point-on-rect-to-circle-center check
    // Intersector.overlaps(Circle, Rectangle) performs, just inlined; dx is hoisted per column so a whole column of
    // tiles can be skipped without ever touching y.
    private fun handleLightSource(lightSourceDef: LightSourceDef) {
        val startTime = System.currentTimeMillis()

        val center = lightSourceDef.center
        val radius = lightSourceDef.radius
        val radiance = lightSourceDef.radiance

        val radiusF = radius.toFloat()
        val radiusSq = radiusF * radiusF
        val alphaScalar = 1f / (radiusF * radiance)

        val minX = (((center.x - radiusF) - bounds.getX()) / dividedPPM).toInt().coerceIn(0, allTiles.columns - 1)
        val minY = (((center.y - radiusF) - bounds.getY()) / dividedPPM).toInt().coerceIn(0, allTiles.rows - 1)
        val maxX =
            (ceil(((center.x + radiusF) - bounds.getX()) / dividedPPM)).toInt().coerceIn(0, allTiles.columns - 1)
        val maxY =
            (ceil(((center.y + radiusF) - bounds.getY()) / dividedPPM)).toInt().coerceIn(0, allTiles.rows - 1)

        for (x in minX..maxX) {
            val tileMinX = bounds.getX() + x * dividedPPM
            val tileMaxX = tileMinX + dividedPPM

            val closestX = center.x.coerceIn(tileMinX, tileMaxX)
            val dx = center.x - closestX
            val dxSq = dx * dx
            if (dxSq > radiusSq) continue

            for (y in minY..maxY) {
                val tileMinY = bounds.getY() + y * dividedPPM
                val tileMaxY = tileMinY + dividedPPM

                val closestY = center.y.coerceIn(tileMinY, tileMaxY)
                val dy = center.y - closestY
                if (dxSq + dy * dy > radiusSq) continue

                val tile = getTile(x, y)

                // alpha is based on distance from the light's center to the TILE's center, which is a different
                // (larger) distance than the closest-point overlap test above - that distinction is preserved from
                // the original implementation
                val tileCenterX = tileMinX + dividedPPM * 0.5f
                val tileCenterY = tileMinY + dividedPPM * 0.5f
                val cdx = tileCenterX - center.x
                val cdy = tileCenterY - center.y
                val dist = sqrt(cdx * cdx + cdy * cdy)

                val alpha = (dist * alphaScalar).coerceIn(MIN_ALPHA, MAX_ALPHA)
                tile.currentAlpha = min(alpha, tile.currentAlpha)
            }
        }

        debugTime(startTime) {
            "update(): updating light source took too long: " +
                "time=$it, lightSource=$lightSourceDef, minX=$minX, minY=$minY, maxX=$maxX, maxY=$maxY"
        }
    }

    private fun drainLightSourceQueue() {
        for (i in 0 until lightSourceQueue.size) lightSourcePool.free(lightSourceQueue[i])
        lightSourceQueue.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val camBounds = game.getGameCamera().getRotatedBounds()
        camBounds.translate(-CAM_BOUNDS_BUFFER * ConstVals.PPM, -CAM_BOUNDS_BUFFER * ConstVals.PPM)
        camBounds.translateSize(2f * CAM_BOUNDS_BUFFER * ConstVals.PPM, 2f * CAM_BOUNDS_BUFFER * ConstVals.PPM)

        // nothing to draw: either the camera isn't even looking at this darkness's bounds, or the room is lit and
        // every tile that was on screen last frame has already decayed to MIN_ALPHA. Either way, skip the entity
        // sweep, light collection, and tile walk entirely - but still drain and free whatever light source events
        // arrived via onEvent this frame (from entities unrelated to darkMode, e.g. ambient energy items), so the
        // queue and the pool it draws from can't grow unboundedly while skipped.
        if (!camBounds.overlaps(bounds) || (!darkMode && !anyTileLit)) {
            drainLightSourceQueue()
            grid.hidden = true
            return@UpdatablesComponent
        }

        val entities = MegaGameEntities.getOfTypes(reusableEntitiesSet, LIGHT_UP_ENTITY_TYPES)
        entities.forEach { entity -> tryToLightUp(entity) }
        entities.clear()

        if (megaman.ready && megaman.body.getBounds().overlaps(bounds)) {
            val lightSourceDef = lightSourcePool.fetch()
            lightSourceDef.center.set(megaman.body.getCenter())

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

            lightSourceQueue.add(lightSourceDef)
        }

        val beaming = game.isProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)
        if (beaming) {
            val beamCenter = game.getProperty("${Megaman.TAG}_${ConstKeys.BEAM}_${ConstKeys.CENTER}", Vector2::class)

            if (beamCenter != null) {
                val lightSourceDef = lightSourcePool.fetch()

                lightSourceDef.center.set(beamCenter)
                lightSourceDef.radius = MEGAMAN_FULL_CHARGING_RADIUS * ConstVals.PPM
                lightSourceDef.radiance = MEGAMAN_FULL_CHARGING_RADIANCE

                lightSourceQueue.add(lightSourceDef)
            }
        }

        for (i in 0 until lightSourceQueue.size) handleLightSource(lightSourceQueue[i])
        drainLightSourceQueue()

        val (minX, minY, maxX, maxY) = getMinsAndMaxes(camBounds)

        frame++
        var anyLit = false

        for (x in minX..maxX) for (y in minY..maxY) {
            val tile = getTile(x, y)

            if (tile.stamp != frame - 1) tile.reset(darkMode) else tile.update(delta, darkMode)
            tile.stamp = frame

            if (tile.currentAlpha > MIN_ALPHA) anyLit = true
        }

        anyTileLit = anyLit

        grid.minX = minX
        grid.maxX = maxX
        grid.minY = minY
        grid.maxY = maxY
        grid.hidden = false
    })

    override fun overlaps(shape: IGameShape2D) = this.bounds.overlaps(shape)

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
