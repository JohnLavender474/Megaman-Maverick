package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Matasaburo(services: MegaServices) : AbstractEnemy(services, size = Size.MEDIUM), IFaceable {

    companion object {
        const val TAG = "Matasaburo"
        private const val BLOW_FORCE = 20f
        private const val BLOW_MAX = 8f
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    override fun init(vararg params: Any) {
        super.init()
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.ENEMIES_1.source, TAG)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = if (targetPlayer.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.75f * ConstVals.PPM)

        val blowFixture = Fixture(
            body,
            FixtureType.FORCE,
            GameRectangle().setSize(10f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        )
        blowFixture.setReceiver(Receivers.VELOCITY_ALTERATION) { fixture, delta, _ ->
            val entity = fixture.getEntity()
            val player = entity.asPlayer ?: return@setReceiver VelocityAlteration.addNone()

            /*
            TODO: for now, ONLY apply force to Megaman and no other entities
            if (entity is AbstractEnemy) return@setReceiver VelocityAlteration.addNone()
            if (entity is AbstractProjectile) entity.owner = null
             */

            if ((isFacing(Facing.LEFT) && player.body.physics.velocity.x <= -BLOW_MAX * ConstVals.PPM) ||
                (isFacing(Facing.RIGHT) && player.body.physics.velocity.x >= BLOW_MAX * ConstVals.PPM)
            ) return@setReceiver VelocityAlteration.addNone()

            val force = BLOW_FORCE * ConstVals.PPM * facing.value
            return@setReceiver VelocityAlteration.add(force * delta, 0f)
        }
        body.addFixture(blowFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val offsetX = 5f * ConstVals.PPM * facing.value
            blowFixture.offsetFromBodyAttachment.x = offsetX
        }

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.hidden = damageBlink
            val position = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (targetPlayer.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 6, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
