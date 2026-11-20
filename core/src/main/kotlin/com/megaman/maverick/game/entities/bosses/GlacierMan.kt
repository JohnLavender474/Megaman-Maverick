package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.*
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.toGameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.state.StateMachine
import com.maverick.game.engine.state.StateMachineBuilder
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.drawables.ui.BossBitsBar
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import com.megaman.maverick.game.entities.bosses.glacierman.GlacierManTuning
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IFireEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.FallingIcicle
import com.megaman.maverick.game.entities.projectiles.FallingIcicle.FallingIcicleState
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.entities.projectiles.Snowhead
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import com.badlogic.gdx.utils.FloatArray as GdxFloatArray
import com.badlogic.gdx.utils.IntArray as GdxIntArray

class GlacierMan(services: MegaServices) : AbstractBoss(services, defeatDur = DEFEAT_DUR), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "GlacierMan"

        const val SPRITE_WIDTH = 4f
        const val SPRITE_HEIGHT = 3f

        private const val CELL_WIDTH = 64f
        private const val CELL_HEIGHT = 48f

        private const val INIT_DUR = 1f
        private const val FIRED_DUR = 1f
        private const val MAX_BRAKE_DUR = 0.75f

        private const val SHOOT_ANIM_DUR = 0.25f
        private const val SLED_SHOT_GAP = 0.5f
        private const val SLED_SHOTS = 2

        private const val FLASH_FRAME_DUR = 0.05f
        private const val THROW_RELEASE_FRAME = 2

        private const val ICE_BLAST_VEL = 10f
        private const val CUBE_GRAVITY = -22.5f
        private const val CHUNK_SPEED_X = 7f
        private const val CHUNK_MIN_TIME = 0.6f
        private const val CHUNK_MAX_TIME = 1.3f

        private const val MEGAMAN_OFFSET_X = 2.5f
        private const val MEGAMAN_ABOVE_OFFSET_Y = 2f

        private const val JUMP_IMPULSE_X = 25f
        private const val JUMP_IMPULSE_Y = 15f
        private const val JUMP_OVER_PAST = 2f
        private const val AIR_TIME_ITERATIONS = 4

        private const val GRAVITY = -22.5f
        private const val GROUND_GRAVITY = -1.5f

        private const val SLED_SPEED = 8f
        private const val BRAKE_FRICTION_X = 1.25f
        private const val VELOCITY_CLAMP_X = 10f
        private const val VELOCITY_CLAMP_Y = 25f
        private const val SLED_RAM_MAX_DUR = 3f

        private const val POUND_FRAMES = 5
        private const val POUND_FRAME_DUR = 0.1f
        private const val POUND_IMPACT_FRAME = 2
        private const val POUND_SHAKE_DUR = 0.4f
        private const val SHAKE_INTERVAL = 0.1f
        private const val SHAKE_X = 0.001f
        private const val SHAKE_Y = 0.005f
        private const val ICICLE_COLUMN_WIDTH = 1f
        private const val ICICLE_GAP_COLUMNS = 2
        private const val LOW_HEALTH_RAIN_RATIO = 0.25f

        private const val SNOWBALL_VEL_UP_X = 9f
        private const val SNOWBALL_VEL_UP_Y = 15f
        private const val SNOWBALL_VEL_STRAIGHT_X = 15f
        private const val SNOWBALL_VEL_STRAIGHT_Y = 6f
        private const val SNOWBALL_AIM_SPEED_X = 10f
        private const val SNOWBALL_AIM_MIN_TIME = 0.4f
        private const val SNOWBALL_AIM_MAX_TIME = 1f

        private const val PHASE_SHAKE_DUR = 0.5f
        private const val PHASE_FLASH_BLINK = 0.1f
        private val PHASE_FLASH_COLOR: Color = Color.CYAN

        private const val FREEZE_FRAMES = 5
        private const val FREEZE_FRAME_DUR = 0.15f
        private const val CRACK_FRAME_DUR = 0.25f
        private const val CRACK_LAST_FRAME_DUR = 0.1f
        private const val SHATTER_FRAMES = 3
        private const val SHATTER_FRAME_DUR = 0.1f
        private const val FREEZE_DUR = FREEZE_FRAMES * FREEZE_FRAME_DUR
        private const val SHATTER_START = FREEZE_DUR + 2 * CRACK_FRAME_DUR + CRACK_LAST_FRAME_DUR
        private const val FREEZE_SEQUENCE_DUR = SHATTER_START + SHATTER_FRAMES * SHATTER_FRAME_DUR
        private const val SHATTER_HOLD_DUR = 1f
        private const val DEFEAT_DUR = FREEZE_SEQUENCE_DUR + SHATTER_HOLD_DUR
        private const val SHARDS_CELL_X = 32f
        private const val SHARDS_CELL_Y = 22f

        private val DAMAGE = objectMapOf<KClass<out IDamagerEntity>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(2),
            ProtoBuster::class pairTo dmgNeg(2),
            PlayerSabreSlash::class pairTo dmgNeg {
                it as PlayerSabreSlash
                if (it.fullStrength) 4 else 2
            },
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 4 else 2
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 4 else 2
            },
            Fireball::class pairTo dmgNeg(8),
            MagmaWave::class pairTo dmgNeg(8),
            MagmaFlame::class pairTo dmgNeg(8),
            ReactorManProjectile::class pairTo dmgNeg(6),
            MoonScythe::class pairTo dmgNeg(2),
            SmallIceCube::class pairTo dmgNeg(2),
            Axe::class pairTo dmgNeg(2),
            Needle::class pairTo dmgNeg(2),
            SlashWave::class pairTo dmgNeg(2),
            PreciousGem::class pairTo dmgNeg(2),
            IceBombExplosion::class pairTo dmgNeg(2),
            ReactorExplosion::class pairTo dmgNeg(2)
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class GlacierManState {
        INIT, STAND, TELL, STOP, JUMP, SLED, BRAKE, DUCK, ICE_BLAST_ATTACK, ICICLE_RAIN, SLED_RAM, PHASE_START, FIRED
    }

    private enum class IceThrow(val key: String, val frames: Int, val spawnX: Float, val spawnY: Float) {
        CHUNK("chunk", 5, 14f, 28f),
        AIM("aim", 4, 13f, 20f),
        AIM_UP("aim_up", 4, 13f, 32f)
    }

    override val damageNegotiator: IDamageNegotiator = object : BossDamageNegotiator() {
        override fun get(damager: IDamagerEntity) = DAMAGE[damager::class]?.get(damager) ?: super.get(damager)
    }
    override val healthPhases = 2
    override val healthBarRegions = gdxArrayOf(ConstKeys.STANDARD, BossBitsBar.PHASE_2_BITS)

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<GlacierManState>
    private val currentState: GlacierManState
        get() = stateMachine.getCurrentElement()
    private var nextState: GlacierManState? = null
    private var tellMove = GlacierManState.JUMP
    private var lastMove: GlacierManState? = null
    private var moveBeforeLast: GlacierManState? = null
    private var jumpOver = false

    private val tuning: GlacierManTuning
        get() = GlacierManTuning.of(services.gameState.hardMode, healthPhase)
    private var volleyTuning = GlacierManTuning.NORMAL

    private val timers = ObjectMap<String, Timer>()
    private val shotTimes = GdxFloatArray()
    private var shotIndex = 0

    private lateinit var animator: Animator
    private lateinit var animations: ObjectMap<String, IAnimation>

    private val walls = Array<GameRectangle>()
    private var wallCount = 0
    private var arenaMinX = 0f
    private var arenaMaxX = 0f
    private val arena = GameRectangle()

    private val volleyAims = Array<OrbAim>()
    private var iceThrow = IceThrow.AIM
    private var throwIndex = 0
    private var throwReleased = false
    private val randomRange: (Float, Float) -> Float = { min, max -> services.random.nextFloat(min, max) }

    private var moveTuning = GlacierManTuning.NORMAL
    private var rainWavesLeft = 0
    private var poundImpacted = false
    private val waveColumns = GdxIntArray()
    private val freeColumns = GdxIntArray()
    private var waveSpawnIndex = 0
    private val icicles = Array<FallingIcicle>()
    private val releasedIcicles = Array<FallingIcicle>()
    private val releaseDeadIcicles: () -> Unit = {
        for (i in icicles.size - 1 downTo 0) {
            val icicle = icicles[i]
            if (!icicle.existenceState.liveAfterUpdate) {
                icicle.runnablesOnDestroyQueued.remove(TAG)
                icicles.removeIndex(i)
            }
        }
    }

    private var ramBounced = false

    private var phaseStartPending = false
    private var shardsSpawned = false

    private var shootUp = false

    private val fired: Boolean
        get() = !timers["fired"].isFinished()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_1.source)
            gdxArrayOf(
                "freeze",
                "frozen_crack",
                "frozen_shatter",
                "fired",
                "stand",
                "stand_shoot",
                "stand_shoot_up",
                "stop",
                "jump",
                "fall",
                "fall_shoot_up",
                "fall_flash",
                "sled",
                "sled_shoot",
                "sled_shoot_up",
                "brake",
                "brake_shoot",
                "brake_shoot_up",
                "brake_flash",
                "duck",
                "duck_shoot",
                "duck_shoot_up",
                "throw_flash",
                "fist_flash",
                "chunk",
                "aim",
                "aim_up",
                "pound"
            ).forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        buildTimers()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        body.physics.defaultFrictionOnSelf.x = 1f
        body.physics.applyFrictionX = false
        body.physics.velocity.setZero()
        body.physics.gravityOn = true

        stateMachine.reset()

        timers.forEach { entry ->
            val key = entry.key
            val timer = entry.value
            when {
                key.equalsAny("shoot_anim", "fired") -> timer.setToEnd()
                else -> timer.reset()
            }
        }

        facing = if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        shootUp = false
        phaseStartPending = false
        shardsSpawned = false
        nextState = null
        tellMove = GlacierManState.JUMP
        lastMove = null
        moveBeforeLast = null
        jumpOver = false
        shotTimes.clear()
        shotIndex = 0
        volleyAims.clear()
        throwIndex = 0
        throwReleased = false
        rainWavesLeft = 0
        poundImpacted = false
        waveColumns.clear()
        waveSpawnIndex = 0
        releaseIcicles()
        releasedIcicles.clear()
        ramBounced = false
        body.physics.velocityClamp.x = VELOCITY_CLAMP_X * ConstVals.PPM

        wallCount = 0
        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.WALL)) {
                if (wallCount == walls.size) walls.add(GameRectangle())
                (value as RectangleMapObject).rectangle.toGameRectangle(walls[wallCount])
                wallCount++
            }
        }
        setArenaEdges(spawn.x)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        releaseIcicles()
        for (i in 0 until releasedIcicles.size) releasedIcicles[i].destroy()
        releasedIcicles.clear()
    }

    override fun isReady(delta: Float) = timers["init"].isFinished()

    override fun triggerDefeat() {
        super.triggerDefeat()
        body.physics.velocity.setZero()
        body.physics.gravityOn = false
        phaseStartPending = false
        shardsSpawned = false
        releaseIcicles()
        for (i in 0 until releasedIcicles.size) {
            val icicle = releasedIcicles[i]
            if (icicle.existenceState == EntityExistenceState.LIVE) icicle.explodeAndDie() else icicle.destroy()
        }
        releasedIcicles.clear()
    }

    private fun releaseIcicles() {
        for (i in 0 until icicles.size) {
            val icicle = icicles[i]
            if (icicle.runnablesOnDestroyQueued[TAG] === releaseDeadIcicles) icicle.runnablesOnDestroyQueued.remove(TAG)
            releasedIcicles.add(icicle)
        }
        icicles.clear()
    }

    override fun onReady() {
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun onHealthPhaseStart(phase: Int) {
        GameLogger.debug(TAG, "onHealthPhaseStart(): phase=$phase")
        phaseStartPending = true
    }

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val takeDamage = super.takeDamageFrom(damager)

        if (takeDamage && damager is IFireEntity) {
            timers["fired"].reset()
            requestToPlaySound(SoundAsset.SOLAR_BLAZE_SOUND, false)
        }

        return takeDamage
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (bossComponent.phase == BossPhase.INTRO) return@add

            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                if (!shardsSpawned && defeatTimer.time >= SHATTER_START) {
                    shardsSpawned = true
                    IceShard.spawn5(cellPoint(SHARDS_CELL_X, SHARDS_CELL_Y, GameObjectPools.fetch(Vector2::class)))
                    if (spawnDefeatOrbs) {
                        spawnDefeatOrbs = false
                        spawnExplosionOrbs(body.getCenter())
                    }
                }
                return@add
            }

            timers["shoot_anim"].update(delta)

            if (phaseStartPending && !fired && body.isSensing(BodySense.FEET_ON_GROUND) &&
                !currentState.equalsAny(GlacierManState.INIT, GlacierManState.JUMP, GlacierManState.FIRED)
            ) {
                phaseStartPending = false
                goTo(GlacierManState.PHASE_START)
            }

            if (currentState != GlacierManState.FIRED && fired) stateMachine.next()

            when (currentState) {
                GlacierManState.INIT -> {
                    val timer = timers["init"]
                    timer.update(delta)

                    if (timer.isFinished()) stateMachine.next()
                }

                GlacierManState.STAND,
                GlacierManState.TELL,
                GlacierManState.DUCK,
                GlacierManState.SLED,
                GlacierManState.STOP -> {
                    if (currentState.equalsAny(GlacierManState.STAND, GlacierManState.DUCK, GlacierManState.TELL))
                        faceTargetPlayer()

                    body.physics.velocity.x =
                        if (currentState == GlacierManState.SLED) SLED_SPEED * ConstVals.PPM * facing.value else 0f

                    val timer = timers[currentState.name.lowercase()]
                    timer.update(delta)

                    while (shotIndex < shotTimes.size && timer.time >= shotTimes[shotIndex]) {
                        shotIndex++
                        shoot(aimAtPlayer = services.random.nextFloat(0f, 1f) < tuning.snowballAimChance)
                    }

                    if (timer.isFinished() || (currentState == GlacierManState.SLED && shouldStopSledding()))
                        endTimedState()
                }

                GlacierManState.ICE_BLAST_ATTACK -> {
                    body.physics.velocity.x = 0f

                    val timer = timers["ice_throw"]
                    timer.update(delta)

                    if (!throwReleased && timer.time >= THROW_RELEASE_FRAME * volleyTuning.throwFrameDur) {
                        throwReleased = true
                        releaseCube()
                    }

                    if (timer.isFinished()) {
                        throwIndex++
                        if (throwIndex < volleyAims.size) startThrow() else goTo(GlacierManState.STAND)
                    }
                }

                GlacierManState.ICICLE_RAIN -> {
                    body.physics.velocity.x = 0f

                    val timer = timers["icicle_rain"]
                    timer.update(delta)

                    if (!poundImpacted && timer.time >= POUND_IMPACT_FRAME * POUND_FRAME_DUR) {
                        poundImpacted = true
                        poundImpact()
                    }

                    while (poundImpacted && waveSpawnIndex < waveColumns.size &&
                        timer.time >= POUND_IMPACT_FRAME * POUND_FRAME_DUR + waveSpawnIndex * moveTuning.icicleStagger
                    ) {
                        spawnIcicle(waveColumns[waveSpawnIndex])
                        waveSpawnIndex++
                    }

                    if (timer.isFinished()) when {
                        rainWavesLeft <= 0 -> goTo(GlacierManState.STAND)
                        !anyOwnLiveIcicle() -> startPound()
                    }
                }

                GlacierManState.SLED_RAM -> {
                    body.physics.velocity.x = moveTuning.sledRamSpeed * ConstVals.PPM * facing.value

                    val timer = timers["sled_ram"]
                    timer.update(delta)

                    if (!ramBounced && shouldStopSledding()) {
                        ramBounced = true
                        facing = facing.opposite()
                        body.physics.velocity.x = moveTuning.sledRamSpeed * ConstVals.PPM * facing.value
                        GameLogger.debug(TAG, "update(): sled ram bounce, facing=$facing")
                    }

                    if ((ramBounced && shouldStopSledding()) || timer.isFinished()) goTo(GlacierManState.BRAKE)
                }

                GlacierManState.JUMP -> {
                    if (!jumpOver) faceTargetPlayer()

                    when {
                        body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND) -> {
                            GameLogger.debug(TAG, "update(): end jump: jumpOver=$jumpOver")
                            if (jumpOver) {
                                faceTargetPlayer()
                                shoot(aimAtPlayer = true)
                            }
                            goTo(GlacierManState.BRAKE)
                        }

                        !jumpOver -> body.physics.velocity.x += JUMP_IMPULSE_X * ConstVals.PPM * facing.value * delta
                    }
                }

                GlacierManState.BRAKE -> {
                    val brakeTimer = timers["brake"]
                    brakeTimer.update(delta)

                    if ((abs(body.physics.velocity.x).epsilonEquals(0f, 0.25f * ConstVals.PPM) &&
                            body.isSensing(BodySense.FEET_ON_GROUND)) || brakeTimer.isFinished()
                    ) {
                        GameLogger.debug(TAG, "update(): end brake")
                        body.physics.velocity.x = 0f
                        goTo(GlacierManState.STOP)
                    }
                }

                GlacierManState.PHASE_START -> {
                    faceTargetPlayer()
                    body.physics.velocity.x = 0f
                    if (body.physics.velocity.y > 0f) body.physics.velocity.y = 0f

                    val timer = timers["phase_start"]
                    timer.update(delta)

                    if (timer.isFinished()) goTo(GlacierManState.STAND)
                }

                GlacierManState.FIRED -> {
                    body.physics.velocity.x = 0f
                    if (body.physics.velocity.y > 0f) body.physics.velocity.y = 0f

                    val timer = timers["fired"]
                    timer.update(delta)

                    if (timer.isFinished()) {
                        damageTimer.reset()
                        stateMachine.next()
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM, 1.75f * ConstVals.PPM)
        body.physics.velocityClamp.set(VELOCITY_CLAMP_X * ConstVals.PPM, VELOCITY_CLAMP_Y * ConstVals.PPM)
        body.physics.applyFrictionX = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
        )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
        )
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY) * ConstVals.PPM

            if ((isFacing(Facing.LEFT) && body.physics.velocity.x < 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                (isFacing(Facing.RIGHT) && body.physics.velocity.x > 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
            ) body.physics.velocity.x = 0f
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER
            )
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            val translateY = if (!defeated && stateMachine.getCurrentElement() == GlacierManState.JUMP) -0.5f else 0f
            sprite.translateY(translateY * ConstVals.PPM)

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.priority.section = if (defeated) DrawingSection.FOREGROUND else DrawingSection.PLAYGROUND
            sprite.priority.value = if (defeated) ExplosionOrb.SPRITE_PRIORITY_VALUE + 1 else 1

            sprite.hidden = (if (defeated) defeatTimer.time >= FREEZE_SEQUENCE_DUR else damageBlink) ||
                services.levelState.roomTransition
            sprite.color = if (isPhaseFlashOn()) PHASE_FLASH_COLOR else Color.WHITE
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        animations = objectMapOf<String, IAnimation>(
            "fired" pairTo Animation(regions["fired"], 3, 1, 0.1f, true),
            "freeze" pairTo Animation(regions["freeze"], FREEZE_FRAMES, 1, FREEZE_FRAME_DUR, false),
            "frozen_crack" pairTo Animation(
                regions["frozen_crack"], 3, 1, gdxArrayOf(CRACK_FRAME_DUR, CRACK_FRAME_DUR, CRACK_LAST_FRAME_DUR), false
            ),
            "frozen_shatter" pairTo Animation(regions["frozen_shatter"], SHATTER_FRAMES, 1, SHATTER_FRAME_DUR, false),
            "stand" pairTo Animation(regions["stand"]),
            "stand_shoot" pairTo Animation(regions["stand_shoot"]),
            "stand_shoot_up" pairTo Animation(regions["stand_shoot_up"]),
            "brake" pairTo Animation(regions["brake"], 3, 1, 0.1f, true),
            "brake_shoot" pairTo Animation(regions["brake_shoot"], 3, 1, 0.1f, true),
            "brake_shoot_up" pairTo Animation(regions["brake_shoot_up"], 3, 1, 0.1f, true),
            "brake_flash" pairTo Animation(regions["brake_flash"], 2, 1, FLASH_FRAME_DUR, true),
            "duck" pairTo Animation(regions["duck"]),
            "duck_shoot" pairTo Animation(regions["duck_shoot"]),
            "duck_shoot_up" pairTo Animation(regions["duck_shoot_up"]),
            "fall" pairTo Animation(regions["fall"], 4, 2, 0.1f, true),
            "fall_shoot_up" pairTo Animation(regions["fall_shoot_up"], 4, 2, 0.1f, true),
            "fall_flash" pairTo Animation(regions["fall_flash"], 2, 1, FLASH_FRAME_DUR, true),
            "jump" pairTo Animation(regions["jump"], 4, 2, 0.1f, true),
            "sled" pairTo Animation(regions["sled"], 2, 2, 0.1f, true),
            "sled_shoot" pairTo Animation(regions["sled_shoot"], 2, 2, 0.1f, true),
            "sled_shoot_up" pairTo Animation(regions["sled_shoot_up"], 2, 2, 0.1f, true),
            "stop" pairTo Animation(regions["stop"], 3, 3, 0.075f, false),
            "throw_flash" pairTo Animation(regions["throw_flash"], 2, 1, FLASH_FRAME_DUR, true),
            "fist_flash" pairTo Animation(regions["fist_flash"], 2, 1, FLASH_FRAME_DUR, true),
            "chunk" pairTo Animation(regions["chunk"], IceThrow.CHUNK.frames, 1, 0.1f, false),
            "aim" pairTo Animation(regions["aim"], IceThrow.AIM.frames, 1, 0.1f, false),
            "aim_up" pairTo Animation(regions["aim_up"], IceThrow.AIM_UP.frames, 1, 0.1f, false),
            "pound" pairTo Animation(regions["pound"], POUND_FRAMES, 1, POUND_FRAME_DUR, false)
        )
        val keySupplier: (String?) -> String? = {
            if (defeated) defeatKey() else if (fired) "fired" else {
                val state = stateMachine.getCurrentElement()

                val key = when (state) {
                    GlacierManState.INIT -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "stop" else "fall"
                    GlacierManState.JUMP -> if (body.physics.velocity.y > 0f) "jump" else "fall"
                    GlacierManState.TELL -> tellKey(tellMove)
                    GlacierManState.ICE_BLAST_ATTACK -> iceThrow.key
                    GlacierManState.ICICLE_RAIN ->
                        if (timers["icicle_rain"].time < POUND_FRAMES * POUND_FRAME_DUR) "pound" else "stand"

                    GlacierManState.SLED_RAM -> "sled"
                    GlacierManState.PHASE_START -> "fist_flash"
                    else -> state.name.lowercase()
                }

                when {
                    !timers["shoot_anim"].isFinished() -> {
                        val shootKey = if (shootUp) "${key}_shoot_up" else "${key}_shoot"
                        if (animations.containsKey(shootKey)) shootKey else key
                    }

                    else -> key
                }
            }
        }
        val onChangeKey: (Animator, String?, String?) -> Unit = { _, oldKey, newKey ->
            if (newKey != null && oldKey != null && newKey.contains("_shoot") && !oldKey.contains("_shoot")) {
                val newKeyRaw = newKey.removeSuffix("_up").removeSuffix("_shoot")
                if (newKeyRaw == oldKey) {
                    val oldAnimation = animations[oldKey]
                    val currentAnimation = animations[newKey]
                    if (oldAnimation != null && currentAnimation != null) {
                        val oldAnimIndex = oldAnimation.getIndex()
                        currentAnimation.setIndex(oldAnimIndex)
                    }
                }
            }
        }
        animator = Animator(keySupplier, animations, onChangeKey = onChangeKey)
        return AnimationsComponent(this, animator)
    }

    private fun buildStateMachine(): StateMachine<GlacierManState> {
        val builder = StateMachineBuilder<GlacierManState>()
        GlacierManState.entries.forEach { state -> builder.state(state.name, state) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(GlacierManState.INIT.name)
            .transition(GlacierManState.INIT.name, GlacierManState.STAND.name) { bossComponent.phase != BossPhase.ENTRANCE }
            .transition(GlacierManState.FIRED.name, GlacierManState.STAND.name) { true }
        GlacierManState.entries.forEach { from ->
            if (from.equalsAny(GlacierManState.INIT, GlacierManState.FIRED)) return@forEach
            builder.transition(from.name, GlacierManState.FIRED.name) { fired }
            GlacierManState.entries.forEach { to ->
                if (to != from && !to.equalsAny(GlacierManState.INIT, GlacierManState.FIRED))
                    builder.transition(from.name, to.name) { nextState == to }
            }
        }
        return builder.build()
    }

    private fun buildTimers() {
        val row = GlacierManTuning.NORMAL
        timers.put("fired", Timer(FIRED_DUR))
        timers.put("shoot_anim", Timer(SHOOT_ANIM_DUR))
        timers.put("init", Timer(INIT_DUR))
        timers.put("stand", Timer(row.standDurMax))
        timers.put("tell", Timer(row.sledTellDur))
        timers.put("duck", Timer(row.duckDur))
        timers.put("sled", Timer(row.sledDur))
        timers.put("brake", Timer(MAX_BRAKE_DUR))
        timers.put("stop", Timer(row.stopDur))
        timers.put("ice_throw", Timer(IceThrow.CHUNK.frames * row.throwFrameDur + row.throwRest))
        timers.put("icicle_rain", Timer(POUND_FRAMES * POUND_FRAME_DUR))
        timers.put("sled_ram", Timer(SLED_RAM_MAX_DUR))
        timers.put("phase_start", Timer(row.phaseStartDur))
    }

    private fun goTo(state: GlacierManState) {
        nextState = state
        val current = stateMachine.next()
        GameLogger.debug(TAG, "goTo(): state=$state, current=$current")
    }

    private fun tellThen(move: GlacierManState, jumpOver: Boolean = false) {
        tellMove = move
        this.jumpOver = jumpOver
        goTo(GlacierManState.TELL)
    }

    private fun startMove(move: GlacierManState) {
        if (move.equalsAny(GlacierManState.DUCK, GlacierManState.ICICLE_RAIN)) goTo(move) else tellThen(move)
    }

    private fun endTimedState() {
        when (currentState) {
            GlacierManState.STAND -> startMove(pickMove())
            GlacierManState.TELL -> goTo(tellMove)
            GlacierManState.DUCK -> when {
                services.random.nextFloat(0f, 1f) < tuning.duckSledChainChance || isMegamanOutsideOffsetX() ->
                    tellThen(GlacierManState.SLED)

                isMegamanAboveOffsetY() -> tellThen(GlacierManState.JUMP)
                else -> goTo(GlacierManState.STAND)
            }

            GlacierManState.SLED -> when {
                isMegamanAboveOffsetY() -> {
                    jumpOver = false
                    goTo(GlacierManState.JUMP)
                }

                shouldStopSledding() && services.random.nextFloat(0f, 1f) < tuning.sledJumpChainChance ->
                    tellThen(GlacierManState.JUMP, jumpOver = true)

                else -> goTo(GlacierManState.BRAKE)
            }

            GlacierManState.STOP -> goTo(GlacierManState.STAND)
            else -> throw IllegalStateException("$TAG: no timed end for state $currentState")
        }
    }

    private fun pickMove(): GlacierManState {
        val row = tuning
        val far = isMegamanOutsideOffsetX()
        val above = isMegamanAboveOffsetY()

        val iceBlast = repeatScaled(
            GlacierManState.ICE_BLAST_ATTACK,
            if (canIceBlast()) row.iceBlastWeight * (if (far) row.farIceBlastScale else 1f) else 0f,
            row
        )
        val duck = repeatScaled(GlacierManState.DUCK, if (above) 0f else row.duckWeight, row)
        val sled = repeatScaled(GlacierManState.SLED, if (far) row.sledWeight else 0f, row)
        val jump = repeatScaled(
            GlacierManState.JUMP,
            row.jumpWeight * (if (far) 1f else row.nearJumpScale) * (if (above) row.aboveJumpScale else 1f),
            row
        )
        val icicleRain = repeatScaled(GlacierManState.ICICLE_RAIN, row.icicleRainWeight, row)
        val sledRam = repeatScaled(GlacierManState.SLED_RAM, row.sledRamWeight, row)

        val total = iceBlast + duck + sled + jump + icicleRain + sledRam
        val move = when {
            total <= 0f -> if (lastMove == GlacierManState.JUMP && moveBeforeLast == GlacierManState.JUMP)
                GlacierManState.DUCK else GlacierManState.JUMP

            else -> weightedMove(services.random.nextFloat(0f, total), iceBlast, duck, sled, icicleRain, sledRam, jump)
        }
        GameLogger.debug(
            TAG, "pickMove(): move=$move, iceBlast=$iceBlast, duck=$duck, sled=$sled, jump=$jump, " +
                "icicleRain=$icicleRain, sledRam=$sledRam, last=$lastMove, beforeLast=$moveBeforeLast"
        )
        return move
    }

    private fun weightedMove(
        roll: Float,
        iceBlast: Float,
        duck: Float,
        sled: Float,
        icicleRain: Float,
        sledRam: Float,
        jump: Float
    ) = when {
        roll < iceBlast -> GlacierManState.ICE_BLAST_ATTACK
        roll < iceBlast + duck -> GlacierManState.DUCK
        roll < iceBlast + duck + sled -> GlacierManState.SLED
        roll < iceBlast + duck + sled + icicleRain -> GlacierManState.ICICLE_RAIN
        roll < iceBlast + duck + sled + icicleRain + sledRam -> GlacierManState.SLED_RAM
        jump > 0f -> GlacierManState.JUMP
        sled > 0f -> GlacierManState.SLED
        duck > 0f -> GlacierManState.DUCK
        else -> GlacierManState.ICE_BLAST_ATTACK
    }

    private fun repeatScaled(move: GlacierManState, weight: Float, row: GlacierManTuning) = when {
        lastMove == move && moveBeforeLast == move -> 0f
        lastMove == move -> weight * row.lastMoveScale
        else -> weight
    }

    private fun recordMove(move: GlacierManState) {
        moveBeforeLast = lastMove
        lastMove = move
    }

    private fun tellKey(move: GlacierManState) = when (move) {
        GlacierManState.SLED, GlacierManState.SLED_RAM -> "brake_flash"
        GlacierManState.JUMP -> "fall_flash"
        else -> "throw_flash"
    }

    private fun tellDur(move: GlacierManState, row: GlacierManTuning) = when (move) {
        GlacierManState.SLED -> row.sledTellDur
        GlacierManState.SLED_RAM -> row.sledRamTellDur
        GlacierManState.JUMP -> row.jumpTellDur
        else -> row.iceBlastTellDur
    }

    private fun setArenaEdges(spawnX: Float) {
        arenaMinX = -Float.MAX_VALUE
        arenaMaxX = Float.MAX_VALUE
        for (i in 0 until wallCount) {
            val wall = walls[i]
            if (wall.getMaxX() <= spawnX) arenaMinX = max(arenaMinX, wall.getMaxX())
            else if (wall.getX() >= spawnX) arenaMaxX = min(arenaMaxX, wall.getX())
        }
        if (arenaMinX == -Float.MAX_VALUE || arenaMaxX == Float.MAX_VALUE) throw IllegalStateException(
            "$TAG: the spawn needs a wall object on each side of the boss (spawn props wall_1 and wall_2); " +
                "found $wallCount wall(s) for spawn x=$spawnX"
        )
    }

    private fun cellPoint(x: Float, y: Float, out: Vector2): Vector2 {
        val scaleX = SPRITE_WIDTH * ConstVals.PPM / CELL_WIDTH
        val scaleY = SPRITE_HEIGHT * ConstVals.PPM / CELL_HEIGHT
        val cellX = if (isFacing(Facing.LEFT)) x else CELL_WIDTH - x
        val spriteLeft = body.getX() + body.getWidth() / 2f - SPRITE_WIDTH * ConstVals.PPM / 2f
        return out.set(spriteLeft + cellX * scaleX, body.getY() + y * scaleY)
    }

    private fun defeatKey() = when {
        defeatTimer.time < FREEZE_DUR -> "freeze"
        defeatTimer.time < SHATTER_START -> "frozen_crack"
        else -> "frozen_shatter"
    }

    private fun isPhaseFlashOn() = currentState == GlacierManState.PHASE_START && !defeated &&
        (timers["phase_start"].time / PHASE_FLASH_BLINK).toInt() % 2 == 0

    private fun startPhasePose() {
        GameLogger.debug(TAG, "startPhasePose(): healthPhase=$healthPhase")
        timers["phase_start"].resetDuration(tuning.phaseStartDur)
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo PHASE_SHAKE_DUR,
                    ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                    ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )
        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun canIceBlast(): Boolean {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) return false
        val point = GameObjectPools.fetch(Vector2::class)
        val throws = IceThrow.entries
        for (i in 0 until throws.size) {
            cellPoint(throws[i].spawnX, throws[i].spawnY, point)
            for (j in 0 until wallCount) if (walls[j].contains(point)) return false
        }
        return true
    }

    private fun startVolley() {
        volleyTuning = tuning
        volleyAims.clear()
        val aims = OrbAim.entries
        for (i in 0 until volleyTuning.iceBlastCount) volleyAims.add(aims[i % aims.size])
        services.random.shuffle(volleyAims)
        throwIndex = 0
        startThrow()
    }

    private fun startThrow() {
        faceTargetPlayer()
        iceThrow = when {
            isMegamanAboveOffsetY() -> IceThrow.AIM_UP
            services.random.nextFloat(0f, 1f) < volleyTuning.chunkChance -> IceThrow.CHUNK
            else -> IceThrow.AIM
        }
        val animation = animations[iceThrow.key]
        animation.setFrameDuration(volleyTuning.throwFrameDur)
        animation.reset()
        timers["ice_throw"].resetDuration(iceThrow.frames * volleyTuning.throwFrameDur + volleyTuning.throwRest)
        throwReleased = false
    }

    private fun releaseCube() {
        val aim = volleyAims[throwIndex]
        GameLogger.debug(TAG, "releaseCube(): throw=$iceThrow, aim=$aim, index=$throwIndex")

        val spawn = cellPoint(iceThrow.spawnX, iceThrow.spawnY, GameObjectPools.fetch(Vector2::class))
        arena.set(arenaMinX, body.getY(), arenaMaxX - arenaMinX, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        val target = OrbAim.point(
            aim,
            targetPlayer.body.getCenter(),
            targetPlayer.body.physics.velocity,
            volleyTuning.cubeLeadTime,
            arena,
            randomRange,
            GameObjectPools.fetch(Vector2::class)
        )

        val lob = iceThrow == IceThrow.CHUNK
        val trajectory = GameObjectPools.fetch(Vector2::class)
        if (lob) arcTo(spawn, target, CUBE_GRAVITY, CHUNK_SPEED_X, CHUNK_MIN_TIME, CHUNK_MAX_TIME, trajectory)
        else trajectory.set(target).sub(spawn).nor().scl(ICE_BLAST_VEL * ConstVals.PPM)

        val cube = MegaEntityFactory.fetch(SmallIceCube::class)!!
        cube.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.GRAVITY pairTo CUBE_GRAVITY,
                ConstKeys.GRAVITY_ON pairTo lob,
                ConstKeys.CLAMP pairTo !lob,
                ConstKeys.FRICTION_X pairTo false,
                ConstKeys.FRICTION_Y pairTo false,
                ConstKeys.HIT_BY_BLOCK pairTo true,
                ConstKeys.MAX pairTo 1
            )
        )
    }

    private fun arcTo(
        from: Vector2,
        to: Vector2,
        gravity: Float,
        speedX: Float,
        minTime: Float,
        maxTime: Float,
        out: Vector2
    ): Vector2 {
        val dx = to.x - from.x
        val time = MathUtils.clamp(abs(dx) / (speedX * ConstVals.PPM), minTime, maxTime)
        return out.set(dx / time, (to.y - from.y) / time - 0.5f * gravity * ConstVals.PPM * time)
    }

    private fun startPound() {
        rainWavesLeft--
        poundImpacted = false
        waveColumns.clear()
        waveSpawnIndex = 0
        faceTargetPlayer()
        animations["pound"].reset()
        val waveDur = POUND_IMPACT_FRAME * POUND_FRAME_DUR +
            (moveTuning.icicleCount - 1) * moveTuning.icicleStagger + moveTuning.icicleWaveRest
        timers["icicle_rain"].resetDuration(max(POUND_FRAMES * POUND_FRAME_DUR, waveDur))
    }

    private fun poundImpact() {
        GameLogger.debug(TAG, "poundImpact(): wavesLeft=$rainWavesLeft")
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo POUND_SHAKE_DUR,
                    ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                    ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
        buildWave()
    }

    private fun buildWave() {
        val columns = ((arenaMaxX - arenaMinX) / (ICICLE_COLUMN_WIDTH * ConstVals.PPM)).toInt()

        arena.set(arenaMinX, body.getY(), arenaMaxX - arenaMinX, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        val lead = OrbAim.point(
            OrbAim.LEAD,
            targetPlayer.body.getCenter(),
            targetPlayer.body.physics.velocity,
            moveTuning.icicleLeadTime,
            arena,
            randomRange,
            GameObjectPools.fetch(Vector2::class)
        )
        val leadColumn = columnOf(lead.x, columns)
        val bossMinColumn = columnOf(body.getX(), columns)
        val bossMaxColumn = columnOf(body.getMaxX(), columns)

        var gapStarts = 0
        for (start in 0..columns - ICICLE_GAP_COLUMNS)
            if (isSafeGap(start, leadColumn, bossMinColumn, bossMaxColumn)) gapStarts++
        if (gapStarts == 0) throw IllegalStateException(
            "$TAG: no safe icicle gap of $ICICLE_GAP_COLUMNS columns in an arena of $columns columns " +
                "(lead column $leadColumn, boss columns $bossMinColumn to $bossMaxColumn); widen the arena walls"
        )
        var pick = services.random.nextInt(0, gapStarts - 1)
        var gapStart = 0
        for (start in 0..columns - ICICLE_GAP_COLUMNS) {
            if (!isSafeGap(start, leadColumn, bossMinColumn, bossMaxColumn)) continue
            if (pick == 0) {
                gapStart = start
                break
            }
            pick--
        }

        freeColumns.clear()
        for (column in 0 until columns)
            if (column != leadColumn && (column < gapStart || column >= gapStart + ICICLE_GAP_COLUMNS))
                freeColumns.add(column)
        val others = min(moveTuning.icicleCount - 1, freeColumns.size)
        while (freeColumns.size > others) freeColumns.removeIndex(services.random.nextInt(0, freeColumns.size - 1))

        waveColumns.clear()
        waveColumns.add(leadColumn)
        val bossOnLeft = body.getX() + body.getWidth() / 2f < (arenaMinX + arenaMaxX) / 2f
        for (i in 0 until freeColumns.size)
            waveColumns.add(if (bossOnLeft) freeColumns[i] else freeColumns[freeColumns.size - 1 - i])
        waveSpawnIndex = 0

        GameLogger.debug(TAG, "buildWave(): lead=$leadColumn, gapStart=$gapStart, columns=$waveColumns")
    }

    private fun isSafeGap(start: Int, leadColumn: Int, bossMinColumn: Int, bossMaxColumn: Int): Boolean {
        val end = start + ICICLE_GAP_COLUMNS - 1
        return (leadColumn < start || leadColumn > end) && (end < bossMinColumn || start > bossMaxColumn)
    }

    private fun columnOf(x: Float, columns: Int) =
        MathUtils.clamp(((x - arenaMinX) / (ICICLE_COLUMN_WIDTH * ConstVals.PPM)).toInt(), 0, columns - 1)

    private fun spawnIcicle(column: Int) {
        val x = arenaMinX + (column + 0.5f) * ICICLE_COLUMN_WIDTH * ConstVals.PPM
        val spawn = GameObjectPools.fetch(Vector2::class).set(x, ceilingAt(x))
        val icicle = MegaEntityFactory.fetch(FallingIcicle::class)!!
        icicle.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.STATE pairTo FallingIcicleState.FORM,
                ConstKeys.DELAY pairTo moveTuning.icicleStillDur,
                ConstKeys.DURATION pairTo moveTuning.icicleShakeDur
            )
        )
        if (!icicle.existenceState.liveAfterUpdate || icicles.contains(icicle, true)) return
        icicle.runnablesOnDestroyQueued.put(TAG, releaseDeadIcicles)
        icicles.add(icicle)
    }

    private fun ceilingAt(x: Float): Float {
        var ceiling = body.getY() + ConstVals.VIEW_HEIGHT * ConstVals.PPM
        val blocks = MegaGameEntities.getOfType(EntityType.BLOCK).orderedItems()
        for (i in 0 until blocks.size) {
            val blockBody = (blocks[i] as Block).body
            if (blockBody.getX() < x && blockBody.getMaxX() > x && blockBody.getY() >= body.getMaxY())
                ceiling = min(ceiling, blockBody.getY())
        }
        return ceiling
    }

    private fun anyOwnLiveIcicle() = !icicles.isEmpty

    private fun canShootUp() = !stateMachine.getCurrentElement()
        .equalsAny(GlacierManState.SLED, GlacierManState.SLED_RAM, GlacierManState.JUMP)

    private fun shoot(aimAtPlayer: Boolean) {
        val onGround = body.isSensing(BodySense.FEET_ON_GROUND)
        val playerCenter = targetPlayer.body.getCenter()
        val inFront = (playerCenter.x - (body.getX() + body.getWidth() / 2f)) * facing.value > 0f
        val aimed = aimAtPlayer && onGround && inFront

        shootUp = canShootUp() &&
            (isFalling() || isMegamanAboveOffsetY() ||
                (!aimed && services.random.nextFloat(0f, 1f) <= tuning.shootUpChance))

        GameLogger.debug(TAG, "shoot(): aimed=$aimed, shootUp=$shootUp")

        val spawn = body.getCenter()
        val trajectory = GameObjectPools.fetch(Vector2::class).setZero()
        if (onGround) {
            when {
                stateMachine.getCurrentElement() == GlacierManState.SLED ->
                    spawn.add(0.75f * facing.value * ConstVals.PPM, 0.1f * ConstVals.PPM)

                shootUp -> spawn.add(0.4f * facing.value * ConstVals.PPM, 0.1f * ConstVals.PPM)
                else -> spawn.add(0.5f * facing.value * ConstVals.PPM, 0f)
            }

            if (aimed) arcTo(
                spawn, playerCenter, Snowhead.GRAVITY, SNOWBALL_AIM_SPEED_X, SNOWBALL_AIM_MIN_TIME,
                SNOWBALL_AIM_MAX_TIME, trajectory
            ) else trajectory.set(
                (if (shootUp) SNOWBALL_VEL_UP_X else SNOWBALL_VEL_STRAIGHT_X) * facing.value,
                if (shootUp) SNOWBALL_VEL_UP_Y else SNOWBALL_VEL_STRAIGHT_Y
            ).scl(ConstVals.PPM.toFloat())
        }

        val snowhead = MegaEntityFactory.fetch(Snowhead::class)!!
        snowhead.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TYPE pairTo ConstKeys.BLANK,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        timers["shoot_anim"].reset()
    }

    private fun jump() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPositionPoint(Position.BOTTOM_CENTER), targetPlayer.body.getCenter(), JUMP_IMPULSE_Y * ConstVals.PPM
        )
        body.physics.velocity.y = impulse.y
    }

    private fun jumpOverTargetPlayer() {
        val playerX = targetPlayer.body.getCenter().x
        val bodyX = body.getX() + body.getWidth() / 2f
        facing = if (playerX < bodyX) Facing.LEFT else Facing.RIGHT
        val halfWidth = body.getWidth() / 2f
        val landX = MathUtils.clamp(
            playerX + facing.value * JUMP_OVER_PAST * ConstVals.PPM, arenaMinX + halfWidth, arenaMaxX - halfWidth
        )
        val airTime = airTime(JUMP_IMPULSE_Y, GRAVITY, body.physics.defaultFrictionOnSelf.y)
        body.physics.velocity.set((landX - bodyX) / airTime, JUMP_IMPULSE_Y * ConstVals.PPM)
    }

    private fun airTime(speedY: Float, gravity: Float, drag: Float): Float {
        var time = 2f * speedY / -gravity
        if (drag <= 0f) return time
        val terminal = gravity / drag
        repeat(AIR_TIME_ITERATIONS) {
            val decay = exp(-drag * time)
            val height = terminal * time + (speedY - terminal) * (1f - decay) / drag
            val speed = terminal + (speedY - terminal) * decay
            time -= height / speed
        }
        return time
    }

    private fun faceTargetPlayer() {
        facing = if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    private fun isFalling() =
        stateMachine.getCurrentElement() == GlacierManState.JUMP && body.physics.velocity.y < 0f && !body.isSensing(BodySense.FEET_ON_GROUND)

    private fun onChangeState(current: GlacierManState, previous: GlacierManState) {
        GameLogger.debug(TAG, "onChangeState(): new=$current, old=$previous")

        shotTimes.clear()
        shotIndex = 0

        val row = tuning
        when (current) {
            GlacierManState.STAND -> {
                jumpOver = false
                timers["stand"].resetDuration(services.random.nextFloat(row.standDurMin, row.standDurMax))
                shotTimes.add(services.random.nextFloat(row.standShotMin, row.standShotMax))
            }

            GlacierManState.TELL -> timers["tell"].resetDuration(tellDur(tellMove, row))

            GlacierManState.DUCK -> {
                recordMove(current)
                timers["duck"].resetDuration(row.duckDur)
                shotTimes.add(services.random.nextFloat(row.duckShotMin, row.duckShotMax))
            }

            GlacierManState.SLED -> {
                recordMove(current)
                timers["sled"].resetDuration(row.sledDur)
                for (i in 1..SLED_SHOTS) shotTimes.add(
                    i * SLED_SHOT_GAP + services.random.nextFloat(-row.sledShotJitter, row.sledShotJitter)
                )
            }

            GlacierManState.JUMP -> {
                recordMove(current)
                if (jumpOver) jumpOverTargetPlayer() else jump()
            }

            GlacierManState.ICE_BLAST_ATTACK -> {
                recordMove(current)
                startVolley()
            }

            GlacierManState.ICICLE_RAIN -> {
                recordMove(current)
                moveTuning = row
                val lowHealth = healthPhase >= healthPhases &&
                    getCurrentHealth() < LOW_HEALTH_RAIN_RATIO * getMaxHealth()
                rainWavesLeft = if (lowHealth) row.lowHealthRainWaves else row.icicleRainWaves
                startPound()
            }

            GlacierManState.SLED_RAM -> {
                recordMove(current)
                moveTuning = row
                ramBounced = false
                timers["sled_ram"].reset()
                body.physics.velocityClamp.x = max(VELOCITY_CLAMP_X, row.sledRamSpeed) * ConstVals.PPM
            }

            GlacierManState.BRAKE -> {
                GameLogger.debug(TAG, "onChangeState(): start brake, apply brake friction")
                timers["brake"].reset()
                body.physics.defaultFrictionOnSelf.x = BRAKE_FRICTION_X
                body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", false)
                body.physics.applyFrictionX = true
            }

            GlacierManState.STOP -> timers["stop"].resetDuration(row.stopDur)

            GlacierManState.PHASE_START -> startPhasePose()

            else -> GameLogger.debug(TAG, "onChangeState(): no change when current=$current")
        }

        if (previous == GlacierManState.SLED_RAM) body.physics.velocityClamp.x = VELOCITY_CLAMP_X * ConstVals.PPM

        if (previous == GlacierManState.BRAKE) {
            GameLogger.debug(TAG, "onChangeState(): stop brake")
            body.physics.defaultFrictionOnSelf.x = ConstVals.STANDARD_RESISTANCE_X
            body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", true)
            body.physics.applyFrictionX = true
        }

        if (previous != GlacierManState.BRAKE && current != GlacierManState.BRAKE) {
            body.physics.defaultFrictionOnSelf.x = 1f
            body.physics.applyFrictionX = false
        }
    }

    private fun isMegamanAboveOffsetY() = targetPlayer.body.getMaxY() >= body.getY() + MEGAMAN_ABOVE_OFFSET_Y * ConstVals.PPM

    private fun isMegamanOutsideOffsetX() = abs(targetPlayer.body.getX() - body.getX()) > MEGAMAN_OFFSET_X * ConstVals.PPM

    private fun shouldStopSledding() =
        (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
}
