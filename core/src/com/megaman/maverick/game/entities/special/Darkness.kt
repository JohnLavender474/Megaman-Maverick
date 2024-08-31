package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.IPropertizable
import com.engine.common.interpolate
import com.engine.common.objects.Matrix
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.ISpritesEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType

import java.util.*

class Darkness(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IEventListener {

    companion object {
        const val TAG = "BlackBackground"
        private const val TRANS_DUR = 0.25f
        private var region: TextureRegion? = null
    }

    private class BlackTile(
        val sprite: GameSprite,
        val timer: Timer,
        var startAlpha: Float,
        var targetAlpha: Float,
        var currentAlpha: Float = 0f,
        var set: Boolean = false
    )

    private enum class LightEventType {
        LIGHT_SOURCE, LIGHT_UP_ALL, DARKEN_ALL
    }

    private class LightEvent(
        val lightEventType: LightEventType, override val properties: Properties = Properties()
    ) : IPropertizable, Comparable<LightEvent> {
        override fun compareTo(other: LightEvent) = lightEventType.compareTo(other.lightEventType)
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_SPAWN, EventType.BLACK_BACKGROUND, EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS
    )

    private val lightEventQueue = PriorityQueue<LightEvent>()

    private lateinit var tiles: Matrix<BlackTile>
    private lateinit var bounds: GameRectangle
    private lateinit var room: String

    private var key = -1
    private var darkMode = false

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, "Black")
        addComponent(SpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        game.eventsMan.addListener(this)

        key = spawnProps.get(ConstKeys.KEY, Int::class)!!
        room = spawnProps.get(ConstKeys.ROOM, String::class)!!

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        val rows = (bounds.height / ConstVals.PPM).toInt()
        val columns = (bounds.width / ConstVals.PPM).toInt()

        tiles = Matrix(rows, columns)
        for (x in 0 until columns) {
            for (y in 0 until rows) {
                val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
                sprite.setRegion(region!!)
                val spriteX = bounds.x + (x * ConstVals.PPM)
                val spriteY = bounds.y + (y * ConstVals.PPM)
                sprite.setBounds(spriteX, spriteY, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())

                val timer = Timer(TRANS_DUR).setToEnd()

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

        lightEventQueue.clear()
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        game.eventsMan.removeListener(this)
        sprites.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_SPAWN -> {
                darkMode = false
                tiles.forEach { tile ->
                    tile.startAlpha = tile.currentAlpha
                    tile.targetAlpha = 0f
                    tile.timer.reset()
                    tile.set = true
                }
            }

            EventType.BLACK_BACKGROUND -> {
                val keys = event.getProperty(ConstKeys.KEYS) as ObjectSet<Int>
                if (keys.contains(key)) {
                    val light = event.getProperty(ConstKeys.LIGHT, Boolean::class)!!
                    val center = event.getProperty(ConstKeys.CENTER, Vector2::class)!!
                    val radius = event.getProperty(ConstKeys.RADIUS, Int::class)!!
                    val radiance = event.getProperty(ConstKeys.RADIANCE, Float::class)!!
                    lightEventQueue.add(
                        LightEvent(
                            LightEventType.LIGHT_SOURCE, props(
                                ConstKeys.LIGHT to light,
                                ConstKeys.CENTER to center,
                                ConstKeys.RADIUS to radius,
                                ConstKeys.RADIANCE to radiance
                            )
                        )
                    )
                }
            }

            EventType.BEGIN_ROOM_TRANS -> {
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                GameLogger.debug(TAG, "BEGIN_ROOM_TRANS: this room = $room, next room = $newRoom")
                if (this.room != newRoom) lightEventQueue.add(LightEvent(LightEventType.LIGHT_UP_ALL))
            }

            EventType.END_ROOM_TRANS -> {
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                GameLogger.debug(TAG, "END_ROOM_TRANS: this room = $room, next room = $newRoom")
                if (this.room == newRoom) lightEventQueue.add(LightEvent(LightEventType.DARKEN_ALL))
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        tiles.forEach { it.set = false }

        while (!lightEventQueue.isEmpty()) {
            val lightEvent = lightEventQueue.poll()
            handleLightEvent(lightEvent.lightEventType, lightEvent.properties)
        }

        tiles.forEach {
            if (!it.set) {
                it.startAlpha = it.currentAlpha
                it.targetAlpha = if (darkMode) 1f else 0f
                it.timer.reset()
                it.set = true
            }
        }
    })

    private fun handleLightEvent(lightEventType: LightEventType, properties: Properties) {
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
                val center = properties.get(ConstKeys.CENTER, Vector2::class)!!
                val radius = properties.get(ConstKeys.RADIUS, Int::class)!!.toFloat() * ConstVals.PPM
                val light = properties.get(ConstKeys.LIGHT, Boolean::class)!!
                val radiance = properties.get(ConstKeys.RADIANCE, Float::class)!!

                val circle = GameCircle(center, radius)
                tiles.forEach { _, _, tile ->
                    val bounds = tile!!.sprite.boundingRectangle.toGameRectangle()
                    if (circle.overlaps(bounds)) {
                        val tempTargetAlpha = if (light) {
                            var alpha = (bounds.getCenter().dst(center) / radius) / radiance
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
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullable))
    }
}