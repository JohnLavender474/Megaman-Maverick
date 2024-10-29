package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.MinsAndMaxes
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
import com.megaman.maverick.game.entities.MegaGameEntitiesMap
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.events.EventType
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KClass

class DarknessV2(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener {

    data class LightSourceDef(var center: Vector2, var radius: Int, var radiance: Float)

    private class BlackTile(val bounds: GameRectangle, var step: Float = 1f, var currentAlpha: Float = 1f) {

        fun update(delta: Float, darken: Boolean) {
            currentAlpha += (if (darken) step else -step) * delta
            currentAlpha = currentAlpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
        }

        fun reset(dark: Boolean) {
            currentAlpha = if (dark) MAX_ALPHA else MIN_ALPHA
        }
    }

    companion object {
        const val TAG = "DarknessV2"
        const val MIN_ALPHA = 0f
        const val MAX_ALPHA = 0.75f
        private const val CAM_BOUNDS_BUFFER = 2f
        private const val DEFAULT_PPM_DIVISOR = 2
        private const val MEGAMAN_HALF_CHARGING_RADIUS = 3
        private const val MEGAMAN_HALF_CHARGING_RADIANCE = 1.25f
        private const val MEGAMAN_FULL_CHARGING_RADIUS = 4
        private const val MEGAMAN_FULL_CHARGING_RADIANCE = 1.5f
        private var region: TextureRegion? = null
        private val standardProjLightDef: (IBodyEntity) -> LightSourceDef =
            { LightSourceDef(it.body.getCenter(), 2 * ConstVals.PPM, 1.5f) }
        private val brighterProjLightDef: (IBodyEntity) -> LightSourceDef =
            { LightSourceDef(it.body.getCenter(), 3 * ConstVals.PPM, 2f) }
        private val lightUpEntities = objectMapOf<KClass<out IBodyEntity>, (IBodyEntity) -> LightSourceDef>(
            Bullet::class pairTo standardProjLightDef,
            ChargedShot::class pairTo brighterProjLightDef,
            ArigockBall::class pairTo standardProjLightDef,
            CactusMissile::class pairTo brighterProjLightDef,
            SmallMissile::class pairTo standardProjLightDef,
            Explosion::class pairTo brighterProjLightDef,
            ExplosionOrb::class pairTo standardProjLightDef,
            ChargedShotExplosion::class pairTo {
                it as ChargedShotExplosion
                if (it.fullyCharged) brighterProjLightDef.invoke(it) else standardProjLightDef.invoke(it)
            },
        )
        private const val DEBUG_THRESHOLD_SECS = 0.025f

        private fun debugTime(start: Long, messageOnTooLong: (Float) -> String) {
            val end = System.currentTimeMillis()
            val totalTime = (end - start) / 1000f
            if (totalTime > DEBUG_THRESHOLD_SECS) GameLogger.debug(TAG, messageOnTooLong.invoke(totalTime))
        }
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_READY,
        EventType.BEGIN_ROOM_TRANS,
        EventType.SET_TO_ROOM_NO_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.ADD_LIGHT_SOURCE
    )

    private val rooms = ObjectSet<String>()
    private val lightSourceQueue = Queue<LightSourceDef>()
    private val previousTiles = OrderedSet<BlackTile>()
    private val currentTiles = OrderedSet<BlackTile>()

    private lateinit var allTiles: Matrix<BlackTile>
    private lateinit var tileSpritesPool: Pool<GameSprite>
    private lateinit var lightSourcePool: Pool<LightSourceDef>
    private lateinit var bounds: GameRectangle

    private var key = -1
    private var darkMode = false
    private var dividedPPM = 0f

    private val reusableCircle = GameCircle()
    private val reusableRect = GameRectangle()

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, "Black")
        super.init()
        addComponent(SpritesComponent())
        addComponent(defineUpdatablesComponent())
        tileSpritesPool = Pool(
            startAmount = 0,
            supplier = { GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND, 10)) },
            onPool = { sprite -> sprite.hidden = true },
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

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        val ppmDivisor =
            spawnProps.getOrDefault("${ConstKeys.PPM}_${ConstKeys.DIVISOR}", DEFAULT_PPM_DIVISOR, Int::class)
        dividedPPM = ConstVals.PPM.toFloat() / ppmDivisor

        val rows = (bounds.height / dividedPPM).toInt()
        val columns = (bounds.width / dividedPPM).toInt()
        GameLogger.debug(TAG, "onSpawn(): rows=$rows, columns=$columns")
        allTiles = Matrix(rows, columns)

        darkMode = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        game.eventsMan.removeListener(this)
        lightSourceQueue.clear()
        rooms.clear()
        allTiles.clear()
        previousTiles.clear()
        currentTiles.clear()
        tileSpritesPool.clear()
        lightSourcePool.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_READY -> {
                darkMode = rooms.contains(game.getCurrentRoom())
                GameLogger.debug(TAG, "onEvent(): PLAYER_READY: darkMode=$darkMode")
            }

            EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)?.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)?.name
                if (priorRoom == null || newRoom == null) return

                if (rooms.contains(priorRoom) && !rooms.contains(newRoom)) {
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

                if (priorRoom == null && newRoom == null) return

                if (!rooms.contains(priorRoom) && rooms.contains(newRoom)) {
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
        val minX = ((rect.x - bounds.x) / dividedPPM).toInt().coerceIn(0, allTiles.columns - 1)
        val minY = ((rect.y - bounds.y) / dividedPPM).toInt().coerceIn(0, allTiles.rows - 1)
        val maxX = (ceil((rect.getMaxX() - bounds.x) / dividedPPM)).toInt().coerceIn(0, allTiles.columns - 1)
        val maxY = (ceil((rect.getMaxY() - bounds.y) / dividedPPM)).toInt().coerceIn(0, allTiles.rows - 1)
        return MinsAndMaxes(minX, minY, maxX, maxY)
    }

    private fun getTile(x: Int, y: Int): BlackTile {
        if (allTiles[x, y] == null) {
            val posX = bounds.x + (x * dividedPPM)
            val posY = bounds.y + (y * dividedPPM)
            val tileBounds = GameRectangle(posX, posY, dividedPPM, dividedPPM)
            allTiles[x, y] = BlackTile(tileBounds)
        }
        return allTiles[x, y]!!
    }

    private fun tryToLightUp(entity: IGameEntity) {
        if (entity is IBodyEntity &&
            entity.body.overlaps(bounds as Rectangle) &&
            lightUpEntities.containsKey((entity as IBodyEntity)::class)
        ) {
            val lightSourceDef = lightSourcePool.fetch()
            lightUpEntities[(entity as IBodyEntity)::class].invoke(entity).let {
                lightSourceDef.center = it.center
                lightSourceDef.radiance = it.radiance
                lightSourceDef.radius = it.radius
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
        MegaGameEntitiesMap.getEntitiesOfType(EntityType.PROJECTILE).forEach { t -> tryToLightUp(t) }
        MegaGameEntitiesMap.getEntitiesOfType(EntityType.EXPLOSION).forEach { t -> tryToLightUp(t) }

        if (getMegaman().body.overlaps(bounds as Rectangle)) {
            if (getMegaman().charging) {
                val fullCharged = getMegaman().fullyCharged
                val lightSourceDef = lightSourcePool.fetch()
                lightSourceDef.center = getMegaman().body.getCenter()
                lightSourceDef.radius =
                    (if (fullCharged) MEGAMAN_FULL_CHARGING_RADIUS else MEGAMAN_HALF_CHARGING_RADIUS) * ConstVals.PPM
                lightSourceDef.radiance =
                    if (fullCharged) MEGAMAN_FULL_CHARGING_RADIANCE else MEGAMAN_HALF_CHARGING_RADIANCE
                lightSourceQueue.addLast(lightSourceDef)
            } else if (getMegaman().isBehaviorActive(BehaviorType.JETPACKING)) {
                val lightSourceDef = lightSourcePool.fetch()
                lightSourceDef.center = getMegaman().body.getCenter()
                lightSourceDef.radius = MEGAMAN_HALF_CHARGING_RADIUS * ConstVals.PPM
                lightSourceDef.radiance = MEGAMAN_HALF_CHARGING_RADIANCE
                lightSourceQueue.addLast(lightSourceDef)
            }
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

        while (!lightSourceQueue.isEmpty()) {
            val lightSourceDef = lightSourceQueue.removeFirst()
            handleLightSource(lightSourceDef)
            lightSourcePool.pool(lightSourceDef)
        }

        val camBounds = game.getGameCamera().getRotatedBounds()
        camBounds.x -= CAM_BOUNDS_BUFFER * ConstVals.PPM
        camBounds.y -= CAM_BOUNDS_BUFFER * ConstVals.PPM
        camBounds.width += 2f * CAM_BOUNDS_BUFFER * ConstVals.PPM
        camBounds.height += 2f * CAM_BOUNDS_BUFFER * ConstVals.PPM

        previousTiles.forEach { t -> if (!camBounds.overlaps(t.bounds as Rectangle)) t.reset(darkMode) }
        previousTiles.clear()
        previousTiles.addAll(currentTiles)
        currentTiles.clear()

        sprites.values().forEach { t -> tileSpritesPool.pool(t) }
        sprites.clear()

        val startTime = System.currentTimeMillis()

        val (minX, minY, maxX, maxY) = getMinsAndMaxes(camBounds)
        for (x in minX..maxX) for (y in minY..maxY) {
            val tile = getTile(x, y)
            currentTiles.add(tile)

            if (!previousTiles.contains(tile)) tile.reset(darkMode) else tile.update(delta, darkMode)

            val sprite = tileSpritesPool.fetch()
            sprite.setBounds(bounds.x + x * sprite.width, bounds.y + y * sprite.height, dividedPPM, dividedPPM)
            sprite.setAlpha(tile.currentAlpha)

            sprites.put("${x}_${y}", sprite)
        }

        debugTime(startTime) { "update(): updating tiles took too long: time=$it, size=${(maxX - minX) * (maxY - minY)}" }
    })

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
