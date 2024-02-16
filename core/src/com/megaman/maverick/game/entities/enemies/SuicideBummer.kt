package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
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
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class SuicideBummer(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable, IAnimatedEntity {

    companion object {
        const val TAG = "SuicideBummer"
        private const val GRAVITY = -0.375f
        private const val GROUND_GRAVITY = -0.0015f
        private const val GROUND_X_VEL = 4f
        private const val AIR_X_VEL = 2.5f
        private const val JUMP_IMPULSE = 12f
        private var runRegion: TextureRegion? = null
        private var jumpRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class to dmgNeg(5)
    )

    private var wasSideOnGround = false

    override fun init() {
        if (runRegion == null || jumpRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            runRegion = atlas.findRegion("SuicideBummer/Run")
            jumpRegion = atlas.findRegion("SuicideBummer/Jump")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
        wasSideOnGround = false
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        explode(
            props(
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.SOUND to true
            )
        )
        kill()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            val sense =
                if (isFacing(Facing.LEFT)) BodySense.SIDE_TOUCHING_BLOCK_LEFT else BodySense.SIDE_TOUCHING_BLOCK_RIGHT
            val isSideOnGround = body.isSensing(sense)
            if (
                (wasSideOnGround && !isSideOnGround) ||
                (isSideOnGround &&
                        megaman.body.getMaxX() >= body.x &&
                        megaman.body.x <= body.getMaxX() &&
                        megaman.body.getY() > body.getY())
            ) jump()
            wasSideOnGround = isSideOnGround
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        // body fixture
        val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.GRAY
        debugShapes.add { bodyFixture.shape }

        // damager fixture
        val damagerFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        // damageable fixture
        val damageableFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)

        // left fixture
        val leftFixture = Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM), FixtureType.SIDE)
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyCenter = Vector2(-0.6255f * ConstVals.PPM, -0.625f * ConstVals.PPM)
        body.addFixture(leftFixture)
        leftFixture.shape.color = Color.YELLOW
        debugShapes.add { leftFixture.shape }

        // right fixture
        val rightFixture = Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM), FixtureType.SIDE)
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyCenter = Vector2(0.625f * ConstVals.PPM, -0.625f * ConstVals.PPM)
        body.addFixture(rightFixture)
        rightFixture.shape.color = Color.YELLOW
        debugShapes.add { rightFixture.shape }

        // feet fixture
        val feetFixture = Fixture(GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM), FixtureType.FEET)
        feetFixture.offsetFromBodyCenter.y = -0.5f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.shape.color = Color.GREEN
        debugShapes.add { feetFixture.shape }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.x =
                facing.value * ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_X_VEL else AIR_X_VEL
            body.physics.gravity.y = ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY
            else GRAVITY
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }


    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (body.isSensing(BodySense.FEET_ON_GROUND)) "run" else "jump" }
        val animations = objectMapOf<String, IAnimation>(
            "run" to Animation(runRegion!!, 1, 4, 0.125f, true),
            "jump" to Animation(jumpRegion!!, 1, 2, 0.125f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun jump() {
        body.physics.velocity.y = JUMP_IMPULSE * ConstVals.PPM
    }
}