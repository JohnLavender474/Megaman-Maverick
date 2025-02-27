package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.points.PointsComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class CactusMissile(game: MegamanMaverickGame) : AbstractProjectile(game), IHealthEntity, IAnimatedEntity, IDamageable {

    companion object {
        const val TAG = "CactusMissile"

        private const val SPEED = 4f

        private const val UP_DUR = 0.25f
        private const val RECALC_DELAY = 0.5f
        private const val DAMAGE_DURATION = 0.1f
        private const val LIFE_DUR = 3f
        private const val BLINK_TIME = 2.5f

        private val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>(
            Bullet::class pairTo 10,
            Fireball::class pairTo ConstVals.MAX_HEALTH,
            ChargedShot::class pairTo ConstVals.MAX_HEALTH,
            ChargedShotExplosion::class pairTo ConstVals.MAX_HEALTH
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    private val upTimer = Timer(UP_DUR)
    private val recalcTimer = Timer(RECALC_DELAY)
    private val damageTimer = Timer(DAMAGE_DURATION)
    private val lifeTimer = Timer(LIFE_DUR, TimeMarkedRunnable(BLINK_TIME) { blink = true })

    private var blink = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            gdxArrayOf("fly", "blink").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(definePointsComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        setHealth(ConstVals.MAX_HEALTH)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        upTimer.reset()
        lifeTimer.reset()
        recalcTimer.reset()
        damageTimer.setToEnd()

        blink = false
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isHealthDepleted()) explode()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        explode()
        destroy()
    }

    private fun explode() {
        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
                ConstKeys.OWNER pairTo this
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

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)

        return true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        lifeTimer.update(delta)
        if (lifeTimer.isFinished()) {
            explodeAndDie()
            return@UpdatablesComponent
        }

        damageTimer.update(delta)

        if (!upTimer.isFinished()) {
            upTimer.update(delta)

            val velocity = GameObjectPools.fetch(Vector2::class)
            velocity.y = SPEED * ConstVals.PPM
            body.physics.velocity.set(velocity)

            return@UpdatablesComponent
        }

        recalcTimer.update(delta)
        if (recalcTimer.isFinished()) {
            recalcTimer.reset()
            val angle = megaman.body.getCenter().sub(body.getCenter()).angleDeg()
            val roundedAngle45 = MathUtils.round(angle / 45f) * 45f

            val velocity = GameObjectPools.fetch(Vector2::class)
                .set(0f, SPEED * ConstVals.PPM)
                .setAngleDeg(roundedAngle45)
            body.physics.velocity.set(velocity)
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.75f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.75f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.75f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(ConstKeys.HEALTH, ConstVals.MAX_HEALTH)
        pointsComponent.putListener(ConstKeys.HEALTH) { if (it.current <= 0) destroy() }
        return pointsComponent
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg() - 90f

            sprite.setCenter(body.getCenter())

            sprite.hidden = !damageTimer.isFinished()
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (blink) "blink" else "fly" }
        val animations = objectMapOf<String, IAnimation>(
            "blink" pairTo Animation(regions["blink"], 2, 2, 0.05f, true),
            "fly" pairTo Animation(regions["fly"], 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
