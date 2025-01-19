package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.setCenterX

class FlipperPlatform(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, IAudioEntity {

    companion object {
        const val TAG = "FlipperPlatform"

        private const val SWITCH_DELAY = 0.4f
        private const val SWITCH_DURATION = 0.25f

        private const val BLOCK_WIDTH = 2f
        private const val BLOCK_HEIGHT = 0.5f
        private const val OFFSET_X = 0.5f
        private const val OFFSET_Y = 0.75f

        private var leftRegion: TextureRegion? = null
        private var rightRegion: TextureRegion? = null
        private var leftDelayRegion: TextureRegion? = null
        private var rightDelayRegion: TextureRegion? = null
        private var flipToRightRegion: TextureRegion? = null
        private var flipToLeftRegion: TextureRegion? = null
    }

    private enum class FlipperPlatformState { LEFT, RIGHT, FLIP_TO_RIGHT, FLIP_TO_LEFT }

    private val switchDelay = Timer(SWITCH_DELAY)
    private val switchTimer = Timer(SWITCH_DURATION)

    private lateinit var flipperPlatformState: FlipperPlatformState

    private val bounds = GameRectangle()
    private var block: Block? = null

    override fun init() {
        if (leftRegion == null || rightRegion == null || flipToRightRegion == null || flipToLeftRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            leftRegion = atlas.findRegion("$TAG/Left")
            rightRegion = atlas.findRegion("$TAG/Right")
            leftDelayRegion = atlas.findRegion("$TAG/LeftDelay")
            rightDelayRegion = atlas.findRegion("$TAG/RightDelay")
            flipToRightRegion = atlas.findRegion("$TAG/FlipToRight")
            flipToLeftRegion = atlas.findRegion("$TAG/FlipToLeft")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        switchTimer.reset()
        switchDelay.setToEnd()

        flipperPlatformState = FlipperPlatformState.LEFT
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)

        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        block!!.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle()
                    .setSize(1.25f * ConstVals.PPM, 0.5f * ConstVals.PPM)
                    .setX(-100f * ConstVals.PPM),
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY),
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                ConstKeys.BLOCK_FILTERS pairTo gdxArrayOf(this::blockFilter),
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        block?.destroy()
        block = null
    }

    private fun blockFilter(entity1: MegaGameEntity, entity2: MegaGameEntity): Boolean {
        GameLogger.debug(TAG, "blockFilter(): entity1=$entity1, entity2=$entity2")
        return entity1 is Megaman &&
                (entity1.body.physics.velocity.y > 0f ||
                    !(entity2 as Block).body.getBounds().overlaps(entity1.feetFixture.getShape()))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        block!!.setSize(BLOCK_WIDTH * ConstVals.PPM, BLOCK_HEIGHT * ConstVals.PPM)

        switchDelay.update(delta)
        if (switchDelay.isJustFinished()) {
            switchTimer.reset()
            flipperPlatformState = when (flipperPlatformState) {
                FlipperPlatformState.LEFT -> FlipperPlatformState.FLIP_TO_RIGHT
                FlipperPlatformState.RIGHT -> FlipperPlatformState.FLIP_TO_LEFT
                else -> throw IllegalStateException("Invalid state during switch delay: $flipperPlatformState")
            }
        }

        when (flipperPlatformState) {
            FlipperPlatformState.FLIP_TO_RIGHT -> {
                block!!.body.setCenterX(-100f * ConstVals.PPM)

                switchTimer.update(delta)
                if (switchTimer.isFinished()) {
                    flipperPlatformState = FlipperPlatformState.RIGHT
                    switchTimer.reset()
                }
            }

            FlipperPlatformState.FLIP_TO_LEFT -> {
                block!!.body.setCenterX(-100f * ConstVals.PPM)

                switchTimer.update(delta)
                if (switchTimer.isFinished()) {
                    flipperPlatformState = FlipperPlatformState.LEFT
                    switchTimer.reset()
                }
            }

            FlipperPlatformState.LEFT -> {
                val position = bounds.getPositionPoint(Position.TOP_CENTER)
                block!!.body.setTopRightToPoint(position)
                block!!.body.translate(-OFFSET_X * ConstVals.PPM, -OFFSET_Y * ConstVals.PPM)

                if (switchDelay.isFinished() &&
                    block!!.body.getBounds().overlaps(megaman.feetFixture.getShape()) &&
                    megaman.body.physics.velocity.y <= 0f
                ) {
                    switchDelay.reset()
                    requestToPlaySound(SoundAsset.BLOOPITY_SOUND, false)
                }
            }


            FlipperPlatformState.RIGHT -> {
                val position = bounds.getPositionPoint(Position.TOP_CENTER)
                block!!.body.setTopLeftToPoint(position)
                block!!.body.translate(OFFSET_X * ConstVals.PPM, -OFFSET_Y * ConstVals.PPM)

                if (switchDelay.isFinished() &&
                    block!!.body.getBounds().overlaps(megaman.feetFixture.getShape()) &&
                    megaman.body.physics.velocity.y <= 0f
                ) {
                    switchDelay.reset()
                    requestToPlaySound(SoundAsset.BLOOPITY_SOUND, false)
                }
            }
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val gameCamera = game.getGameCamera()
        val cullable = getGameCameraCullingLogic(gameCamera, { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(6f * ConstVals.PPM, 4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(bounds.getPositionPoint(Position.TOP_CENTER), Position.TOP_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (flipperPlatformState) {
                FlipperPlatformState.LEFT -> if (switchDelay.isFinished()) "left" else "leftDelay"
                FlipperPlatformState.RIGHT -> if (switchDelay.isFinished()) "right" else "rightDelay"
                FlipperPlatformState.FLIP_TO_RIGHT -> "flipToRight"
                FlipperPlatformState.FLIP_TO_LEFT -> "flipToLeft"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "left" pairTo Animation(leftRegion!!),
            "right" pairTo Animation(rightRegion!!),
            "leftDelay" pairTo Animation(leftDelayRegion!!, 1, 4, 0.1f, false),
            "rightDelay" pairTo Animation(rightDelayRegion!!, 1, 4, 0.1f, false),
            "flipToRight" pairTo Animation(flipToRightRegion!!, 1, 5, 0.05f, false),
            "flipToLeft" pairTo Animation(flipToLeftRegion!!, 1, 5, 0.05f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
