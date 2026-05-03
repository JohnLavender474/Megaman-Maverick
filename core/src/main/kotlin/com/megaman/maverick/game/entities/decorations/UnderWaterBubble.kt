package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Speed
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.setToDirection
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import kotlin.math.floor

class UnderWaterBubble(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, ISpritesEntity,
    IDirectional {

    companion object {
        const val TAG = "UnderWaterBubble"
        private const val SLOW_SPEED = 2.5f
        private const val FAST_SPEED = 8f
        private const val TIME_TO_FADE = 0.5f
        private var region: TextureRegion? = null
    }

    override lateinit var direction: Direction

    private val position = Vector2()
    private val velocity = Vector2()

    private val fadeTimer = Timer(TIME_TO_FADE)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        position.set(spawn)

        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!

        val speed = spawnProps.get(ConstKeys.SPEED, Speed::class)
        if (speed != null)
            velocity
                .setToDirection(direction)
                .scl((if (speed == Speed.SLOW) SLOW_SPEED else FAST_SPEED) * ConstVals.PPM)

        fadeTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        velocity.setZero()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        position.add(velocity.x * delta, velocity.y * delta)

        if (!game.getGameCamera().getRotatedBounds().contains(position)) {
            GameLogger.debug(TAG, "update(): out of game cam, destroy")
            destroy()
            return@UpdatablesComponent
        }

        fadeTimer.update(delta)
        if (fadeTimer.isFinished()) {
            GameLogger.debug(TAG, "update(): finish fading, destroy")
            destroy()
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 2))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            sprite.setCenter(position)

            val alpha = 1f - (floor(fadeTimer.getRatio() * 10) / 10)
            sprite.setAlpha(alpha)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
