package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class SmallIceCube(game: MegamanMaverickGame) : AbstractProjectile(game), IFreezerEntity {

    companion object {
        const val TAG = "SmallIceCube"

        const val BODY_SIZE = 0.75f

        private const val DEFAULT_GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val CLAMP = 10f
        private const val CULL_TIME = 2f
        private const val DEFAULT_MAX_HIT_TIMES = 1

        private var region1: TextureRegion? = null
        private var region2: TextureRegion? = null

        private val INSTANT_DEATH_ENTITIES = objectSetOf(
            Bullet::class,
            Megaman::class,
            ChargedShot::class,
            SlashWave::class
        )
        private val SMOKE_PUFF_ENTITIES = objectSetOf(
            MagmaWave::class,
            MagmaMeteor::class,
            MagmaFlame::class,
            Fireball::class,
            FlameThrower::class,
            FireWall::class,
            SpitFireball::class
        )
    }

    private var hitTimes = 0
    private var destroyOnHitBlock = false
    private var maxHitTimes = DEFAULT_MAX_HIT_TIMES
    private val ignoreHitsFromIds = ObjectSet<Int>()

    private var gravity = DEFAULT_GRAVITY

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region1 == null || region2 == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            region1 = atlas.findRegion("${TAG}/1")
            region2 = atlas.findRegion("${TAG}/2")
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineBodyComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(trajectory)

        gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, DEFAULT_GRAVITY, Float::class)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, true, Boolean::class)
        body.physics.gravityOn = gravityOn

        val applyFrictionX = spawnProps.getOrDefault(ConstKeys.FRICTION_X, true, Boolean::class)
        body.physics.applyFrictionX = applyFrictionX

        val applyFrictionY = spawnProps.getOrDefault(ConstKeys.FRICTION_Y, true, Boolean::class)
        body.physics.applyFrictionY = applyFrictionY

        val section = spawnProps.getOrDefault(ConstKeys.SECTION, DrawingSection.PLAYGROUND, DrawingSection::class)
        defaultSprite.priority.section = section

        val priority = spawnProps.getOrDefault(ConstKeys.PRIORITY, 5, Int::class)
        defaultSprite.priority.value = priority

        val doClamp = spawnProps.getOrDefault(ConstKeys.CLAMP, true, Boolean::class)
        val clamp = GameObjectPools.fetch(Vector2::class)
        if (doClamp) clamp.set(CLAMP, CLAMP).scl(ConstVals.PPM.toFloat()) else clamp.set(
            Float.MAX_VALUE,
            Float.MAX_VALUE
        )
        body.physics.velocityClamp.set(clamp)

        destroyOnHitBlock = spawnProps.getOrDefault(ConstKeys.HIT_BY_BLOCK, false, Boolean::class)
        maxHitTimes = spawnProps.getOrDefault(ConstKeys.MAX, DEFAULT_MAX_HIT_TIMES, Int::class)
        hitTimes = 0

        val blockIds = spawnProps.getOrDefault("${ConstKeys.IGNORE}_${ConstKeys.HIT}", Array<Int>()) as Iterable<Int>
        blockIds.forEach { ignoreHitsFromIds.add(it) }
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = shatterAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        playSoundNow(SoundAsset.DINK_SOUND, false)
        shatterAndDie()
    }

    override fun shatterAndDie() {
        GameLogger.debug(TAG, "shatterAndDie()")
        IceShard.spawn5(body.getCenter())
        if (owner == megaman) {
            val explosion = MegaEntityFactory.fetch(IceBombExplosion::class)!!
            explosion.spawn(props(ConstKeys.OWNER pairTo megaman, ConstKeys.POSITION pairTo body.getCenter()))
        }
        destroy()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        ignoreHitsFromIds.clear()
    }

    private fun smokePuff() {
        GameLogger.debug(TAG, "smokePuff()")

        destroy()

        val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
        puff.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))

        playSoundNow(SoundAsset.WHOOSH_SOUND, false)
    }

    private fun getHit(entity: IGameEntity) {
        GameLogger.debug(TAG, "getHit(): entity=$entity")

        if (entity == owner) {
            GameLogger.debug(TAG, "getHit(): ignoring hit from owner")
            return
        }

        val id = (entity as MegaGameEntity).id
        if (ignoreHitsFromIds.contains(id)) {
            GameLogger.debug(TAG, "getHit(): ignoring hit from id=$id")
            return
        }

        when {
            SMOKE_PUFF_ENTITIES.contains(entity::class) -> smokePuff()
            INSTANT_DEATH_ENTITIES.contains(entity::class) -> shatterAndDie()
            else -> {
                hitTimes++
                when {
                    hitTimes > maxHitTimes -> shatterAndDie()
                    overlapsGameCamera() -> requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false)
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_SIZE * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(body.getSize().scl(1.25f)))
        bodyFixture.setHitByExplosionReceiver { entity ->
            if (SMOKE_PUFF_ENTITIES.contains(entity::class)) smokePuff() else shatterAndDie()
        }
        bodyFixture.setHitByBodyReceiver { entity, state ->
            if (state != ProcessState.BEGIN) return@setHitByBodyReceiver
            if (entity != owner && entity is IDamageable && entity.invincible) shatterAndDie()
        }
        bodyFixture.setHitByPlayerReceiver { if (!it.canBeDamaged) shatterAndDie() }
        bodyFixture.setHitByProjectileReceiver {
            if (it !is SmallIceCube && it.owner != owner) getHit(it)
        }
        bodyFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            if (destroyOnHitBlock) shatterAndDie() else getHit(block)
        }
        body.addFixture(bodyFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else gravity
            body.physics.gravity.y = gravity * ConstVals.PPM

            if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.y = 0f

            when {
                body.physics.velocity.x < -0.1f * ConstVals.PPM && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ->
                    body.physics.velocity.x = -0.1f * ConstVals.PPM
                body.physics.velocity.x > 0.1f * ConstVals.PPM && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) ->
                    body.physics.velocity.x = 0.1f * ConstVals.PPM
            }
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.PROJECTILE))
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOnEvents = getStandardEventCullingLogic(this)
        val cullOutOfBounds = getGameCameraCullingLogic(this, CULL_TIME)
        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_EVENTS pairTo cullOnEvents,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds
            )
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(BODY_SIZE * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val region = if (hitTimes == 0) region1 else region2
            sprite.setRegion(region)
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        return component
    }

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
