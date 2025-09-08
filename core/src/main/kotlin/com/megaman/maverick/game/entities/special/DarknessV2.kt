package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.Queue
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

class DarknessV2(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener,
    IGameShapeOverlappable {

    companion object {
        const val TAG = "DarknessV2"

        const val MIN_ALPHA = 0f
        const val MAX_ALPHA = 1f

        private const val DARKNEN_STEP_SCALAR = 2f
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
            SlashWave::class pairTo { STANDARD_LIGHT_SOURCE }
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

    private class BlackTile(
        val bounds: GameRectangle,
        var currentAlpha: Float = 1f,
        var darkenStepScalar: Float = DARKNEN_STEP_SCALAR,
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
    private val lightSourceQueue = Queue<LightSourceDef>()
    private val previousTiles = OrderedSet<BlackTile>()
    private val currentTiles = OrderedSet<BlackTile>()

    private lateinit var allTiles: Matrix<BlackTile>
    private lateinit var tileSpritesPool: Pool<GameSprite>
    private lateinit var lightSourcePool: Pool<LightSourceDef>

    private val bounds = GameRectangle()

    private var dividedPPM = 0f
    private var darkMode = false

    private val reusableCircle = GameCircle()
    private val reusableRect = GameRectangle()

    private val reusableEntitiesSet = ObjectSet<MegaGameEntity>()
    private val reusableMnMs = MinsAndMaxes()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        super.init()
        addComponent(SpritesComponent())
        addComponent(defineUpdatablesComponent())
        tileSpritesPool = Pool(
            startAmount = 0,
            supplier = { GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND, 5)) },
            onFree = { sprite -> sprite.hidden = true },
            onFetch = { sprite -> sprite.hidden = false })
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
    }

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
                    lightSourceQueue.addLast(lightSourceDef)
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
        if (allTiles[x, y] == null) {
            val posX = bounds.getX() + (x * dividedPPM)
            val posY = bounds.getY() + (y * dividedPPM)
            val tileBounds = GameRectangle(posX, posY, dividedPPM, dividedPPM)
            allTiles[x, y] = BlackTile(tileBounds)
        }
        return allTiles[x, y]!!
    }

    private fun tryToLightUp(entity: IGameEntity) {
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

            lightSourceQueue.addLast(lightSourceDef)
        }
    }

    private fun handleLightSource(lightSourceDef: LightSourceDef) {
        val startTime = System.currentTimeMillis()
        val (center, radius, radiance) = lightSourceDef

        reusableCircle.setRadius(radius.toFloat()).setCenter(center)
        reusableRect.setSize(2f * radius).setCenter(center)

        val (minX, minY, maxX, maxY) = getMinsAndMaxes(reusableRect)
        for (x in minX..maxX) for (y in minY..maxY) {
            val tile = getTile(x, y)
            if (reusableCircle.overlaps(tile.bounds)) {
                val alpha = ((tile.bounds.getCenter().dst(center) / radius) / radiance)
                tile.currentAlpha = min(alpha.coerceIn(MIN_ALPHA, MAX_ALPHA), tile.currentAlpha)
            }
        }

        debugTime(startTime) {
            "update(): updating light source took too long: " +
                "time=$it, lightSource=$lightSourceDef, minX=$minX, minY=$minY, maxX=$maxX, maxY=$maxY"
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val entities = MegaGameEntities.getOfTypes(reusableEntitiesSet, LIGHT_UP_ENTITY_TYPES)
        entities.forEach { entity -> tryToLightUp(entity) }
        entities.clear()

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

            lightSourceQueue.addLast(lightSourceDef)
        }

        val beaming = game.isProperty("${Megaman.TAG}_${ConstKeys.BEAM}", true)
        if (beaming) {
            val beamCenter = game.getProperty("${Megaman.TAG}_${ConstKeys.BEAM}_${ConstKeys.CENTER}", Vector2::class)

            if (beamCenter != null) {
                val lightSourceDef = lightSourcePool.fetch()

                lightSourceDef.center = beamCenter
                lightSourceDef.radius = MEGAMAN_FULL_CHARGING_RADIUS * ConstVals.PPM
                lightSourceDef.radiance = MEGAMAN_FULL_CHARGING_RADIANCE

                lightSourceQueue.addLast(lightSourceDef)
            }
        }

        while (!lightSourceQueue.isEmpty) {
            val lightSourceDef = lightSourceQueue.removeFirst()
            handleLightSource(lightSourceDef)
            lightSourcePool.free(lightSourceDef)
        }

        val camBounds = game.getGameCamera().getRotatedBounds()
        camBounds.translate(-CAM_BOUNDS_BUFFER * ConstVals.PPM, -CAM_BOUNDS_BUFFER * ConstVals.PPM)
        camBounds.translateSize(2f * CAM_BOUNDS_BUFFER * ConstVals.PPM, 2f * CAM_BOUNDS_BUFFER * ConstVals.PPM)

        previousTiles.forEach { t -> if (!camBounds.overlaps(t.bounds)) t.reset(darkMode) }
        previousTiles.clear()
        previousTiles.addAll(currentTiles)
        currentTiles.clear()

        sprites.values().forEach { t -> tileSpritesPool.free(t) }
        sprites.clear()

        val startTime = System.currentTimeMillis()

        val (minX, minY, maxX, maxY) = getMinsAndMaxes(camBounds)
        for (x in minX..maxX) for (y in minY..maxY) {
            val tile = getTile(x, y)
            currentTiles.add(tile)

            if (!previousTiles.contains(tile)) tile.reset(darkMode) else tile.update(delta, darkMode)

            val sprite = tileSpritesPool.fetch()
            sprite.setBounds(
                bounds.getX() + x * sprite.width,
                bounds.getY() + y * sprite.height,
                dividedPPM,
                dividedPPM
            )
            sprite.setAlpha(tile.currentAlpha)

            sprites.put("${x}_${y}", sprite)
        }

        debugTime(startTime) { "update(): updating tiles took too long: time=$it, size=${(maxX - minX) * (maxY - minY)}" }
    })

    override fun overlaps(shape: IGameShape2D) = this.bounds.overlaps(shape)

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
