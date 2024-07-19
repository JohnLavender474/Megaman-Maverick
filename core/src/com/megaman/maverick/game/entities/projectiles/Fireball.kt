package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.getOverlapPushDirection
import com.engine.common.mask
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameCircle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.utils.playSoundNow
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

class Fireball(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "Fireball"
        const val BURST_ON_DAMAGE_INFLICTED = "burst_on_damage_inflicted"
        const val BURST_ON_HIT_BODY = "burst_on_hit_body"
        const val BURST_ON_HIT_BLOCK = "burst_on_hit_block"
        private var fireballAtlas: TextureAtlas? = null
        private var flameAtlas: TextureAtlas? = null
        private const val ROTATION = 720f
        private const val BURST_CULL_DUR = 1f
    }

    override var owner: IGameEntity? = null

    private lateinit var burstCullTimer: Timer
    private lateinit var burstDirection: Direction

    private var burstOnDamageInflicted = false
    private var burstOnHitBlock = true
    private var burstOnHitBody = false
    private var burst = false

    override fun init() {
        if (fireballAtlas == null)
            fireballAtlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
        if (flameAtlas == null) flameAtlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)

        val spawn = spawnProps.getOrDefault(ConstKeys.POSITION, Vector2(), Vector2::class)
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.velocity = trajectory

        val cullTime = spawnProps.getOrDefault(ConstKeys.CULL_TIME, BURST_CULL_DUR, Float::class)
        burstCullTimer = Timer(cullTime)

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2(), Vector2::class)
        body.physics.gravity = gravity

        burstOnDamageInflicted = spawnProps.getOrDefault(BURST_ON_DAMAGE_INFLICTED, false, Boolean::class)
        burstOnHitBody = spawnProps.getOrDefault(BURST_ON_HIT_BODY, false, Boolean::class)
        burstOnHitBlock = spawnProps.getOrDefault(BURST_ON_HIT_BLOCK, true, Boolean::class)

        burst = false
        burstDirection = Direction.UP


    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        super.onDamageInflictedTo(damageable)
        if (burstOnDamageInflicted) explodeAndDie()
    }

    override fun hitBody(bodyFixture: IFixture) {
        if (burstOnHitBody && mask(owner, bodyFixture.getEntity(), { it is Megaman }, { it is AbstractEnemy })) {
            burstDirection = getOverlapPushDirection(body, bodyFixture.getShape()) ?: Direction.UP
            explodeAndDie()
        }
    }

    override fun hitBlock(blockFixture: IFixture) {
        if (burstOnHitBlock) {
            burstDirection = getOverlapPushDirection(body, blockFixture.getShape()) ?: Direction.UP
            explodeAndDie()
        }
    }

    override fun hitShield(shieldFixture: IFixture) {
        body.physics.velocity.x *= -1f
        burstDirection = getOverlapPushDirection(body, shieldFixture.getShape()) ?: Direction.UP
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun hitWater(waterFixture: IFixture) {
        val smokePuff = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SMOKE_PUFF)!!
        val spawn = Vector2(body.getCenter().x, waterFixture.getShape().getMaxY())
        game.engine.spawn(smokePuff, props(ConstKeys.POSITION to spawn, ConstKeys.OWNER to owner))
        playSoundNow(SoundAsset.WHOOSH_SOUND, false)
        kill()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.6f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.rotatedBounds }

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.rawShape.color = Color.GREEN
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.25f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { delta, _sprite ->
            val size = if (burst) 0.85f else 1.65f
            _sprite.setSize(size * ConstVals.PPM)
            val position = if (burst) Position.BOTTOM_CENTER else Position.CENTER
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
            _sprite.setOriginCenter()
            _sprite.rotation = if (burst) burstDirection.rotation else _sprite.rotation + ROTATION * delta
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (burst) "burst" else "fireball" }
        val animations =
            objectMapOf<String, IAnimation>(
                "burst" to Animation(flameAtlas!!.findRegion("Flame"), 1, 4, 0.1f, true),
                "fireball" to Animation(fireballAtlas!!.findRegion("Fire/Fireball"))
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun defineUpdatablesComponent(): UpdatablesComponent {
        val updatablesComponent = UpdatablesComponent(this)
        updatablesComponent.add {
            if (burst) {
                body.physics.velocity.setZero()
                burstCullTimer.update(it)
            }
            if (burstCullTimer.isFinished()) kill()
        }
        return updatablesComponent
    }

    override fun explodeAndDie(vararg params: Any?) {
        burst = true
        requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }
}
