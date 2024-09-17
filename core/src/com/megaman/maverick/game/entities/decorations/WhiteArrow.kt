package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.SmoothOscillationTimer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class WhiteArrow(game: MegamanMaverickGame) : MegaGameEntity(game), IDirectionRotatable {

    companion object {
        const val TAG = "WhiteArrow"
        private const val ALPHA_FREQUENCY = 2f
        private const val SPEED = 3f
        private const val MIN_ALPHA = 0.3f
        private const val MAX_ALPHA = 0.9f
        private var region: TextureRegion? = null
    }

    override var directionRotation: Direction? = null

    private val oscillationTimer = SmoothOscillationTimer(ALPHA_FREQUENCY, MIN_ALPHA, MAX_ALPHA)
    private lateinit var position: Vector2
    private var maxValue = 0f

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)
        position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        val maxOffset = spawnProps.get(ConstKeys.MAX, Int::class)!!
        maxValue = when (directionRotation!!) {
            Direction.UP -> position.y + maxOffset * ConstVals.PPM
            Direction.DOWN -> position.y - maxOffset * ConstVals.PPM
            Direction.LEFT -> position.x - maxOffset * ConstVals.PPM
            Direction.RIGHT -> position.x + maxOffset * ConstVals.PPM
        }
        oscillationTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        oscillationTimer.update(delta)

        val movement = when (directionRotation!!) {
            Direction.UP -> Vector2(0f, SPEED)
            Direction.DOWN -> Vector2(0f, -SPEED)
            Direction.LEFT -> Vector2(-SPEED, 0f)
            Direction.RIGHT -> Vector2(SPEED, 0f)
        }.scl(ConstVals.PPM.toFloat() * delta)
        position.add(movement)

        if (when (directionRotation!!) {
                Direction.UP -> position.y >= maxValue
                Direction.DOWN -> position.y <= maxValue
                Direction.LEFT -> position.x <= maxValue
                Direction.RIGHT -> position.x >= maxValue
            }
        ) destroy()
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND, 5))
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation
            _sprite.setCenter(position)
            _sprite.setAlpha(oscillationTimer.getValue())
        }
        return spritesComponent
    }

    override fun getEntityType() = EntityType.DECORATION

    override fun getTag() = TAG
}