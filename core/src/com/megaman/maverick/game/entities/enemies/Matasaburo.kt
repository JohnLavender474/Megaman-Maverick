package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.VelocityAlteration

import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.setVelocityAlteration
import kotlin.reflect.KClass

class Matasaburo(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "Matasaburo"
        private const val BLOW_FORCE = 25f
        private var matasaburoReg: TextureRegion? = null
    }

    override var facing = Facing.RIGHT
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 5
        }
    )

    override fun init() {
        super.init()
        if (matasaburoReg == null) matasaburoReg =
            game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "Matasaburo")
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val blowFixture = Fixture(
            body,
            FixtureType.FORCE,
            GameRectangle().setSize(10f * ConstVals.PPM, 1.15f * ConstVals.PPM)
        )
        blowFixture.setVelocityAlteration { fixture, _ ->
            val entity = fixture.getEntity()
            if (entity is AbstractEnemy) return@setVelocityAlteration VelocityAlteration.addNone()
            if (entity is AbstractProjectile) entity.owner = null
            val force = BLOW_FORCE * ConstVals.PPM * facing.value
            return@setVelocityAlteration VelocityAlteration.add(force, 0f)
        }
        body.addFixture(blowFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.85f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damageableFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val offsetX = 5f * ConstVals.PPM * facing.value
            blowFixture.offsetFromBodyCenter.x = offsetX
        })

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val position = body.getBottomCenterPoint()
            _sprite.setPosition(position, Position.BOTTOM_CENTER)
            _sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (game.megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(matasaburoReg!!, 1, 6, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
