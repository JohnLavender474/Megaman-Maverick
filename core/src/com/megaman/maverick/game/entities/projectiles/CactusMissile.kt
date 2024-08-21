package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.points.PointsComponent
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class CactusMissile(game: MegamanMaverickGame): AbstractProjectile(game), IHealthEntity, IAnimatedEntity, IDamageable {

    companion object {
        const val TAG = "CactusMissile"
        private const val SPEED = 2.5f
        private const val UP_DUR = 0.5f
        private const val RECALC_DELAY = 0.5f
        private const val DAMAGE_DURATION = 0.25f
        private var region: TextureRegion? = null
    }

    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    private val damageNegotiations =
        objectMapOf<KClass<out IDamager>, Int>(
            Bullet::class to 10,
            Fireball::class to ConstVals.MAX_HEALTH,
            ChargedShot::class to ConstVals.MAX_HEALTH,
            ChargedShotExplosion::class to ConstVals.MAX_HEALTH
        )
    private val damageTimer = Timer(DAMAGE_DURATION)
    private val upTimer = Timer(UP_DUR)
    private val recalcTimer = Timer(RECALC_DELAY)

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super<AbstractProjectile>.init()
        addComponent(defineUpdatablesComponent())
        addComponent(definePointsComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        setHealth(ConstVals.MAX_HEALTH)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        damageTimer.setToEnd()
        upTimer.reset()
        recalcTimer.reset()
    }

    override fun onDestroy() {
        super<AbstractProjectile>.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        kill()
        explode()
    }

    private fun explode() {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        game.engine.spawn(
            explosion,
            props(
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.SOUND to SoundAsset.EXPLOSION_2_SOUND,
                ConstKeys.OWNER to this
            )
        )
    }

    override fun canBeDamagedBy(damager: IDamager) =
        !invincible && damageNegotiations.containsKey(damager::class)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damagerKey = damager::class
        if (!damageNegotiations.containsKey(damagerKey)) return false

        damageTimer.reset()

        val damage = damageNegotiations[damagerKey]
        translateHealth(-damage)
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!upTimer.isFinished()) {
            upTimer.update(delta)
            body.physics.velocity = Vector2(0f, SPEED * ConstVals.PPM)
            return@UpdatablesComponent
        }

        recalcTimer.update(delta)
        if (recalcTimer.isFinished()) {
            recalcTimer.reset()
            val angle = getMegaman().body.getCenter().sub(body.getCenter()).angleDeg()
            val roundedAngle45 = MathUtils.round(angle / 45f) * 45f
            body.physics.velocity = Vector2(0f, SPEED * ConstVals.PPM).setAngleDeg(roundedAngle45)
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.625f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.625f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.625f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(ConstKeys.HEALTH, ConstVals.MAX_HEALTH)
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= 0) kill(props(CAUSE_OF_DEATH_MESSAGE to "Health depleted"))
        }
        return pointsComponent
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = body.physics.velocity.angleDeg() - 90f
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}