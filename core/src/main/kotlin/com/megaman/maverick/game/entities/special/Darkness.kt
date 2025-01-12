package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.interpolate
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import java.util.*
import kotlin.math.ceil
import kotlin.reflect.KClass

class Darkness(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener {

    companion object {
        const val TAG = "Darkness"
        private const val DEFAULT_TRANS_DUR = 0.25f
        private const val DEFAULT_PPM_DIVISOR = 4
        private const val MEGAMAN_CHARGING_RADIUS = 4
        private const val MEGAMAN_CHARGING_RADIANCE = 1f
        private var region: TextureRegion? = null
        private val standardProjLightDef: (IBodyEntity) -> LightEventDef =
            { LightEventDef(true, it.body.getCenter(), 2, 1.5f) }
        private val brighterProjLightDef: (IBodyEntity) -> LightEventDef =
            { LightEventDef(true, it.body.getCenter(), 3, 2f) }
        private val lightUpEntities = objectMapOf<KClass<out IBodyEntity>, (IBodyEntity) -> LightEventDef>(
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

    data class LightEventDef(
        var light: Boolean,
        var center: Vector2,
        var radius: Int,
        var radiance: Float
    )

    private class BlackTile(
        val sprite: GameSprite,
        val timer: Timer,
        var startAlpha: Float,
        var targetAlpha: Float,
        var currentAlpha: Float = 0f,
        var set: Boolean = false
    )

    enum class LightEventType { LIGHT_SOURCE, LIGHT_UP_ALL, DARKEN_ALL }

    class LightEvent(
        val lightEventType: LightEventType,
        val lightEventDef: LightEventDef? = null
    ) : Comparable<LightEvent> {

        override fun compareTo(other: LightEvent) = lightEventType.compareTo(other.lightEventType)
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.ADD_LIGHT_SOURCE, EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS, EventType.END_ROOM_TRANS
    )

    private val lightEventQueue = PriorityQueue<LightEvent>()
    private val rooms = ObjectSet<String>()
    private lateinit var tiles: Matrix<BlackTile>
    private lateinit var bounds: GameRectangle
    private var key = -1
    private var darkMode = false
    private var ppmDivisor = 2

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, "Black")
        addComponent(SpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        key = spawnProps.getOrDefault(ConstKeys.KEY, -1, Int::class)
        spawnProps.get(ConstKeys.ROOM, String::class)!!.split(",").forEach { t -> rooms.add(t) }

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        ppmDivisor = spawnProps.getOrDefault("${ConstKeys.PPM}_${ConstKeys.DIVISOR}", DEFAULT_PPM_DIVISOR, Int::class)
        val rows = (bounds.getHeight() / (ConstVals.PPM / ppmDivisor)).toInt()
        val columns = (bounds.getWidth() / (ConstVals.PPM / ppmDivisor)).toInt()
        GameLogger.debug(TAG, "onSpawn(): rows=$rows, columns=$columns")

        tiles = Matrix(rows, columns)
        for (x in 0 until columns) {
            for (y in 0 until rows) {
                val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
                sprite.setRegion(region!!)
                sprite.setAlpha(0f)
                val spriteX = bounds.getX() + (x * (ConstVals.PPM / ppmDivisor))
                val spriteY = bounds.getY() + (y * (ConstVals.PPM / ppmDivisor))
                sprite.setBounds(
                    spriteX,
                    spriteY,
                    ConstVals.PPM.toFloat() / ppmDivisor,
                    ConstVals.PPM.toFloat() / ppmDivisor
                )

                val timer = Timer(DEFAULT_TRANS_DUR).setToEnd()
                val tile = BlackTile(sprite, timer, 0f, 0f)
                tiles[x, y] = tile

                val key = "[$x][$y]"
                sprites.put(key, sprite)
                putUpdateFunction(key) { delta, _sprite ->
                    if (tile.startAlpha < 0f) tile.startAlpha = 0f
                    else if (tile.startAlpha > 1f) tile.startAlpha = 1f

                    if (tile.targetAlpha < 0f) tile.targetAlpha = 0f
                    else if (tile.targetAlpha > 1f) tile.targetAlpha = 1f

                    var alpha = if (!timer.isFinished()) {
                        timer.update(delta)
                        val ratio = timer.getRatio()
                        interpolate(tile.startAlpha, tile.targetAlpha, ratio)
                    } else tile.targetAlpha

                    if (alpha < 0f) alpha = 0f
                    else if (alpha > 1f) alpha = 1f

                    tile.currentAlpha = alpha
                    _sprite.setAlpha(alpha)
                }
            }
        }
        GameLogger.debug(TAG, "onSpawn(): loaded ${rows * columns} sprites")
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)
        rooms.clear()
        sprites.clear()
        lightEventQueue.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.ADD_LIGHT_SOURCE -> {
                val keys = event.getProperty(ConstKeys.KEYS) as ObjectSet<Int>
                if (keys.contains(key)) {
                    val light = event.getProperty(ConstKeys.LIGHT, Boolean::class)!!
                    val center = event.getProperty(ConstKeys.CENTER, Vector2::class)!!
                    val radius = event.getProperty(ConstKeys.RADIUS, Int::class)!!
                    val radiance = event.getProperty(ConstKeys.RADIANCE, Float::class)!!
                    lightEventQueue.add(
                        LightEvent(
                            LightEventType.LIGHT_SOURCE,
                            LightEventDef(light, center, radius, radiance)
                        )
                    )
                }
            }

            EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)!!.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (rooms.contains(priorRoom) && !rooms.contains(newRoom)) {
                    GameLogger.debug(
                        TAG, "onEvent(): BEGIN_ROOM_TRANS/SET_TO_ROOM_NO_TRANS: lighting up all: " +
                            "event=$event, rooms=$rooms, newRoom=$newRoom"
                    )
                    lightEventQueue.add(LightEvent(LightEventType.LIGHT_UP_ALL))
                }
            }

            EventType.END_ROOM_TRANS -> {
                val priorRoom = event.getProperty(ConstKeys.PRIOR, RectangleMapObject::class)!!.name
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (!rooms.contains(priorRoom) && rooms.contains(newRoom)) {
                    GameLogger.debug(
                        TAG, "onEvent(): END_ROOM_TRANS: darken all: event=$event, rooms=$rooms, newRoom=$newRoom"
                    )
                    lightEventQueue.add(LightEvent(LightEventType.DARKEN_ALL))
                }
            }
        }
    }

    private fun tryToLightUp(entity: IGameEntity) {
        if (entity is IBodyEntity &&
            entity.body.getBounds().overlaps(bounds) &&
            lightUpEntities.containsKey(entity::class)
        ) {
            val lightEvent = LightEvent(LightEventType.LIGHT_SOURCE, lightUpEntities[entity::class].invoke(entity))
            lightEventQueue.add(lightEvent)
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        val currentRoom = game.getCurrentRoom()
        if (currentRoom != null) {
            MegaGameEntities.getEntitiesOfType(EntityType.PROJECTILE).forEach { t -> tryToLightUp(t) }
            MegaGameEntities.getEntitiesOfType(EntityType.EXPLOSION).forEach { t -> tryToLightUp(t) }

            if (megaman.body.getBounds().overlaps(bounds) && megaman.charging) {
                val lightEvent = LightEvent(
                    LightEventType.LIGHT_SOURCE,
                    LightEventDef(
                        true,
                        megaman.body.getCenter(),
                        MEGAMAN_CHARGING_RADIUS,
                        MEGAMAN_CHARGING_RADIANCE
                    )
                )
                lightEventQueue.add(lightEvent)
            }

            tiles.forEach { it.set = false }
            while (!lightEventQueue.isEmpty()) {
                val lightEvent = lightEventQueue.poll()
                handleLightEvent(lightEvent.lightEventType, lightEvent.lightEventDef)
            }
            tiles.forEach {
                if (!it.set) {
                    it.startAlpha = it.currentAlpha
                    it.targetAlpha = if (darkMode) 1f else 0f
                    it.timer.reset()
                    it.set = true
                }
            }
        }
    })

    private fun handleLightEvent(lightEventType: LightEventType, lightEventDef: LightEventDef? = null) {
        when (lightEventType) {
            LightEventType.LIGHT_UP_ALL -> {
                darkMode = false
                tiles.forEach { tile ->
                    tile.startAlpha = tile.currentAlpha
                    tile.targetAlpha = 0f
                    tile.timer.reset()
                    tile.set = true
                }
            }

            LightEventType.DARKEN_ALL -> {
                darkMode = true
                tiles.forEach { tile ->
                    tile.startAlpha = tile.currentAlpha
                    tile.targetAlpha = 1f
                    tile.timer.reset()
                    tile.set = true
                }
            }

            LightEventType.LIGHT_SOURCE -> {
                if (lightEventDef == null)
                    throw IllegalStateException("LightEventDef cannot be null for LIGHT_SOURCE event")

                val (light, center, radius, radiance) = lightEventDef
                val adjustedRadius = radius.toFloat() * ConstVals.PPM
                val circle = GameCircle(center, adjustedRadius)

                var startX = (((center.x - adjustedRadius) - bounds.getX()) / (ConstVals.PPM.toFloat() / ppmDivisor)).toInt()
                startX = startX.coerceIn(0, tiles.columns - 1)
                var endX = ceil(((center.x + adjustedRadius) - bounds.getX()) / (ConstVals.PPM / ppmDivisor)).toInt()
                endX = endX.coerceIn(0, tiles.columns - 1)
                var startY = (((center.y - adjustedRadius) - bounds.getY()) / (ConstVals.PPM / ppmDivisor)).toInt()
                startY = startY.coerceIn(0, tiles.rows - 1)
                var endY = ceil(((center.y + adjustedRadius) - bounds.getY()) / (ConstVals.PPM / ppmDivisor)).toInt()
                endY = endY.coerceIn(0, tiles.rows - 1)

                GameLogger.debug(
                    TAG,
                    "handleLightEvent(): LIGHT_SOURCE: startX=$startX, startY=$startY, endX=$endX, endY=$endY"
                )

                for (x in startX..endX) for (y in startY..endY) {
                    val tile = tiles[x, y]
                    val bounds = tile!!.sprite.boundingRectangle.toGameRectangle()
                    if (circle.overlaps(bounds)) {
                        val tempTargetAlpha = if (light) {
                            var alpha = (bounds.getCenter().dst(center) / adjustedRadius) / radiance
                            if (alpha < 0f) alpha = 0f else if (alpha > 1f) alpha = 1f
                            alpha
                        } else 1f

                        if (tile.set && tempTargetAlpha < tile.targetAlpha) {
                            tile.startAlpha = tile.currentAlpha
                            tile.targetAlpha = tempTargetAlpha
                            tile.timer.reset()
                        } else if (!tile.set) {
                            tile.startAlpha = tile.currentAlpha
                            tile.targetAlpha = tempTargetAlpha
                            tile.timer.reset()
                            tile.set = true
                        }
                    }
                }
            }
        }
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag(): String = TAG
}
