package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.points.PointsComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class Needle(game: MegamanMaverickGame) : AbstractProjectile(game), IHealthEntity, IDamageable {

    companion object {
        const val TAG = "Needle"
        private const val DAMAGE_DURATION = 0.1f
        private val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>(
            Bullet::class pairTo 15,
            Fireball::class pairTo ConstVals.MAX_HEALTH,
            ChargedShot::class pairTo ConstVals.MAX_HEALTH,
            ChargedShotExplosion::class pairTo ConstVals.MAX_HEALTH,
            SlashWave::class pairTo ConstVals.MAX_HEALTH,
            MoonScythe::class pairTo ConstVals.MAX_HEALTH
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class NeedleType { DEFAULT, ICE }

    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    lateinit var type: NeedleType
    lateinit var damagerFixture: Fixture

    private val damageTimer = Timer(DAMAGE_DURATION)
    private var blink = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            gdxArrayOf("default", "ice").forEach { regions.put(it, atlas.findRegion("${TAG}/${it}")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(definePointsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        setHealth(ConstVals.MAX_HEALTH)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val bodyPosition =
            spawnProps.getOrDefault("${ConstKeys.BODY}_${ConstKeys.POSITION}", Position.CENTER, Position::class)
        body.positionOnPoint(spawn, bodyPosition)

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, 0f, Float::class)
        body.physics.gravity.set(0f, gravity)

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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHealthDepleted()) explode()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        explodeAndDie()

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

    fun explode() {
        val flash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.MUZZLE_FLASH)!!
        flash.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun explodeAndDie(vararg params: Any?) {
        explode()
        destroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta -> damageTimer.update(delta) })

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
            val region = regions[type.name.lowercase()]
            sprite.setRegion(region)
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg() + 270f
            sprite.hidden = !damageTimer.isFinished()
        }
        return spritesComponent
    }
}
