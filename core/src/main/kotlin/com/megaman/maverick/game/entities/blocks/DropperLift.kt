package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint

class DropperLift(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

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

    private val loop = Loop(DropperLiftState.entries.toTypedArray().toGdxArray())
    private val timer = Timer(DURATION)

    override fun init() {
        super.init()
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

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.BODY_LABELS, objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY))
        super.onSpawn(spawnProps)
        loop.reset()
        timer.reset()
    }

    private fun isMegamanOverlapping() =
        megaman.feetFixture.getShape().overlaps(body.getBounds()) ||
            megaman.body.getBounds().overlaps(body.getBounds())

    private fun setActive(active: Boolean) {
        body.physics.collisionOn = active
        blockFixture.setActive(active)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
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
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(ConstVals.PPM * 1.5f, ConstVals.PPM.toFloat())

        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.TOP_CENTER), Position.TOP_CENTER)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { currentState.name }
        val animations = objectMapOf<String, IAnimation>(
            DropperLiftState.CLOSED.name pairTo Animation(closedRegion!!),
            DropperLiftState.OPENING.name pairTo Animation(openingRegion!!, 2, 2, 0.1f, false),
            DropperLiftState.OPEN.name pairTo Animation(openRegion!!),
            DropperLiftState.CLOSING.name pairTo Animation(openingRegion!!, 2, 2, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}
