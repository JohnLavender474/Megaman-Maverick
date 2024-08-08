package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.extensions.getTextureRegion
import com.engine.common.getRandom
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaGameEntity

class FallingLeaf(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "FallingLeaf"
        private var region: TextureRegion? = null
        private const val DEFAULT_MIN_TRAJECTORY_X = -0.25f
        private const val DEFAULT_MAX_TRAJECTORY_X = -3f
        private const val DEFAULT_MIN_TRAJECTORY_Y = -0.25f
        private const val DEFAULT_MAX_TRAJECTORY_Y = -3f
        private const val DEFAULT_MIN_FALL_DURATION = 0.5f
        private const val DEFAULT_MAX_FALL_DURATION = 2f
        private const val DEFAULT_MIN_ELAPSE_DURATION = 0.5f
        private const val DEFAULT_MAX_ELAPSE_DURATION = 2f
    }

    private lateinit var spawnPosition: Vector2
    private lateinit var currentPosition: Vector2

    private lateinit var currentTrajectory: Vector2
    private lateinit var minTrajectory: Vector2
    private lateinit var maxTrajectory: Vector2

    private lateinit var fallTimer: Timer
    private lateinit var elapseTimer: Timer

    private var minFallDuration = DEFAULT_MIN_FALL_DURATION
    private var maxFallDuration = DEFAULT_MAX_FALL_DURATION
    private var minElapseDuration = DEFAULT_MIN_ELAPSE_DURATION
    private var maxElapseDuration = DEFAULT_MAX_ELAPSE_DURATION

    private var hidden = true

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENVIRONS_1.source, "Wood/FallingLeaf")
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineDrawableShapesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        spawnPosition = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        currentPosition = Vector2(spawnPosition)

        val minX = spawnProps.getOrDefault(
            "${ConstKeys.MIN}_${ConstKeys.TRAJECTORY}_${ConstKeys.X}",
            DEFAULT_MIN_TRAJECTORY_X, Float::class
        )
        val minY = spawnProps.getOrDefault(
            "${ConstKeys.MIN}_${ConstKeys.TRAJECTORY}_Y",
            DEFAULT_MIN_TRAJECTORY_Y, Float::class
        )
        minTrajectory = Vector2(minX, minY)

        val maxX = spawnProps.getOrDefault(
            "${ConstKeys.MAX}_${ConstKeys.TRAJECTORY}_${ConstKeys.X}",
            DEFAULT_MAX_TRAJECTORY_X, Float::class
        )
        val maxY = spawnProps.getOrDefault(
            "${ConstKeys.MAX}_${ConstKeys.TRAJECTORY}_Y",
            DEFAULT_MAX_TRAJECTORY_Y, Float::class
        )
        maxTrajectory = Vector2(maxX, maxY)

        currentTrajectory =
            Vector2(getRandom(minTrajectory.x, maxTrajectory.x), getRandom(minTrajectory.y, maxTrajectory.y))

        minFallDuration = spawnProps.getOrDefault(ConstKeys.FALL, DEFAULT_MIN_FALL_DURATION, Float::class)
        maxFallDuration = spawnProps.getOrDefault(ConstKeys.FALL, DEFAULT_MAX_FALL_DURATION, Float::class)
        fallTimer = Timer(getRandom(minFallDuration, maxFallDuration))

        minElapseDuration = spawnProps.getOrDefault(ConstKeys.ELAPSE, DEFAULT_MIN_ELAPSE_DURATION, Float::class)
        maxElapseDuration = spawnProps.getOrDefault(ConstKeys.ELAPSE, DEFAULT_MAX_ELAPSE_DURATION, Float::class)
        elapseTimer = Timer(getRandom(minElapseDuration, maxElapseDuration))

        hidden = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        if (hidden) {
            elapseTimer.update(delta)
            if (elapseTimer.isFinished()) {
                currentPosition.set(spawnPosition)
                hidden = false

                elapseTimer.resetDuration(getRandom(minElapseDuration, maxElapseDuration))
                currentTrajectory.set(
                    getRandom(minTrajectory.x, maxTrajectory.x),
                    getRandom(minTrajectory.y, maxTrajectory.y)
                )
            }
            return@UpdatablesComponent
        }

        currentPosition.add(currentTrajectory.cpy().scl(delta * ConstVals.PPM))

        fallTimer.update(delta)
        if (fallTimer.isFinished()) {
            hidden = true
            fallTimer.resetDuration(getRandom(minFallDuration, maxFallDuration))
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, gameSprite ->
            gameSprite.setCenter(currentPosition)
            gameSprite.hidden = hidden
            val alpha = 1f - fallTimer.getRatio()
            gameSprite.setAlpha(alpha)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 10, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineDrawableShapesComponent(): DrawableShapesComponent {
        val shapes = Array<() -> IDrawableShape?>()
        shapes.add { firstSprite.boundingRectangle.toGameRectangle() }
        return DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true)
    }
}