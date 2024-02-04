package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.map
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.time.Timer
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.FixtureType

class DropperLift(game: MegamanMaverickGame) : Block(game), ISpriteEntity, IAnimatedEntity {

    enum class DropperLiftState {
        CLOSED, OPENING, OPEN, CLOSING
    }

    companion object {
        const val TAG = "DropperLift"
        private var closedRegion: TextureRegion? = null
        private var openingRegion: TextureRegion? = null
        private var openRegion: TextureRegion? = null
        private const val DURATION = 0.3f
    }

    val currentState: DropperLiftState
        get() = loop.getCurrent()

    private val loop = Loop(DropperLiftState.values().toGdxArray())
    private val timer = Timer(DURATION)

    override fun init() {
        super<Block>.init()
        if (closedRegion == null || openingRegion == null || openRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            closedRegion = atlas.findRegion("DropperLift/Closed")
            openingRegion = atlas.findRegion("DropperLift/Opening")
            openRegion = atlas.findRegion("DropperLift/Open")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        loop.reset()
        timer.reset()
    }

    private fun isMegamanOverlapping(): Boolean {
        val megaman = getMegamanMaverickGame().megaman
        val megamanFeet = megaman.body.fixtures.map { it.second }.find { it.fixtureLabel == FixtureType.FEET }!!
        return megamanFeet.shape.overlaps(body) || megaman.body.overlaps(body as Rectangle)
    }

    private fun setActive(active: Boolean) {
        body.physics.collisionOn = active
        blockFixture.active = active
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        when (currentState) {
            DropperLiftState.CLOSED -> {
                setActive(true)
                if (isMegamanOverlapping()) {
                    timer.reset()
                    loop.next()
                }
            }

            DropperLiftState.OPENING -> {
                setActive(true)
                if (isMegamanOverlapping()) {
                    timer.update(delta)
                    if (timer.isFinished()) loop.next()
                } else loop.setIndex(DropperLiftState.CLOSED.ordinal)
            }

            DropperLiftState.OPEN -> {
                setActive(false)
                if (!isMegamanOverlapping()) {
                    timer.reset()
                    loop.next()
                }
            }

            DropperLiftState.CLOSING -> {
                setActive(false)
                timer.update(delta)
                if (timer.isFinished()) loop.next()
            }
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM * 1.25f, ConstVals.PPM.toFloat())

        val spritesComponent = SpritesComponent(this, "dropperLift" to sprite)
        spritesComponent.putUpdateFunction("dropperLift") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { currentState.name }
        val animations = objectMapOf<String, IAnimation>(
            DropperLiftState.CLOSED.name to Animation(closedRegion!!),
            DropperLiftState.OPENING.name to Animation(openingRegion!!, 2, 2, 0.1f, false),
            DropperLiftState.OPEN.name to Animation(openRegion!!),
            DropperLiftState.CLOSING.name to Animation(openingRegion!!, 2, 2, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}