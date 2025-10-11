package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Trajectory
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPosition
import com.megaman.maverick.game.world.body.getBounds
import kotlin.math.ceil

class PreciousBlock(game: MegamanMaverickGame) : ShieldBlock(game), ISpritesEntity, IAnimatedEntity, IMotionEntity {

    companion object {
        const val TAG = "PreciousBlock"
        private var region: TextureRegion? = null
    }

    private val cells = Matrix<GameRectangle>()
    private var spawn: Vector2? = null

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(MotionComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ANIMATION, TAG)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val createSprites = spawnProps.getOrDefault("${ConstKeys.CREATE}_${ConstKeys.SPRITE}", true, Boolean::class)
        if (createSprites) {
            val animIndex = spawnProps.getOrDefault("${ConstKeys.ANIMATION}_${ConstKeys.INDEX}", null) as Int?
            val rows = ceil(bounds.getHeight() / ConstVals.PPM).toInt()
            val cols = ceil(bounds.getWidth() / ConstVals.PPM).toInt()
            defineDrawables(rows, cols, animIndex)
        }

        if (spawnProps.containsKey("${ConstKeys.TRAJECTORY}_${ConstKeys.DEF}")) {
            val trajectoryDefinition = spawnProps.get("${ConstKeys.TRAJECTORY}_${ConstKeys.DEF}", String::class)!!

            spawn = bounds.getCenter(false)

            val trajectory = Trajectory(trajectoryDefinition, ConstVals.PPM)
            putMotionDefinition(
                ConstKeys.TRAJECTORY, MotionDefinition(
                    motion = trajectory,
                    onReset = { spawn?.let { body.setCenter(it) } },
                    function = { value, _ -> body.physics.velocity.set(value) }
                )
            )
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        cells.clear()

        sprites.clear()
        animators.clear()

        if (this.spawn != null) {
            GameObjectPools.free(this.spawn!!)
            this.spawn = null
        }

        removeMotionDefinition(ConstKeys.TRAJECTORY)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        body.getBounds().splitByCellSize(ConstVals.PPM.toFloat(), cells)
    })

    private fun defineDrawables(rows: Int, cols: Int, animIndex: Int? = null) {
        for (row in 0 until rows) for (col in 0 until cols) {
            val key = "${row}_${col}"

            val sprite = GameSprite()
            sprite.setSize(ConstVals.PPM.toFloat())

            sprites.put(key, sprite)

            putSpriteUpdateFunction(key) { _, _ ->
                val position = body.getBounds()
                    .translate(
                        col * ConstVals.PPM.toFloat(),
                        row * ConstVals.PPM.toFloat()
                    )
                    .getPosition()
                sprite.setPosition(position)
            }

            val animation = Animation(region!!, 3, 1, 0.2f, true)
            animation.setIndex(animIndex ?: ((row + col) % 3))

            val animator = Animator(animation)
            putAnimator(key, sprite, animator)
        }
    }
}
