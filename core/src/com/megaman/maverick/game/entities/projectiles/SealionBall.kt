package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.enemies.Sealion
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SealionBall(game: MegamanMaverickGame) : AbstractHealthEntity(game), IProjectileEntity, ISpritesEntity,
    IDamageable {

    companion object {
        const val TAG = "SealionBall"
        private const val VELOCITY_Y = 15f
        private const val GRAVITY = -0.15f
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 20
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 10
        }
    )
    override var owner: IGameEntity? = null

    private var ballInHand = true

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        owner = spawnProps.get(ConstKeys.OWNER, Sealion::class)!!
        catchBall()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo(damageable)
        (owner as Sealion).onBallDamagedInflicted()
    }

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
        if (owner != null) {
            (owner as Sealion).onBallDestroyed()
            owner = null
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.425f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.rawShape.color = Color.RED
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.425f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.425f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!)
        sprite.setSize(2f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = damageBlink || ballInHand
        }
        return spritesComponent
    }
}