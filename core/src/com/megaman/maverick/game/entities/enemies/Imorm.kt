package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class Imorm(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable, IDirectionRotatable {

    companion object {
        const val TAG = "Imorm"
        private const val SLITHER_DISTANCE = 0.3f * ConstVals.PPM
        private const val SLITHER_DURATION = 0.6f
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        // Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        /*
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg(5)
         */
    )

    override lateinit var facing: Facing
    override lateinit var directionRotation: Direction

    private val slitherTimer = Timer(SLITHER_DURATION)
    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Imorm")
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(position)
        slitherTimer.reset()
        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
        directionRotation = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class)
                .uppercase()
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            slitherTimer.update(it)
            if (slitherTimer.isFinished()) {
                // TODO: accomodate body rotation
                val x = SLITHER_DISTANCE * facing.value
                body.x += x
                slitherTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1f * ConstVals.PPM, 0.375f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.GRAY
        debugShapes.add { bodyFixture.shape }

        val damagerFixture = Fixture(GameRectangle().setHeight(0.5f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        val damageableFixture = Fixture(GameRectangle().setHeight(0.5f * ConstVals.PPM), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        debugShapes.add { damageableFixture.shape }

        val shieldFixture = Fixture(GameRectangle().setHeight(0.5f * ConstVals.PPM), FixtureType.SHIELD)
        body.addFixture(shieldFixture)
        shieldFixture.shape.color = Color.CYAN
        debugShapes.add { shieldFixture.shape }

        val leftFixture = Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM), FixtureType.SIDE)
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyCenter.x = -SLITHER_DISTANCE * 1.5f
        body.addFixture(leftFixture)
        leftFixture.shape.color = Color.YELLOW
        debugShapes.add { leftFixture.shape }

        val rightFixture =
            Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM), FixtureType.SIDE)
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyCenter.x = SLITHER_DISTANCE * 1.5f
        body.addFixture(rightFixture)
        rightFixture.shape.color = Color.YELLOW
        debugShapes.add { rightFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            val width = if (slitherTimer.time <= 0.15f) 0.375f * ConstVals.PPM
            else if (slitherTimer.time <= 0.3f || slitherTimer.time >= 0.45f) 0.5f * ConstVals.PPM
            else 0.95f * ConstVals.PPM
            (damagerFixture.shape as GameRectangle).width = width
            (damageableFixture.shape as GameRectangle).width = width
            (shieldFixture.shape as GameRectangle).width = 0.9f * ConstVals.PPM

            if (isFacing(Facing.LEFT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT))
                facing = Facing.RIGHT
            else if (isFacing(Facing.RIGHT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                facing = Facing.LEFT
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.15f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}