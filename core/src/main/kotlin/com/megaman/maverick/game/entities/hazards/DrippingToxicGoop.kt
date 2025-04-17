package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
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
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.megaman.components.headFixture
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getBounds

class DrippingToxicGoop(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity {

    companion object {
        const val TAG = "DrippingToxicGoop"
        private const val SMOKE_PUFF_DELAY = 0.25f
        private var region: TextureRegion? = null
    }

    private val bounds = GameRectangle()
    private val smokePuffDelay = Timer(SMOKE_PUFF_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        defineDrawables(bounds)
        smokePuffDelay.reset()
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val rows = (bounds.getHeight() / ConstVals.PPM).toInt()
        val columns = (bounds.getWidth() / ConstVals.PPM).toInt()

        for (x in 0 until columns) {
            for (y in 0 until rows) {
                val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
                sprite.setBounds(
                    bounds.getX() + x * ConstVals.PPM,
                    bounds.getY() + y * ConstVals.PPM,
                    ConstVals.PPM.toFloat(),
                    ConstVals.PPM.toFloat()
                )
                sprites.put("${x}_${y}", sprite)

                val animation = Animation(region!!, 2, 2, 0.1f, true)
                val animator = Animator(animation)
                animators.add({ sprite } pairTo animator)
            }
        }

        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        var megamanTouching = false
        var smokePuffDirection = megaman.direction

        when {
            megaman.feetFixture.getShape().overlaps(bounds) -> {
                megamanTouching = true
                smokePuffDirection = megaman.direction
            }

            megaman.leftSideFixture.getShape().overlaps(bounds) -> {
                megamanTouching = true
                smokePuffDirection = megaman.direction.getRotatedClockwise()
            }

            megaman.rightSideFixture.getShape().overlaps(bounds) -> {
                megamanTouching = true
                smokePuffDirection = megaman.direction.getRotatedCounterClockwise()
            }

            megaman.headFixture.getShape().overlaps(bounds) -> {
                megamanTouching = true
                smokePuffDirection = megaman.direction.getOpposite()
            }
        }

        when {
            megamanTouching -> {
                val spawnSmokePosition = megaman.body.getBounds()
                    .getPositionPoint(DirectionPositionMapper.getInvertedPosition(smokePuffDirection))

                when {
                    !megaman.invincible -> {
                        spawnSmokePuff(spawnSmokePosition, smokePuffDirection)

                        megaman.translateHealth(-1)
                        megaman.damageRecoveryTimer.reset()

                        requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)
                        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)
                    }

                    else -> {
                        smokePuffDelay.update(delta)
                        if (smokePuffDelay.isFinished()) spawnSmokePuff(spawnSmokePosition, smokePuffDirection)
                    }
                }
            }

            else -> smokePuffDelay.setToEnd()
        }
    })

    private fun spawnSmokePuff(position: Vector2, direction: Direction) {
        smokePuffDelay.reset()

        val smokePuff = MegaEntityFactory.fetch(SmokePuff::class)!!
        smokePuff.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo position,
                ConstKeys.DIRECTION pairTo direction
            )
        )
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
