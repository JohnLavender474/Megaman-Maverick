package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.VelocityAlteration

import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class MagFly(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        private const val FORCE_FLASH_DURATION = .1f
        private const val X_VEL_NORMAL = 3f
        private const val X_VEL_SLOW = 1f
        private const val PULL_FORCE_X = 6f
        private const val PULL_FORCE_Y = 68f

        private var magFlyReg: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
    )

    override var facing = Facing.RIGHT

    private val forceFlashTimer = Timer(FORCE_FLASH_DURATION)
    private var flash = false
    private lateinit var forceFixture: Fixture

    override fun init() {
        super.init()
        if (magFlyReg == null) magFlyReg = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "MagFly")
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(bodyFixture)

        shapes.add { bodyFixture.getShape() }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyCenter.x = -0.6f * ConstVals.PPM
        leftFixture.offsetFromBodyCenter.y = 0.4f * ConstVals.PPM
        body.addFixture(leftFixture)

        shapes.add { leftFixture.getShape() }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyCenter.x = 0.6f * ConstVals.PPM
        rightFixture.offsetFromBodyCenter.y = 0.4f * ConstVals.PPM
        body.addFixture(rightFixture)

        shapes.add { rightFixture.getShape() }

        forceFixture = Fixture(
            body,
            FixtureType.FORCE,
            GameRectangle().setSize(ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        )
        forceFixture.offsetFromBodyCenter.y = -ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        forceFixture.setVelocityAlteration { fixture, _ ->
            val entity = fixture.getEntity()
            if (entity is AbstractEnemy || (entity is Megaman && entity.damaged)) return@setVelocityAlteration VelocityAlteration.addNone()
            if (entity is AbstractProjectile) entity.owner = null

            val x = PULL_FORCE_X * ConstVals.PPM * facing.value
            val y = PULL_FORCE_Y * ConstVals.PPM

            return@setVelocityAlteration VelocityAlteration.add(x, y)
        }
        body.addFixture(forceFixture)

        forceFixture.rawShape.color = Color.YELLOW
        shapes.add { forceFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        shapes.add { damageableFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.85f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        shapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val SpritesComponent = SpritesComponent(this, sprite)
        SpritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(facing == Facing.LEFT, false)
        }
        return SpritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            forceFlashTimer.update(it)
            if (forceFlashTimer.isFinished()) {
                flash = !flash
                forceFlashTimer.reset()
            }

            val slow = getMegaman().body.overlaps(forceFixture.getShape() as Rectangle)

            if (!slow && getMegaman().body.y < body.y && !facingAndMMDirMatch()) facing =
                if (getMegaman().body.x > body.x) Facing.RIGHT else Facing.LEFT

            if ((facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) || (facing == Facing.RIGHT && body.isSensing(
                    BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                ))
            ) body.physics.velocity.x = 0f
            else body.physics.velocity.x = (if (slow) X_VEL_SLOW else X_VEL_NORMAL) * ConstVals.PPM * facing.value
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(magFlyReg!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun facingAndMMDirMatch() =
        (game.megaman.body.x > body.x && facing == Facing.RIGHT) || (game.megaman.body.x < body.x && facing == Facing.LEFT)
}
