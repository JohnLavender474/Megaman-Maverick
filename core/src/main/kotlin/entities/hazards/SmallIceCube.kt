package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.vector2Of
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.blocks.BreakableIce
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.*

class SmallIceCube(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAudioEntity, IHazard, IDamager {

    companion object {
        const val TAG = "FragileIceCube"
        private const val DEFAULT_GRAVITY = -0.1f
        private const val GROUND_GRAVITY = -0.01f
        private const val CLAMP = 10f
        private const val CULL_TIME = 2f
        private const val DEFAULT_MAX_HIT_TIMES = 1
        private var region1: TextureRegion? = null
        private var region2: TextureRegion? = null
        private val INSTANT_DEATH_ENTITIES = objectSetOf(
            Megaman::class,
            ChargedShot::class
        )
    }

    private var hitTimes = 0
    private var destroyOnHitBlock = false
    private var maxHitTimes = DEFAULT_MAX_HIT_TIMES
    private var gravity = DEFAULT_GRAVITY

    override fun getEntityType() = EntityType.HAZARD

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

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.velocity.set(trajectory)

        gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, DEFAULT_GRAVITY, Float::class)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, true, Boolean::class)
        body.physics.gravityOn = gravityOn

        val applyFrictionX = spawnProps.getOrDefault(ConstKeys.FRICTION_X, true, Boolean::class)
        body.physics.applyFrictionX = applyFrictionX

        val applyFrictionY = spawnProps.getOrDefault(ConstKeys.FRICTION_Y, true, Boolean::class)
        body.physics.applyFrictionY = applyFrictionY

        val section = spawnProps.getOrDefault(ConstKeys.SECTION, DrawingSection.PLAYGROUND, DrawingSection::class)
        firstSprite!!.priority.section = section

        val priority = spawnProps.getOrDefault(ConstKeys.PRIORITY, 1, Int::class)
        firstSprite!!.priority.value = priority

        val clamp = spawnProps.getOrDefault(ConstKeys.CLAMP, true, Boolean::class)
        body.physics.velocityClamp =
            if (clamp) vector2Of(CLAMP * ConstVals.PPM) else Vector2(Float.MAX_VALUE, Float.MAX_VALUE)

        destroyOnHitBlock = spawnProps.getOrDefault(ConstKeys.HIT_BY_BLOCK, false, Boolean::class)
        maxHitTimes = spawnProps.getOrDefault(ConstKeys.MAX, DEFAULT_MAX_HIT_TIMES, Int::class)
        hitTimes = 0
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = getHit(damageable as IGameEntity)

    private fun shatterAndDie() {
        destroy()
        for (i in 0 until 5) {
            val iceShard = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.ICE_SHARD)!!
            iceShard.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.INDEX pairTo i))
        }
    }

    private fun getHit(entity: IGameEntity) {
        GameLogger.debug(TAG, "getHit(): entity=$entity")
        if (INSTANT_DEATH_ENTITIES.contains(entity::class)) shatterAndDie()
        else {
            hitTimes++
            if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false)
            if (hitTimes > maxHitTimes) shatterAndDie()
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.4f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(0.55f * ConstVals.PPM))
        bodyFixture.setHitByBodyReceiver { entity -> if (entity is SmallIceCube) shatterAndDie() }
        bodyFixture.setHitByPlayerReceiver { getHit(it) }
        bodyFixture.setHitByProjectileReceiver { getHit(it) }
        bodyFixture.setHitByBlockReceiver { if (destroyOnHitBlock) shatterAndDie() else getHit(it) }
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.55f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.225f * ConstVals.PPM
        body.addFixture(feetFixture)

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        leftFixture.offsetFromBodyCenter.x = -0.2f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        rightFixture.offsetFromBodyCenter.x = -0.2f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else gravity
            body.physics.gravity.y = gravity * ConstVals.PPM
            if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.y = 0f

            if (body.physics.velocity.x < -0.1f * ConstVals.PPM &&
                body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
            ) body.physics.velocity.x = -0.1f * ConstVals.PPM
            else if (body.physics.velocity.x > 0.1f * ConstVals.PPM &&
                body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            ) body.physics.velocity.x = 0.1f * ConstVals.PPM
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it) }, cullEvents)
        runnablesOnSpawn.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }
        runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }
        val cullOutOfBounds = getGameCameraCullingLogic(this, CULL_TIME)
        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_EVENTS pairTo cullOnEvents, ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds
            )
        )
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            val region = if (hitTimes == 0) region1 else region2
            _sprite.setRegion(region)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }
}
