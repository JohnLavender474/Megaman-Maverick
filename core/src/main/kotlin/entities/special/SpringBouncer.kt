package com.megaman.maverick.game.entities.special


import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.setVelocityAlteration

class SpringBouncer(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IBodyEntity, IAudioEntity {

    companion object {
        private var atlas: TextureAtlas? = null
        private const val BOUNCE_DURATION = 0.5f
        private const val BOUNCE = 25f
        private const val SPRITE_DIM = 1.5f
    }

    lateinit var direction: Direction
        private set
    private val bounceTimer = Timer(BOUNCE_DURATION)
    private lateinit var bounceFixture: Fixture

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        bounceTimer.setToEnd()
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        (bounceFixture.rawShape as GameRectangle).set(bounds)
        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class)
        direction =
            when (directionString) {
                ConstKeys.UP -> Direction.UP
                ConstKeys.DOWN -> Direction.DOWN
                ConstKeys.LEFT -> Direction.LEFT
                ConstKeys.RIGHT -> Direction.RIGHT
                else -> throw IllegalArgumentException("Incompatible value for direction: $directionString")
            }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)
        bounceFixture = Fixture(body, FixtureType.BOUNCER, GameRectangle())
        bounceFixture.setVelocityAlteration { fixture, _ -> bounce(fixture) }
        body.addFixture(bounceFixture)
        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ bounceTimer.update(it) })

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(SPRITE_DIM * ConstVals.PPM)
        sprite.setOriginCenter()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
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
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (bounceTimer.isFinished()) "still" else "bounce" }
        val animations =
            objectMapOf<String, IAnimation>(
                "still" pairTo Animation(atlas!!.findRegion("SpringBounceStill")),
                "bounce" pairTo Animation(atlas!!.findRegion("SpringBounce"), 1, 5, 0.05f, true)
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun bounce(fixture: IFixture): VelocityAlteration {
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
        bounceTimer.reset()

        val bounce = VelocityAlteration()
        when (direction) {
            Direction.UP -> bounce.forceY = BOUNCE * ConstVals.PPM
            Direction.DOWN -> bounce.forceY = -BOUNCE * ConstVals.PPM
            Direction.LEFT -> bounce.forceX = -BOUNCE * ConstVals.PPM
            Direction.RIGHT -> bounce.forceX = BOUNCE * ConstVals.PPM
        }

        if (fixture.getEntity() is Megaman) {
            val controllerPoller = game.controllerPoller
            if ((direction == Direction.UP && controllerPoller.isPressed(MegaControllerButton.UP)) ||
                (direction == Direction.DOWN && controllerPoller.isPressed(MegaControllerButton.DOWN))
            ) bounce.forceY *= 2f
            else if ((direction == Direction.LEFT && controllerPoller.isPressed(MegaControllerButton.LEFT)) ||
                (direction == Direction.RIGHT && controllerPoller.isPressed(MegaControllerButton.RIGHT))
            ) bounce.forceX *= 2f
        }

        return bounce
    }
}
