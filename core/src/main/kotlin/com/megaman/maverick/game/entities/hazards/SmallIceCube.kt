package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
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
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.BreakableIce
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
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

        const val BODY_SIZE = 0.5f

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
        )
        private val SMOKE_PUFF_ENTITIES = objectSetOf(
            MagmaGoop::class,
            MagmaWave::class,
            MagmaMeteor::class,
            MagmaFlame::class,
            Fireball::class,
            FlameThrower::class,
            FireWall::class
        )
    }

    private var hitTimes = 0
    private var destroyOnHitBlock = false
    private var gravity = DEFAULT_GRAVITY
    private var maxHitTimes = DEFAULT_MAX_HIT_TIMES

    override fun init() {
        if (region1 == null || region2 == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            region1 = atlas.findRegion("${BreakableIce.TAG}/1")
            region2 = atlas.findRegion("${BreakableIce.TAG}/3")
        }
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
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
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = shatterAndDie()

    private fun shatterAndDie() {
        destroy()

        for (i in 0 until 5) {
            val iceShard = MegaEntityFactory.fetch(IceShard::class)!!
            iceShard.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.INDEX pairTo i))
        }
    }

    private fun smokePuff() {
        destroy()

        val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
        puff.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))

        playSoundNow(SoundAsset.WHOOSH_SOUND, false)
    }

    private fun getHit(entity: IGameEntity) {
        GameLogger.debug(TAG, "getHit(): entity=$entity")
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
            if (state == ProcessState.BEGIN && entity is SmallIceCube) shatterAndDie()
        }
        bodyFixture.setHitByPlayerReceiver { if (!it.canBeDamaged) shatterAndDie() }
        bodyFixture.setHitByProjectileReceiver { getHit(it) }
        bodyFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            if (destroyOnHitBlock) shatterAndDie() else getHit(block)
        }
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

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

        return BodyComponentCreator.create(this, body)
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
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            val region = if (hitTimes == 0) region1 else region2
            sprite.setRegion(region)

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        return spritesComponent
    }

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
