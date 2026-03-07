package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.StandardDamageNegotiator
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class HomingMissile(game: MegamanMaverickGame) : AbstractProjectile(game), IHealthEntity, IAnimatedEntity, IDamageable {

    companion object {
        const val TAG = "HomingMissile"

        private const val SPEED = 6f
        private const val SPEED_HARD = 8f

        private const val DAMAGE_DURATION = 0.1f

        private const val RECALC_DELAY = 0.25f
        private const val TIME_BEFORE_FIRST_RECALC = 1f
        private const val TIME_BEFORE_FIRST_RECALC_HARD = 0.5f

        private const val TTL = 3f
        private const val FLASH_START = 2f

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = orderedMapOf(
            "straight" pairTo AnimationDef(2, 1, 0.1f, true),
            "angled" pairTo AnimationDef(2, 1, 0.1f, true),
            "straight_flash" pairTo AnimationDef(2, 2, 0.05f, true),
            "angled_flash" pairTo AnimationDef(2, 2, 0.05f, true),
        )
    }

    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    private val recalcDelay = Timer()
    private val recalcTimer = Timer(RECALC_DELAY)

    private val damageTimer = Timer(DAMAGE_DURATION)

    // Game-space angle: 0=up, 90=right, 180=down, 270=left (clockwise)
    private var currentAngle = 0

    private val ttl = Timer(TTL)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(definePointsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        setHealth(ConstVals.MAX_HEALTH)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.setCenter(spawn)

        currentAngle = spawnProps.getOrDefault(ConstKeys.ANGLE, 0, Int::class)
        setVelocityFromAngle(currentAngle)

        recalcDelay.resetDuration(
            if (game.state.hardMode) TIME_BEFORE_FIRST_RECALC_HARD
            else TIME_BEFORE_FIRST_RECALC
        )
        recalcTimer.reset()

        damageTimer.setToEnd()

        ttl.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        if (isHealthDepleted()) explode()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        GameLogger.debug(TAG, "onDamageInflictedTo(): damageable=$damageable")
        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        GameLogger.debug(TAG, "hitBlock(): blockFixture=$blockFixture")
        explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")
        destroy()
        explode()
    }

    private fun explode() {
        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo body.getCenter()
            )
        )
        requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun canBeDamagedBy(damager: IDamager) =
        !invincible && damager != owner && damager !is HomingMissile && damager !is Explosion

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")

        val damage = StandardDamageNegotiator.get(Size.SMALL, damager)
        translateHealth(-damage)

        damageTimer.reset()

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)

        return true
    }

    private fun setVelocityFromAngle(gameAngle: Int) {
        // Convert game angle (0=up, clockwise) to math angle (0=right, CCW)
        val mathAngle = 90 - gameAngle
        val velocity = GameObjectPools.fetch(Vector2::class)
            .set(0f, (if (game.state.hardMode) SPEED_HARD else SPEED) * ConstVals.PPM)
            .setAngleDeg(mathAngle.toFloat())
        body.physics.velocity.set(velocity)
    }

    private fun recalcDirection() {
        val toMegaman = megaman.body.getCenter().sub(body.getCenter())
        val mathAngle = toMegaman.angleDeg()
        val gameAngle = 90f - mathAngle
        val snapped = MathUtils.round(gameAngle / 45f) * 45
        currentAngle = ((snapped % 360) + 360) % 360
        setVelocityFromAngle(currentAngle)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        ttl.update(delta)
        if (ttl.isFinished()) {
            explodeAndDie()
            return@UpdatablesComponent
        }

        damageTimer.update(delta)

        if (!recalcDelay.isFinished()) {
            recalcDelay.update(delta)
            return@UpdatablesComponent
        }

        recalcTimer.update(delta)
        if (recalcTimer.isFinished()) {
            recalcTimer.reset()
            recalcDirection()
        }
    })

    private fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(ConstKeys.HEALTH, ConstVals.MAX_HEALTH)
        pointsComponent.putListener(ConstKeys.HEALTH) { if (it.current <= 0) destroy() }
        return pointsComponent
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(TAG pairTo sprite)
        component.putPreProcess(TAG) { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            val isDiagonal = currentAngle % 90 != 0
            // Straight sprite points up (base math angle 90°); rotation = -currentAngle
            // Angled sprite points up-right (base math angle 45°); rotation = 45 - currentAngle
            sprite.rotation = (if (isDiagonal) 45 - currentAngle else -currentAngle).toFloat()
            sprite.hidden = !damageTimer.isFinished()
        }
        return component
    }

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    var key = if (currentAngle % 90 != 0) "angled" else "straight"
                    if (ttl.time >= FLASH_START) key += "_flash"
                    return@keySupplier key
                }
                .applyToAnimations { AnimationUtils.loadAnimationDefs(animDefs, it, regions) }
                .build()
        )
        .build()
}
