package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.roundAngleToStep
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*
import kotlin.math.roundToInt
import kotlin.reflect.KClass

class Needle(services: MegaServices) : AbstractProjectile(services), IHealthEntity, IDamageableEntity {

    companion object {
        const val TAG = "Needle"
        private const val DAMAGE_DURATION = 0.1f
        private const val OWNER_COLLISION_DELAY = 0.5f
        private val damageNegotiations = objectMapOf<KClass<out IDamagerEntity>, Int>(
            Bullet::class pairTo 15,
            ProtoBuster::class pairTo ConstVals.MAX_HEALTH,
            PlayerSabreSlash::class pairTo ConstVals.MAX_HEALTH,
            Fireball::class pairTo ConstVals.MAX_HEALTH,
            ChargedShot::class pairTo ConstVals.MAX_HEALTH,
            ChargedShotExplosion::class pairTo ConstVals.MAX_HEALTH,
            SlashWave::class pairTo ConstVals.MAX_HEALTH,
            MoonScythe::class pairTo ConstVals.MAX_HEALTH,
            ReactorManProjectile::class pairTo ConstVals.MAX_HEALTH
        )
        private const val SPRITE_ANGLE_OFFSET = 270f
        private val regions = ObjectMap<NeedleType, Array<TextureRegion>>()
    }

    enum class NeedleType { DEFAULT, ICE }

    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    lateinit var type: NeedleType
    lateinit var damagerFixture: Fixture

    private val damageTimer = Timer(DAMAGE_DURATION)
    private var blink = false

    private val ownerCollisionDelay = Timer(OWNER_COLLISION_DELAY)
    private val drawDirection = NeedleDrawDirection()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            NeedleType.entries.forEach { needleType ->
                val name = needleType.name.lowercase()
                val frames = Array<TextureRegion>()
                frames.add(atlas.findRegion("${TAG}/${name}"))
                for (frame in 1 until NeedleDrawDirection.DRAWN_FRAMES) {
                    frames.add(atlas.findRegion("${TAG}/${name}_step${frame}"))
                }
                regions.put(needleType, frames)
            }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(definePointsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        setHealth(ConstVals.MAX_HEALTH)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val bodyPosition =
            spawnProps.getOrDefault("${ConstKeys.BODY}_${ConstKeys.POSITION}", Position.CENTER, Position::class)
        body.positionOnPoint(spawn, bodyPosition)

        body.physics.gravity.set(
            when (val gravityProp = spawnProps.get(ConstKeys.GRAVITY)) {
                is Vector2 -> gravityProp
                is Float -> GameObjectPools.fetch(Vector2::class).set(0f, gravityProp)
                else -> Vector2.Zero
            }
        )

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(impulse)

        val damagerActive = spawnProps.getOrDefault("${ConstKeys.DAMAGER}_${ConstKeys.ACTIVE}", true, Boolean::class)
        damagerFixture.setActive(damagerActive)

        damageTimer.setToEnd()
        blink = false

        type = if (spawnProps.containsKey(ConstKeys.TYPE)) {
            val rawType = spawnProps.get(ConstKeys.TYPE)
            rawType as? NeedleType ?: if (rawType is String) NeedleType.valueOf(rawType.uppercase())
            else throw IllegalArgumentException("Illegal value for type: $rawType")
        } else NeedleType.DEFAULT

        ownerCollisionDelay.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        if (isHealthDepleted()) explode()
    }

    override fun onDamageInflictedTo(damageable: IDamageableEntity) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = blockFixture.getEntity()
        if (entity == owner && !ownerCollisionDelay.isFinished()) return

        explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = shieldFixture.getEntity()
        if (entity == owner && !ownerCollisionDelay.isFinished()) return

        explodeAndDie()
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        !invincible && damageNegotiations.containsKey(damager::class)

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val damagerKey = damager::class
        if (!damageNegotiations.containsKey(damagerKey)) return false

        damageTimer.reset()

        val damage = damageNegotiations[damagerKey]
        translateHealth(-damage)

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)

        return true
    }

    fun explode() {
        val flash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.MUZZLE_FLASH)!!
        flash.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun explodeAndDie(vararg params: Any?) {
        explode()
        destroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        damageTimer.update(delta)
        ownerCollisionDelay.update(delta)
    })

    private fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(ConstKeys.HEALTH, ConstVals.MAX_HEALTH)
        pointsComponent.putListener(ConstKeys.HEALTH) { if (it.current <= 0) destroy() }
        return pointsComponent
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        debugShapes.add { projectileFixture }

        damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            drawDirection.set(body.physics.velocity.angleDeg() + SPRITE_ANGLE_OFFSET)
            sprite.setRegion(regions[type][drawDirection.frame])
            sprite.setFlip(drawDirection.flipX, false)
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = drawDirection.rotation
            sprite.hidden = !damageTimer.isFinished()
        }
        return spritesComponent
    }
}

internal class NeedleDrawDirection {

    companion object {
        const val DRAW_ANGLE_STEP = 11.25f
        const val DRAW_STEPS_PER_QUADRANT = 8
        const val DRAWN_FRAMES = DRAW_STEPS_PER_QUADRANT / 2 + 1
    }

    var frame = 0
        private set
    var flipX = false
        private set
    var rotation = 0f
        private set

    fun set(spriteAngle: Float) {
        val angle = spriteAngle.roundAngleToStep(DRAW_ANGLE_STEP)
        val step = (angle / DRAW_ANGLE_STEP).roundToInt()
        val quadrant = step / DRAW_STEPS_PER_QUADRANT
        val stepInQuadrant = step % DRAW_STEPS_PER_QUADRANT
        flipX = stepInQuadrant >= DRAWN_FRAMES
        frame = if (flipX) DRAW_STEPS_PER_QUADRANT - stepInQuadrant else stepInQuadrant
        rotation = quadrant * 90f + if (flipX) 90f else 0f
    }
}
