package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
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
import com.engine.entities.IGameEntity
import com.engine.points.PointsComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Petal(game: MegamanMaverickGame) : AbstractProjectile(game), IHealthEntity, IDamager, IDamageable {

    companion object {
        const val TAG = "Petal"
        private var region: TextureRegion? = null
        private const val SPEED = 10f
        private const val DAMAGE_DURATION = 0.25f
    }

    override val invincible = false

    private val damageNegotiations =
        objectMapOf<KClass<out IDamager>, Int>(
            Bullet::class to 10,
            Fireball::class to ConstVals.MAX_HEALTH,
            ChargedShot::class to ConstVals.MAX_HEALTH,
            ChargedShotExplosion::class to ConstVals.MAX_HEALTH
        )
    private val damageTimer = Timer(DAMAGE_DURATION)

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Petal")
        super<AbstractProjectile>.init()
        addComponent(AudioComponent(this))
        addComponent(defineAnimationsComponent())
        addComponent(definePointsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        setHealth(ConstVals.MAX_HEALTH)
        GameLogger.debug(TAG, "Health: ${getCurrentHealth()}. Spawn props: $spawnProps.")

        val center = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(center)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory.scl(ConstVals.PPM.toFloat() * SPEED))

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
    }

    override fun onDestroy() {
        super<AbstractProjectile>.onDestroy()
        GameLogger.debug(TAG, "Petal destroyed")
    }

    override fun canBeDamagedBy(damager: IDamager) =
        !invincible && damageNegotiations.containsKey(damager::class)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damagerKey = damager::class
        if (!damageNegotiations.containsKey(damagerKey)) return false

        damageTimer.reset()

        val damage = damageNegotiations[damagerKey]
        getHealthPoints().translate(-damage)
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return true
    }

    private fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent(this)
        pointsComponent.putPoints(ConstKeys.HEALTH, ConstVals.MAX_HEALTH)
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= 0) kill(props(CAUSE_OF_DEATH_MESSAGE to "Health depleted"))
        }
        return pointsComponent
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.9f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(0.4f * ConstVals.PPM))
        body.addFixture(bodyFixture)

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.4f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.4f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.4f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
