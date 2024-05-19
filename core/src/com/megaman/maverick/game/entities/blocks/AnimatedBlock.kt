package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.interfaces.ArgsPredicate
import com.engine.common.interfaces.Resettable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

open class AnimatedBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity, Resettable {

    companion object {
        const val TAG = "AnimatedBlock"
    }

    lateinit var region: TextureRegion
    lateinit var trajectory: Vector2
    lateinit var spriteSize: Vector2

    var deathPredicate: ArgsPredicate<Properties>? = null
    var hidden = false

    override fun init() {
        super<Block>.init()
        addComponent(defineSpritesComponent())
        addComponent(AnimationsComponent(this, Array()))
        val updateablesComponent = UpdatablesComponent(this)
        defineUpdateablesComponent(updateablesComponent)
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.spawn(spawnProps)
        trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        deathPredicate = spawnProps.get(ConstKeys.DEATH) as ArgsPredicate<Properties>?
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        spriteSize = spawnProps.getOrDefault(ConstKeys.SIZE, bounds.getSize(), Vector2::class)
        spriteSize.x = spawnProps.getOrDefault(ConstKeys.WIDTH, bounds.width, Float::class)
        spriteSize.y = spawnProps.getOrDefault(ConstKeys.HEIGHT, bounds.height, Float::class)
        val animation = spawnProps.get(ConstKeys.ANIMATION, String::class)!!
        AnimatedBlockAnimators.createAndSetAnimations(animation, this)
        if (spawnProps.containsKey(ConstKeys.RUN_ON_SPAWN)) {
            val runOnSpawn = spawnProps.get(ConstKeys.RUN_ON_SPAWN, Runnable::class)!!
            runOnSpawn.run()
        }
    }

    protected open fun defineUpdateablesComponent(updateablesComponent: UpdatablesComponent) {
        updateablesComponent.add {
            body.physics.velocity = trajectory
            if (deathPredicate != null && deathPredicate!!.test(
                    props(
                        ConstKeys.DELTA to it, ConstKeys.ENTITY to this
                    )
                )
            ) kill(props(CAUSE_OF_DEATH_MESSAGE to "Death predicate returned true"))
        }
    }

    protected open fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setSize(spriteSize.x, spriteSize.y)
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = hidden
        }
        return spritesComponent
    }

    override fun reset() = animators.forEach { it.second.reset() }
}

object AnimatedBlockAnimators {

    const val TAG = "AnimatedBlockAnimators"

    fun createAndSetAnimations(key: String, animatedBlock: AnimatedBlock) {
        val animators = animatedBlock.animators
        animators.clear()

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
        animators.add({ animatedBlock.firstSprite!! } to animator)
    }
}
