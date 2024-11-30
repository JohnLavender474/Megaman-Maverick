package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.ArgsPredicate
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getSize
import com.megaman.maverick.game.world.body.getCenter

open class AnimatedBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "AnimatedBlock"
    }

    lateinit var region: TextureRegion
    lateinit var trajectory: Vector2
    lateinit var spriteSize: Vector2

    var deathPredicate: ArgsPredicate<Properties>? = null
    var hidden = false

    override fun init() {
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(AnimationsComponent())
        val updateablesComponent = UpdatablesComponent()
        defineUpdateablesComponent(updateablesComponent)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)

        trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class).cpy()
        deathPredicate = spawnProps.get(ConstKeys.DEATH) as ArgsPredicate<Properties>?

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        spriteSize = spawnProps.getOrDefault(ConstKeys.SIZE, bounds.getSize(), Vector2::class)
        spriteSize.x = spawnProps.getOrDefault(ConstKeys.WIDTH, bounds.getWidth(), Float::class)
        spriteSize.y = spawnProps.getOrDefault(ConstKeys.HEIGHT, bounds.getHeight(), Float::class)

        val animation = spawnProps.get(ConstKeys.ANIMATION, String::class)!!
        AnimatedBlockAnimators.createAndSetAnimations(animation, this)

        if (spawnProps.containsKey(ConstKeys.RUN_ON_SPAWN)) {
            val runOnSpawn = spawnProps.get(ConstKeys.RUN_ON_SPAWN, Runnable::class)!!
            runOnSpawn.run()
        }

        if (spawnProps.containsKey(ConstKeys.KEY)) putProperty(ConstKeys.KEY, spawnProps.get(ConstKeys.KEY))
    }

    protected open fun defineUpdateablesComponent(updateablesComponent: UpdatablesComponent) {
        updateablesComponent.add {
            val velocity = GameObjectPools.fetch(Vector2::class).set(trajectory)
            body.physics.velocity.set(velocity)

            if (deathPredicate != null &&
                deathPredicate!!.test(props(ConstKeys.DELTA pairTo it, ConstKeys.ENTITY pairTo this))
            ) destroy()
        }
    }

    protected open fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setSize(spriteSize.x, spriteSize.y)
            sprite.setCenter(body.getCenter())
            sprite.hidden = hidden
        }
        return spritesComponent
    }
}

object AnimatedBlockAnimators {

    const val TAG = "AnimatedBlockAnimators"

    fun createAndSetAnimations(key: String, animatedBlock: AnimatedBlock) {
        val assMan = animatedBlock.game.assMan

        val animation: Animation = when (key) {
            "Brick1" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "Brick1")
                Animation(region, 1, 3, 0.05f, false)
            }

            "CaveRock" -> {
                val region = assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "CaveRock/Rock")
                Animation(region)
            }

            "Platform1_64x8" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "Platform1_64x8")
                Animation(region)
            }

            else -> throw IllegalArgumentException("$TAG: Illegal key = $key")
        }

        val animator = Animator(animation)
        animatedBlock.putAnimator(animatedBlock.defaultSprite, animator)
    }
}
