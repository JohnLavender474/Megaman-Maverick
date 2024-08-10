package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.audio.AudioComponent
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

import com.megaman.maverick.game.world.FixtureType

class FlipperPlatform(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, IAudioEntity {

    companion object {
        const val TAG = "FlipperPlatform"
        private const val SWITCH_DELAY = 0.4f
        private const val SWITCH_DURATION = 0.25f
        private var leftRegion: TextureRegion? = null
        private var rightRegion: TextureRegion? = null
        private var leftDelayRegion: TextureRegion? = null
        private var rightDelayRegion: TextureRegion? = null
        private var flipToRightRegion: TextureRegion? = null
        private var flipToLeftRegion: TextureRegion? = null
    }

    private enum class FlipperPlatformState {
        LEFT, RIGHT, FLIP_TO_RIGHT, FLIP_TO_LEFT
    }

    private val switchDelay = Timer(SWITCH_DELAY)
    private val switchTimer = Timer(SWITCH_DURATION)

    private lateinit var state: FlipperPlatformState
    private lateinit var bounds: GameRectangle

    private var block: Block? = null

    override fun init() {
        if (leftRegion == null || rightRegion == null || flipToRightRegion == null || flipToLeftRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            leftRegion = atlas.findRegion("FlipperPlatform/Left")
            rightRegion = atlas.findRegion("FlipperPlatform/Right")
            leftDelayRegion = atlas.findRegion("FlipperPlatform/LeftDelay")
            rightDelayRegion = atlas.findRegion("FlipperPlatform/RightDelay")
            flipToRightRegion = atlas.findRegion("FlipperPlatform/FlipToRight")
            flipToLeftRegion = atlas.findRegion("FlipperPlatform/FlipToLeft")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        switchTimer.reset()
        switchDelay.setToEnd()

        state = FlipperPlatformState.LEFT
        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!

        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        game.engine.spawn(
            block!!, props(
                ConstKeys.BOUNDS to GameRectangle().setSize(1.1875f * ConstVals.PPM, 0.25f * ConstVals.PPM)
                    .setX(-100f * ConstVals.PPM), ConstKeys.CULL_OUT_OF_BOUNDS to false
            )
        )
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        block?.kill()
        block = null
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        block!!.setSize(ConstVals.PPM.toFloat(), 0.25f * ConstVals.PPM)

        switchDelay.update(delta)
        if (switchDelay.isJustFinished()) {
            switchTimer.reset()
            state = when (state) {
                FlipperPlatformState.LEFT -> FlipperPlatformState.FLIP_TO_RIGHT
                FlipperPlatformState.RIGHT -> FlipperPlatformState.FLIP_TO_LEFT
                else -> throw IllegalStateException("Invalid state during switch delay: $state")
            }
        }

        when (state) {
            FlipperPlatformState.FLIP_TO_RIGHT -> {
                block!!.body.setCenterX(-100f * ConstVals.PPM)
                switchTimer.update(delta)
                if (switchTimer.isFinished()) {
                    state = FlipperPlatformState.RIGHT
                    switchTimer.reset()
                }
            }

            FlipperPlatformState.FLIP_TO_LEFT -> {
                block!!.body.setCenterX(-100f * ConstVals.PPM)
                switchTimer.update(delta)
                if (switchTimer.isFinished()) {
                    state = FlipperPlatformState.LEFT
                    switchTimer.reset()
                }
            }

            FlipperPlatformState.LEFT -> {
                val position = bounds.getTopCenterPoint()
                block!!.body.setTopRightToPoint(position)
                block!!.body.x -= 0.25f * ConstVals.PPM
                block!!.body.y -= 0.3f * ConstVals.PPM

                val megamanFeet = game.megaman.body.fixtures.first { pair ->
                    pair.second.getFixtureType() == FixtureType.FEET
                }.second.getShape() as Rectangle
                if (switchDelay.isFinished() && block!!.body.overlaps(megamanFeet)) {
                    switchDelay.reset()
                    requestToPlaySound(SoundAsset.BLOOPITY_SOUND, false)
                }
            }


            FlipperPlatformState.RIGHT -> {
                val position = bounds.getTopCenterPoint()
                block!!.body.setTopLeftToPoint(position)
                block!!.body.x += 0.25f * ConstVals.PPM
                block!!.body.y -= 0.3f * ConstVals.PPM

                val megamanFeet = getMegaman().body.fixtures.first { pair ->
                    pair.second.getFixtureType() == FixtureType.FEET
                }.second.getShape() as Rectangle
                if (switchDelay.isFinished() && block!!.body.overlaps(megamanFeet)) {
                    switchDelay.reset()
                    requestToPlaySound(SoundAsset.BLOOPITY_SOUND, false)
                }
            }
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val gameCamera = game.getGameCamera()
        val cullable = getGameCameraCullingLogic(gameCamera, { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullable))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.6875f * ConstVals.PPM, 1.875f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(bounds.getTopCenterPoint(), Position.TOP_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { when (state) {
            FlipperPlatformState.LEFT -> if (switchDelay.isFinished()) "left" else "leftDelay"
            FlipperPlatformState.RIGHT -> if (switchDelay.isFinished()) "right" else "rightDelay"
            FlipperPlatformState.FLIP_TO_RIGHT -> "flipToRight"
            FlipperPlatformState.FLIP_TO_LEFT -> "flipToLeft"
        } }
        val animations = objectMapOf<String, IAnimation>(
            "left" to Animation(leftRegion!!),
            "right" to Animation(rightRegion!!),
            "leftDelay" to Animation(leftDelayRegion!!, 1, 4, 0.1f, false),
            "rightDelay" to Animation(rightDelayRegion!!, 1, 4, 0.1f, false),
            "flipToRight" to Animation(flipToRightRegion!!, 1, 5, 0.05f, false),
            "flipToLeft" to Animation(flipToLeftRegion!!, 1, 5, 0.05f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}