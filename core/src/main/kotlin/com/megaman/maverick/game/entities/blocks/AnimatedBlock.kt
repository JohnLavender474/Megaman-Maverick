package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
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
import com.megaman.maverick.game.world.body.getCenter
import java.util.function.Predicate

open class AnimatedBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "AnimatedBlock"
    }

    lateinit var region: TextureRegion

    var deathPredicate: Predicate<Properties>? = null

    val trajectory = Vector2()
    var followTraj = false

    val spriteSize = Vector2()
    var hidden = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(AnimationsComponent())
        addComponent(defineSpritesComponent())
        val updateablesComponent = UpdatablesComponent()
        defineUpdateablesComponent(updateablesComponent)
        addComponent(updateablesComponent)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        if (spawnProps.containsKey(ConstKeys.TRAJECTORY)) {
            followTraj = true
            trajectory.set(spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class))
        } else followTraj = false

        deathPredicate = spawnProps.get(ConstKeys.DEATH) as Predicate<Properties>?

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        spriteSize.set(spawnProps.getOrDefault(ConstKeys.SIZE, bounds.getSize(spriteSize), Vector2::class))
        spriteSize.x = spawnProps.getOrDefault(ConstKeys.WIDTH, bounds.getWidth(), Float::class)
        spriteSize.y = spawnProps.getOrDefault(ConstKeys.HEIGHT, bounds.getHeight(), Float::class)

        val animation = spawnProps.get(ConstKeys.ANIMATION, String::class)!!
        AnimatedBlockAnimators.createAndSetAnimations(animation, this)

        if (spawnProps.containsKey(ConstKeys.RUN_ON_SPAWN)) {
            val runOnSpawn = spawnProps.get(ConstKeys.RUN_ON_SPAWN, Runnable::class)!!
            runOnSpawn.run()
        }

        if (spawnProps.containsKey(ConstKeys.KEY)) putProperty(ConstKeys.KEY, spawnProps.get(ConstKeys.KEY))
        else removeProperty(ConstKeys.KEY)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        animators.clear()
    }

    protected open fun defineUpdateablesComponent(updateablesComponent: UpdatablesComponent) {
        updateablesComponent.add {
            if (followTraj) {
                val velocity = GameObjectPools.fetch(Vector2::class).set(trajectory)
                body.physics.velocity.set(velocity)
            }

            if (deathPredicate != null &&
                deathPredicate!!.test(props(ConstKeys.DELTA pairTo it, ConstKeys.ENTITY pairTo this))
            ) destroy()
        }
    }

    protected open fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.setSize(spriteSize.x, spriteSize.y)
            sprite.setCenter(body.getCenter())
            sprite.hidden = hidden
        }
        return component
    }
}

object AnimatedBlockAnimators {

    const val TAG = "AnimatedBlockAnimators"

    fun createAndSetAnimations(key: String, block: AnimatedBlock) {
        val assMan = block.game.assMan

        val animation = when (key) {
            "AppearingBrick" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "AppearingBrick")
                Animation(region, 1, 3, 0.05f, false)
            }
            "AppearingBrick2" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "AppearingBrick2")
                Animation(region, 3, 1, 0.05f, false)
            }
            "CaveRock" -> {
                val region = assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "CaveRock/Rock")
                Animation(region)
            }
            "GoldBlock" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "GoldBlock")
                Animation(region, 2, 2, 0.1f, true)
            }
            "Platform1_64x8" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "Platform1_64x8")
                Animation(region)
            }
            "FireballBlock" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "FireballBlock")
                Animation(region)
            }
            "PreciousBlock" -> {
                val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "PreciousBlock")
                Animation(region, 3, 1, 0.2f, true)
            }
            else -> throw IllegalArgumentException("$TAG: Illegal key = $key")
        }

        val animator = Animator(animation)
        block.putAnimator(block.defaultSprite, animator)
    }
}
