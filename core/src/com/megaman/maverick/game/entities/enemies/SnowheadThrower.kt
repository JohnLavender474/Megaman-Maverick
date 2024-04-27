package com.megaman.maverick.game.entities.enemies

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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SnowheadThrower(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SnowheadThrower"
        private const val STAND_DUR = 1f
        private const val THROW_DUR = 0.6f
        private const val THROW_DELAY = 0.1f
        private const val SNOW_HEAD_X_VEL = 6f
        private const val SNOW_HEAD_Y_VEL = 6f
        private var standRegion: TextureRegion? = null
        private var throwRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 20
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }
    )
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val throwTimer = Timer(THROW_DUR)
    private val throwDelay = Timer(THROW_DELAY)

    private var throwing = false

    override fun init() {
        if (standRegion == null || throwRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            standRegion = atlas.findRegion("SnowheadThrower/Stand")
            throwRegion = atlas.findRegion("SnowheadThrower/Throw")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        throwTimer.reset()
        throwDelay.reset()
        standTimer.reset()
        throwing = false
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    override fun explode(explosionProps: Properties?) {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SNOWBALL_EXPLOSION)!!
        game.engine.spawn(explosion, props(ConstKeys.POSITION to body.getCenter()))
    }

    private fun throwHead() {
        val spawn = body.getTopCenterPoint().sub(0f, 0.25f * ConstVals.PPM)
        val trajectory = Vector2(SNOW_HEAD_X_VEL * facing.value, SNOW_HEAD_Y_VEL).scl(ConstVals.PPM.toFloat())
        val snowhead = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOW_HEAD)!!
        game.engine.spawn(
            snowhead, props(
                ConstKeys.OWNER to this, ConstKeys.POSITION to spawn, ConstKeys.TRAJECTORY to trajectory
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (throwing) {
                throwDelay.update(delta)
                if (throwDelay.isJustFinished()) throwHead()

                throwTimer.update(delta)
                if (throwTimer.isFinished()) {
                    throwTimer.reset()
                    throwDelay.reset()
                    throwing = false
                }
            } else {
                facing = if (megaman.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

                standTimer.update(delta)
                if (standTimer.isFinished()) {
                    standTimer.reset()
                    throwing = true
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.rotatedBounds }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (throwing) "throwing" else "standing" }
        val animations = objectMapOf<String, IAnimation>(
            "standing" to Animation(standRegion!!, 1, 3, 0.1f, false),
            "throwing" to Animation(throwRegion!!, 1, 5, 0.075f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}

