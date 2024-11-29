package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.VelocityAlteration

import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class MagFly(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "MagFly"
        private const val FORCE_FLASH_DURATION = 0.1f
        private const val X_VEL_NORMAL = 3f
        private const val X_VEL_SLOW = 1f
        private const val PULL_FORCE_X = 6f
        private const val PULL_FORCE_Y = 68f

        private var magFlyReg: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
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

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture.getShape() }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -0.6f * ConstVals.PPM
        leftFixture.offsetFromBodyAttachment.y = 0.4f * ConstVals.PPM
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture.getShape() }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = 0.6f * ConstVals.PPM
        rightFixture.offsetFromBodyAttachment.y = 0.4f * ConstVals.PPM
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture.getShape() }

        forceFixture = Fixture(
            body,
            FixtureType.FORCE,
            GameRectangle().setSize(ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        )
        forceFixture.offsetFromBodyAttachment.y = -ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        forceFixture.setVelocityAlteration { fixture, delta ->
            val entity = fixture.getEntity()
            if (entity is AbstractEnemy || (entity is Megaman && entity.damaged)) return@setVelocityAlteration VelocityAlteration.addNone()
            if (entity is AbstractProjectile) entity.owner = null

            val x = PULL_FORCE_X * ConstVals.PPM * facing.value
            val y = PULL_FORCE_Y * ConstVals.PPM * delta

            return@setVelocityAlteration VelocityAlteration.add(x, y)
        }
        body.addFixture(forceFixture)
        forceFixture.getShape().color = Color.YELLOW
        debugShapes.add { forceFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.95f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            forceFlashTimer.update(it)
            if (forceFlashTimer.isFinished()) {
                flash = !flash
                forceFlashTimer.reset()
            }

            val slow = megaman().body.overlaps(forceFixture.getShape() as Rectangle)

            if (!slow && megaman().body.getY() < body.getY() && !facingAndMMDirMatch()) facing =
                if (megaman().body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT

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
        (game.megaman.body.getX() > body.getX() && facing == Facing.RIGHT) || (game.megaman.body.getX() < body.getX() && facing == Facing.LEFT)
}
