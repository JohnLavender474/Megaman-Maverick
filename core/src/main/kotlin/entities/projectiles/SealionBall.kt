package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.enemies.Sealion
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.utils.MegaUtilMethods.pooledProps
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class SealionBall(game: MegamanMaverickGame) : AbstractHealthEntity(game), IProjectileEntity, ISpritesEntity,
    IDamageable {

    companion object {
        const val TAG = "SealionBall"
        private const val VELOCITY_Y = 14f
        private const val GRAVITY = -0.1f
        private const val OUT_OF_BOUNDS_CULL_TIME = 1f
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 20
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 10
        }
    )
    override var owner: GameEntity? = null

    private var ballInHand = true

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponents(defineProjectileComponents(OUT_OF_BOUNDS_CULL_TIME))
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        owner = spawnProps.get(ConstKeys.OWNER, Sealion::class)!!
        catchBall()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = (owner as Sealion).onBallDamagedInflicted()

    fun throwBall() {
        ballInHand = false
        body.physics.velocity.y = VELOCITY_Y * ConstVals.PPM
        body.physics.gravityOn = true
        GameLogger.debug(TAG, "Throw ball")
    }

    fun catchBall() {
        ballInHand = true
        body.physics.velocity.setZero()
        body.physics.gravityOn = false
        GameLogger.debug(TAG, "Catch ball")
    }

    override fun onDestroy() {
        super.onDestroy()

        if (overlapsGameCamera()) playSoundNow(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        val disintegration = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)!!
        disintegration.spawn(pooledProps(ConstKeys.POSITION pairTo body.getCenter()))

        if (owner != null) {
            (owner as Sealion).onBallDestroyed()
            owner = null
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.05f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.325f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        debugShapes.add { projectileFixture}

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.325f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.325f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!)
        sprite.setSize(2.25f * ConstVals.PPM, 1.4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink || ballInHand
        }
        return spritesComponent
    }
}
