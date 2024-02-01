package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.audio.AudioComponent
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import com.megaman.maverick.game.world.setVelocityAlteration

class SpringBouncer(game: MegamanMaverickGame) :
    GameEntity(game), ISpriteEntity, IBodyEntity, IAudioEntity {

    companion object {
        private var atlas: TextureAtlas? = null
        private const val BOUNCE_DURATION = .5f
        private const val X_BOUNCE = 25f
        private const val Y_BOUNCE = 18f
        private const val SPRITE_DIM = 1.5f
    }

    private val bounceTimer = Timer(BOUNCE_DURATION)

    lateinit var direction: Direction
        private set

    private lateinit var bounceFixture: Fixture

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
        addComponent(AudioComponent(this))
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        bounceTimer.setToEnd()
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        (bounceFixture.shape as GameRectangle).set(bounds)
        val directionString = spawnProps.get(ConstKeys.DIRECTION, String::class)!!
        direction =
            when (directionString) {
                ConstKeys.UP -> Direction.UP
                ConstKeys.DOWN -> Direction.DOWN
                ConstKeys.LEFT -> Direction.LEFT
                ConstKeys.RIGHT -> Direction.RIGHT
                else ->
                    throw IllegalArgumentException("Incompatible value for direction: $directionString")
            }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)

        bounceFixture = Fixture(GameRectangle(), FixtureType.BOUNCER)
        bounceFixture.setVelocityAlteration { fixture, _ -> bounce(fixture) }
        body.addFixture(bounceFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { bounceTimer.update(it) })

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(SPRITE_DIM * ConstVals.PPM)
        sprite.setOriginCenter()

        val SpritesComponent = SpritesComponent(this, "spring_bouncer" to sprite)
        SpritesComponent.putUpdateFunction("spring_bouncer") { _, _sprite ->
            _sprite as GameSprite
            _sprite.rotation =
                when (direction) {
                    Direction.UP -> 0f
                    Direction.DOWN -> 180f
                    Direction.LEFT -> 90f
                    Direction.RIGHT -> 270f
                }
            val position =
                when (direction) {
                    Direction.UP -> Position.BOTTOM_CENTER
                    Direction.DOWN -> Position.TOP_CENTER
                    Direction.LEFT -> Position.CENTER_RIGHT
                    Direction.RIGHT -> Position.CENTER_LEFT
                }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
        }

        return SpritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (bounceTimer.isFinished()) "still" else "bounce" }
        val animations =
            objectMapOf<String, IAnimation>(
                "still" to Animation(atlas!!.findRegion("SpringBounceStill")),
                "bounce" to Animation(atlas!!.findRegion("SpringBounce"), 1, 5, 0.05f, true)
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun bounce(fixture: Fixture): VelocityAlteration {
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
        bounceTimer.reset()

        val bounce = VelocityAlteration()
        when (direction) {
            Direction.UP -> bounce.forceY = Y_BOUNCE * ConstVals.PPM
            Direction.DOWN -> bounce.forceY = -Y_BOUNCE * ConstVals.PPM
            Direction.LEFT -> bounce.forceX = -X_BOUNCE * ConstVals.PPM
            Direction.RIGHT -> bounce.forceX = X_BOUNCE * ConstVals.PPM
        }

        if (fixture.getEntity() is Megaman) {
            val controllerPoller = game.controllerPoller
            if ((direction == Direction.UP &&
                        controllerPoller.isPressed(ControllerButton.UP)) ||
                (direction == Direction.DOWN &&
                        controllerPoller.isPressed(ControllerButton.DOWN))
            ) {
                bounce.forceY *= 2f
            } else if ((direction == Direction.LEFT &&
                        controllerPoller.isPressed(ControllerButton.LEFT)) ||
                (direction == Direction.RIGHT &&
                        controllerPoller.isPressed(ControllerButton.RIGHT))
            ) {
                bounce.forceX *= 2f
            }
        }

        return bounce
    }
}
