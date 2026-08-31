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
import kotlin.reflect.KClass

class DarknessV2(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener,
    IGameShapeOverlappable {

    companion object {
        const val TAG = "DarknessV2"

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
    // standalone one so it can read the grid live off the enclosing entity without any per-frame field syncing.
    private inner class DarknessTileGrid : GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 5)) {

        override fun draw(drawer: Batch) {
            if (hidden) return

            val prevColor = drawer.packedColor
            val tileSize = grid.tileSize

            for (x in grid.minX..grid.maxX) for (y in grid.minY..grid.maxY) {
                val alpha = grid.alphaAt(x, y)
                if (alpha <= DarknessGrid.MIN_ALPHA) continue

                drawer.setColor(0f, 0f, 0f, alpha)
                drawer.draw(region!!, grid.worldX(x), grid.worldY(y), tileSize, tileSize)
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

    private val grid = DarknessGrid()

    private lateinit var sprite: DarknessTileGrid
    private lateinit var lightSourcePool: Pool<LightSourceDef>

    private val bounds = GameRectangle()

    private var darkMode = false

    // true if any tile in last frame's visible window was above MIN_ALPHA. Together with darkMode this drives the
    // "nothing to draw" early-out: while a room is not dark, tiles only ever trend toward MIN_ALPHA, so once every
    // visible tile has reached it, no further per-frame work can change what's on screen until darkMode flips true
    // again - which is always re-checked at the top of every tick regardless of whether this flag is skipping work.
    private var anyTileLit = false

    private val reusableCircle = GameCircle()

    private val reusableEntitiesSet = ObjectSet<MegaGameEntity>()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        super.init()

        sprite = DarknessTileGrid()
        sprite.hidden = true
        addComponent(SpritesComponent(sprite))

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
        val dividedPPM = ConstVals.PPM.toFloat() / ppmDivisor

        grid.reset(bounds, dividedPPM)
        GameLogger.debug(TAG, "onSpawn(): rows=${grid.rows}, columns=${grid.columns}")

        darkMode = false
        anyTileLit = false

        sprite.hidden = true
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        rooms.clear()
        grid.clear()

        drainLightSourceQueue()

        sprite.hidden = true
    }

    override fun onEvent(event: Event) {
        if (GameLogger.tagsToLog.contains(TAG)) GameLogger.debug(TAG, "onEvent(): event=$event")

        when (event.key) {
            EventType.PLAYER_READY -> {
                darkMode = darkModeOnPlayerReady(game.getCurrentRoom()?.name, rooms)
                GameLogger.debug(TAG, "onEvent(): PLAYER_READY: darkMode=$darkMode")
            }

            EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)?.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)?.name

                val updated = darkModeOnRoomTransBegin(priorRoom, newRoom, rooms, darkMode)
                if (updated != darkMode) GameLogger.debug(
                    TAG,
                    "onEvent(): BEGIN_ROOM_TRANS/SET_TO_ROOM_NO_TRANS: light up all: " +
                        "event=$event, rooms=$rooms, newRoom=$newRoom"
                )
                darkMode = updated
            }

            EventType.END_ROOM_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)?.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)?.name

                val updated = darkModeOnRoomTransEnd(priorRoom, newRoom, rooms, darkMode)
                if (updated != darkMode) GameLogger.debug(
                    TAG,
                    "onEvent(): END_ROOM_TRANS: darken all: event=$event, rooms=$rooms, newRoom=$newRoom"
                )
                darkMode = updated
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
            sprite.hidden = true
            grid.markSkipped()
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

        for (i in 0 until lightSourceQueue.size) {
            val lightSourceDef = lightSourceQueue[i]

            val startTime = System.currentTimeMillis()
            grid.applyLight(lightSourceDef.center, lightSourceDef.radius, lightSourceDef.radiance)
            debugTime(startTime) { "update(): updating light source took too long: time=$it, light=$lightSourceDef" }
        }
        drainLightSourceQueue()

        anyTileLit = grid.step(camBounds, darkMode, delta)

        sprite.hidden = false
    })

    override fun overlaps(shape: IGameShape2D) = this.bounds.overlaps(shape)

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
