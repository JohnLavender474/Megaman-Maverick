package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.interpolate
import com.engine.common.objects.Matrix
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.GameEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame

class BlackBackground(game: MegamanMaverickGame) : GameEntity(game), ISpriteEntity, IEventListener {

    companion object {
        const val TAG = "BlackBackground"
        private const val TRANS_DUR = 0.5f
        private var region: TextureRegion? = null
    }

    private class BlackTile(val sprite: GameSprite, val timer: Timer, var startAlpha: Float, var targetAlpha: Float)

    override val eventKeyMask = objectSetOf<Any>(
        EventType.REQ_BLACK_BACKGROUND, EventType.END_ROOM_TRANS
    )

    private lateinit var tiles: Matrix<BlackTile>
    private lateinit var bounds: GameRectangle
    private lateinit var room: String

    private var key = -1

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.COLORS.source, "Black")
        addComponent(SpritesComponent(this))
        addComponent(defineCullablesComponent())
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
                val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
                val timer = Timer(TRANS_DUR).setToEnd()
                val tile = BlackTile(sprite, timer, 0f, 0f)
                tiles[x, y] = tile

                val key = "[$x][$y]"
                sprites.put(key, sprite)

                val spriteX = bounds.x + (x * ConstVals.PPM)
                val spriteY = bounds.y + (y * ConstVals.PPM)

                putUpdateFunction(key) { delta, _sprite ->
                    _sprite as GameSprite
                    _sprite.setRegion(region!!)
                    _sprite.setBounds(spriteX, spriteY, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())

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
                    _sprite.setAlpha(alpha)
                }
            }
        }
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        game.eventsMan.removeListener(this)
        sprites.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.REQ_BLACK_BACKGROUND -> {
                // GameLogger.debug(TAG, "REQ BLACK BACKGROUND event = $event")
                val keys = event.getProperty(ConstKeys.KEYS) as ObjectSet<Int>
                if (keys.contains(key)) {
                    val light = event.getProperty(ConstKeys.LIGHT, Boolean::class)!!
                    val center = event.getProperty(ConstKeys.CENTER, Vector2::class)!!
                    val radius = event.getProperty(ConstKeys.RADIUS, Int::class)!!

                    val circle = GameCircle(center, radius * ConstVals.PPM.toFloat())
                    tiles.forEach { tile ->
                        if (tile.sprite.priority != DrawingPriority(DrawingSection.BACKGROUND, 0)) {
                            GameLogger.debug(TAG, "Tile priority: ${tile.sprite.priority}")
                        }

                        val bounds = GameRectangle()
                        val sprite = tile.sprite
                        bounds.x = sprite.x
                        bounds.y = sprite.y
                        bounds.width = sprite.width
                        bounds.height = sprite.height

                        if (circle.overlaps(bounds)) {
                            tile.startAlpha = tile.targetAlpha
                            tile.targetAlpha = if (light) {
                                var alpha = bounds.getCenter().dst(center) / (radius * ConstVals.PPM)
                                if (alpha < 0f) alpha = 0f else if (alpha > 1f) alpha = 1f
                                alpha
                            } else 1f
                            tile.timer.reset()
                        }
                    }
                }
            }

            EventType.END_ROOM_TRANS -> {
                // GameLogger.debug(TAG, "END ROOM TRANS event: $event")
                val newRoom = event.getProperty(
                    ConstKeys.ROOM, RectangleMapObject::class
                )!!.name
                tiles.forEach {
                    if (this.room == newRoom) {
                        it.startAlpha = 0f
                        it.targetAlpha = 1f
                    } else {
                        it.startAlpha = 1f
                        it.targetAlpha = 0f
                    }
                    it.timer.reset()
                }
            }
        }
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(getMegamanMaverickGame().getGameCamera(), { bounds })
        return CullablesComponent(this, cullable)
    }
}