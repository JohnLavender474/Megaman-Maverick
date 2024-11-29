package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.*
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.MinsAndMaxes
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

// TODO: not working properly
class DarknessV3(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, ICullableEntity, IEventListener {

    data class LightSourceDef(var center: Vector2, var radius: Int, var radiance: Float)

    private class DarknessTile(
        val bounds: GameRectangle,
        var step: Float = 1f,
        var currentAlpha: Float = 1f
    ) : Resettable {

        var maxAlpha: Float = ABSOLUTE_MAX_ALPHA
            set(value) {
                field = min(value, ABSOLUTE_MAX_ALPHA)
            }
        var minAlpha: Float = ABSOLUTE_MIN_ALPHA
            set(value) {
                field = max(value, ABSOLUTE_MIN_ALPHA)
            }

        fun lighten(delta: Float) {
            currentAlpha -= step * delta
            coerce()
        }

        fun darken(delta: Float) {
            currentAlpha += step * delta
            coerce()
        }

        fun coerce() {
            currentAlpha = currentAlpha.coerceIn(minAlpha, maxAlpha)
        }

        fun isFullyLightened() = currentAlpha == minAlpha

        override fun reset() {
            maxAlpha = ABSOLUTE_MAX_ALPHA
            minAlpha = ABSOLUTE_MIN_ALPHA
        }
    }

    private class DarknessTilesLightener(val tiles: Iterable<DarknessTile>) : Updatable, Resettable {

        private val tilesToUpdate = Array<DarknessTile>()
        private val tilesFinishedUpdating = Array<DarknessTile>()

        init {
            tiles.forEach { t -> tilesToUpdate.add(t) }
        }

        override fun update(delta: Float) {
            val iter = tilesToUpdate.iterator()
            while (iter.hasNext()) {
                val tile = iter.next()
                tile.lighten(delta)
                if (tile.isFullyLightened()) {
                    tilesFinishedUpdating.add(tile)
                    iter.remove()
                }
            }
        }

        override fun reset() {
            tilesToUpdate.clear()
            tilesFinishedUpdating.clear()
        }
    }

    companion object {
        const val TAG = "DarknessV3"
        const val ABSOLUTE_MIN_ALPHA = 0f
        const val ABSOLUTE_MAX_ALPHA = 0.95f
        private const val CAM_BOUNDS_BUFFER = 2f
        private const val DEFAULT_PPM_DIVISOR = 4
        private const val MEGAMAN_CHARGING_RADIUS = 4
        private const val MEGAMAN_CHARGING_RADIANCE = 1f
        private var region: TextureRegion? = null
        private val standardProjLightDef: (IBodyEntity) -> LightSourceDef =
            { LightSourceDef(it.body.getCenter(), 2, 1.5f) }
        private val brighterProjLightDef: (IBodyEntity) -> LightSourceDef =
            { LightSourceDef(it.body.getCenter(), 3, 2f) }
        private val lightUpEntities = objectMapOf<KClass<out IBodyEntity>, (IBodyEntity) -> LightSourceDef>(
            Bullet::class pairTo standardProjLightDef,
            ChargedShot::class pairTo brighterProjLightDef,
            ArigockBall::class pairTo standardProjLightDef,
            CactusMissile::class pairTo brighterProjLightDef,
            SmallMissile::class pairTo standardProjLightDef,
            Explosion::class pairTo brighterProjLightDef,
            ChargedShotExplosion::class pairTo {
                it as ChargedShotExplosion
                if (it.fullyCharged) brighterProjLightDef.invoke(it) else standardProjLightDef.invoke(it)
            },
        )
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.ADD_LIGHT_SOURCE,
        EventType.REMOVE_LIGHT_SOURCE,
        EventType.BEGIN_ROOM_TRANS,
        EventType.SET_TO_ROOM_NO_TRANS,
        EventType.END_ROOM_TRANS
    )

    private val queuedLightSourcesToAdd = Queue<GamePair<Int, LightSourceDef>>()
    private val queuedLightSourcesToRemove = Queue<Int>()

    private lateinit var allTilesMatrix: Matrix<DarknessTile>
    private val tilesInLightSourceMap = OrderedMap<Int, DarknessTilesLightener>()
    private val otherTilesSet = OrderedSet<DarknessTile>()

    private val previousTilesInCamBoundsSet = OrderedSet<DarknessTile>()
    private val currentTilesInCamBoundsSet = OrderedSet<DarknessTile>()

    private val rooms = ObjectSet<String>()

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
        addComponent(defineCullablesComponent())
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

        key = spawnProps.getOrDefault(ConstKeys.ID, -1, Int::class)
        spawnProps.get(ConstKeys.ROOM, String::class)!!.split(",").forEach { t -> rooms.add(t) }
        GameLogger.debug(TAG, "onSpawn(): key=$key, rooms=$rooms")

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        val ppmDivisor =
            spawnProps.getOrDefault("${ConstKeys.PPM}_${ConstKeys.DIVISOR}", DEFAULT_PPM_DIVISOR, Int::class)
        dividedPPM = ConstVals.PPM.toFloat() / ppmDivisor

        val rows = (bounds.getHeight() / dividedPPM).toInt()
        val columns = (bounds.getWidth() / dividedPPM).toInt()
        GameLogger.debug(TAG, "onSpawn(): rows=$rows, columns=$columns")
        allTilesMatrix = Matrix(rows, columns)

        darkMode = false
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)

        queuedLightSourcesToAdd.clear()
        queuedLightSourcesToRemove.clear()

        tilesInLightSourceMap.clear()
        otherTilesSet.clear()
        previousTilesInCamBoundsSet.clear()
        currentTilesInCamBoundsSet.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)!!.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (rooms.contains(priorRoom) && !rooms.contains(newRoom)) {
                    GameLogger.debug(
                        Darkness.Companion.TAG,
                        "onEvent(): BEGIN_ROOM_TRANS/SET_TO_ROOM_NO_TRANS: light up all: " +
                                "event=$event, rooms=$rooms, newRoom=$newRoom"
                    )
                    darkMode = false
                }
            }

            EventType.END_ROOM_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)!!.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (!rooms.contains(priorRoom) && rooms.contains(newRoom)) {
                    GameLogger.debug(
                        Darkness.Companion.TAG,
                        "onEvent(): END_ROOM_TRANS: darken all: event=$event, rooms=$rooms, newRoom=$newRoom"
                    )
                    darkMode = true
                }
            }

            EventType.ADD_LIGHT_SOURCE -> {
                val keys = event.getProperty(ConstKeys.KEYS) as ObjectSet<Int>
                if (keys.contains(key)) {
                    val mapObjectId = event.getProperty(ConstKeys.ID, Int::class)!!
                    val center = event.getProperty(ConstKeys.CENTER, Vector2::class)!!
                    val radius = event.getProperty(ConstKeys.RADIUS, Int::class)!!
                    val radiance = event.getProperty(ConstKeys.RADIANCE, Float::class)!!

                    val lightSourceDef = lightSourcePool.fetch()
                    lightSourceDef.center = center
                    lightSourceDef.radius = radius
                    lightSourceDef.radiance = radiance

                    queuedLightSourcesToAdd.addLast(mapObjectId pairTo lightSourceDef)
                }
            }
        }
    }

    private fun getTile(x: Int, y: Int): DarknessTile {
        if (allTilesMatrix[x, y] == null) {
            val posX = bounds.getX() + (x * dividedPPM)
            val posY = bounds.getY() + (y * dividedPPM)
            val tileBounds = GameRectangle(posX, posY, dividedPPM, dividedPPM)
            allTilesMatrix[x, y] = DarknessTile(tileBounds)
        }
        return allTilesMatrix[x, y]!!
    }

    private fun getMinsAndMaxes(rect: GameRectangle): MinsAndMaxes {
        val minX = ((rect.x - bounds.getX()) / dividedPPM).toInt().coerceIn(0, allTilesMatrix.columns - 1)
        val minY = ((rect.y - bounds.getY()) / dividedPPM).toInt().coerceIn(0, allTilesMatrix.rows - 1)
        val maxX = (ceil((rect.getMaxX() - bounds.getX()) / dividedPPM)).toInt().coerceIn(0, allTilesMatrix.columns - 1)
        val maxY = (ceil((rect.getMaxY() - bounds.getY()) / dividedPPM)).toInt().coerceIn(0, allTilesMatrix.rows - 1)
        return MinsAndMaxes(minX, minY, maxX, maxY)
    }

    private fun findTilesInLightSource(lightSourceDef: LightSourceDef): Array<DarknessTile> {
        val (center, radius, _) = lightSourceDef
        val adjustedRadius = radius.toFloat() * ConstVals.PPM

        reusableCircle.setRadius(adjustedRadius).setCenter(center)
        reusableRect.setSize(2f * adjustedRadius).setCenter(center)

        var array = Array<DarknessTile>()
        val (minX, minY, maxX, maxY) = getMinsAndMaxes(reusableRect)
        for (x in minX..maxX) for (y in minY..maxY) {
            val tile = getTile(x, y)
            if (reusableCircle.overlaps(tile.bounds)) array.add(tile)
        }

        return array
    }

    private fun addLightSource(key: Int, lightSourceDef: LightSourceDef) {
        if (tilesInLightSourceMap.containsKey(key)) {
            val tilesToDarken = tilesInLightSourceMap.get(key).tiles
            tilesToDarken.forEach { t ->
                t.reset()
                otherTilesSet.add(t)
            }
        }

        val (center, radius, radiance) = lightSourceDef

        val tilesToLighten = findTilesInLightSource(lightSourceDef)
        tilesToLighten.forEach { t ->
            val alpha = ((t.bounds.getCenter().dst(center) / radius) / radiance)
            if (alpha > t.maxAlpha) t.maxAlpha = alpha
            otherTilesSet.remove(t)
        }

        val tilesLightener = DarknessTilesLightener(tilesToLighten)
        tilesInLightSourceMap.put(key, tilesLightener)
    }

    private fun removeLightSource(key: Int) {
        if (tilesInLightSourceMap.containsKey(key)) {
            val tilesToDarken = tilesInLightSourceMap.remove(key).tiles
            tilesToDarken.forEach { t ->
                t.reset()
                otherTilesSet.add(t)
            }
        }
    }

    private fun tryToLightUp(entity: MegaGameEntity) {
        if (entity is IBodyEntity && entity.body.overlaps(bounds as Rectangle) &&
            lightUpEntities.containsKey(entity::class)
        ) {
            val lightSourceDef = lightSourcePool.fetch()
            lightUpEntities[entity::class].invoke(entity).let {
                lightSourceDef.center = it.center
                lightSourceDef.radiance = it.radiance
                lightSourceDef.radius = it.radius
            }
            queuedLightSourcesToAdd.addLast(entity.mapObjectId pairTo lightSourceDef)
        }
    }

    private fun setToDraw(tile: DarknessTile) {
        val sprite = tileSpritesPool.fetch()
        sprite.setBounds(tile.bounds)
        sprite.setAlpha(tile.currentAlpha)
        sprite.hidden = !darkMode
        sprites.put(tile.hashCode().toString(), sprite)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        MegaGameEntities.getEntitiesOfType(EntityType.PROJECTILE).forEach { t -> tryToLightUp(t) }
        MegaGameEntities.getEntitiesOfType(EntityType.EXPLOSION).forEach { t -> tryToLightUp(t) }

        if (megaman().body.overlaps(bounds as Rectangle) && megaman().charging) {
            val lightSourceDef = lightSourcePool.fetch()
            lightSourceDef.center = megaman().body.getCenter()
            lightSourceDef.radius = MEGAMAN_CHARGING_RADIUS
            lightSourceDef.radiance = MEGAMAN_CHARGING_RADIANCE
            queuedLightSourcesToAdd.addLast(megaman().mapObjectId pairTo lightSourceDef)
        }

        while (!queuedLightSourcesToAdd.isEmpty) {
            val (mapObjectId, lightSourceDef) = queuedLightSourcesToAdd.removeFirst()
            addLightSource(mapObjectId, lightSourceDef)
            lightSourcePool.pool(lightSourceDef)
        }
        while (!queuedLightSourcesToRemove.isEmpty) {
            val lightSourceKey = queuedLightSourcesToRemove.removeFirst()
            removeLightSource(lightSourceKey)
        }

        val camBounds = game.getGameCamera().getRotatedBounds()
        camBounds.x -= CAM_BOUNDS_BUFFER * ConstVals.PPM
        camBounds.y -= CAM_BOUNDS_BUFFER * ConstVals.PPM
        camBounds.width += 2f * CAM_BOUNDS_BUFFER * ConstVals.PPM
        camBounds.height += 2f * CAM_BOUNDS_BUFFER * ConstVals.PPM

        sprites.values().forEach { t -> tileSpritesPool.pool(t) }
        sprites.clear()

        val (minX, minY, maxX, maxY) = getMinsAndMaxes(camBounds)
        for (x in minX..maxX) for (y in minY..maxY) {
            val tile = getTile(x, y)
            otherTilesSet.add(tile)
        }

        tilesInLightSourceMap.forEach { entry ->
            val mapObjectId = entry.key
            if (!MegaGameEntities.hasAnyEntitiesOfMapObjectId(mapObjectId)) {
                queuedLightSourcesToRemove.addLast(mapObjectId)
                return@forEach
            }

            val lightener = entry.value
            lightener.update(delta)
            lightener.tiles.forEach { t ->
                otherTilesSet.remove(t)
                setToDraw(t)
            }
        }

        otherTilesSet.forEach { t ->
            if (darkMode) t.darken(delta) else t.lighten(delta)
            setToDraw(t)
        }
        otherTilesSet.clear()
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
