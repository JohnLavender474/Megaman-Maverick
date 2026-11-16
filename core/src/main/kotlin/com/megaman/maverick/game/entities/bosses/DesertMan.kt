package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.animations.*
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.*
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.interfaces.UpdateFunction
import com.maverick.game.engine.common.objects.GamePair
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.events.Event
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
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.desertman.DesertManTuning
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.decorations.DustPuff
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.reflect.KClass

class DesertMan(services: MegaServices) : AbstractBoss(services), IAnimatedEntity, ILightSource, IFaceable {

    companion object {
        const val TAG = "DesertMan"

        const val SPRITE_SIZE = 3.5f
        const val SPRITE_Y_OFFSET = -0.5f
        private const val TORNADO_RISE_DUR = 0.2f

        private const val INIT_DUR = 1f
        private val INTRO_FRAME_DURS = gdxArrayOf(0.2f, 0.12f, 0.12f, 0.25f, 0.08f, 0.08f, 1f)

        private const val PHASE_SHAKE_DUR = 0.5f
        private const val PHASE_FLASH_DUR = 0.6f
        private const val SHAKE_X = 0f
        private const val SHAKE_Y = 0.005f
        private const val SHAKE_INTERVAL = 0.1f

        private const val DANCE_DUR = 2.5f
        private const val DANCE_FLASH_DELAY = 0.75f
        private const val DANCE_SPAWN_NEEDLES_DELAY = 0.5f

        private const val TORNADO_DUR = 1.25f
        private const val TORNADO_SAND_DELAY = 0.1f
        private const val TORNADO_Y_THRESHOLD = 3f
        private const val TORNADO_IMPULSE_X = 50f
        private const val TORNADO_HEIGHT = 2f
        private const val TORNADO_TURN_SPEED = 2f
        private const val TORNADO_TURN_MARGIN = 1f

        private const val BURROW_SINK_DEPTH = 1f
        private const val BURROW_FRAMES = 4
        private const val BURROW_SAND_DELAY = 0.1f
        private const val ERUPT_RISE_DUR = 0.3f
        private const val ERUPT_FRAMES = 3
        private const val ERUPT_OVERSHOOT_Y = 1f
        private const val ERUPT_MAX_SPEED_X = 12f
        private const val BURROW_TELL_SPREAD = 0.25f
        private const val BURROW_LIGHT_Y_OFFSET = 0.5f

        private const val JUMP_MAX_IMPULSE_X = 12f
        private const val JUMP_IMPULSE_Y = 15f

        private const val WALL_JUMP_IMPULSE_X = 5f

        private const val WALL_SLIDE_DUR = 0.5f
        private const val WALL_SLIDE_FRICTION_Y = 6f

        private const val PUNCH_Y_THRESHOLD = 1.5f
        private const val SHORT_PUNCH_WIDTH = 0.75f
        private const val PUNCH_X_THRESHOLD = 8f
        private const val PUNCH_MAX_REACH = 9f
        private const val PUNCH_RETRACT_SPEED = 16f
        private const val PUNCH_MAX_SPEED = 30f
        private const val PUNCH_FLASH_INTERVAL = 0.06f
        private const val PUNCH_TREMBLE_INTERVAL = 0.05f
        private const val PUNCH_SPEED_LINES_RATIO = 0.75f
        private const val PUNCH_SPEED_LINES_FRAME_DUR = 0.04f
        private const val PUNCH_WALL_SHAKE_DUR = 0.2f

        private const val ART_SIZE = 64f
        private const val ART_PX = SPRITE_SIZE * ConstVals.PPM / ART_SIZE
        private const val PUNCH_SHOULDER_X = 20
        private const val PUNCH_REST_LENGTH = 10
        private const val PUNCH_PULL_BACK = 4
        private const val PUNCH_TREMBLE_X = 1
        private const val PUNCH_ARM_BOTTOM = 14
        private const val PUNCH_ARM_HEIGHT = 5
        private const val PUNCH_FIST_WIDTH = 12
        private const val PUNCH_FIST_HEIGHT = 10
        private const val PUNCH_FIST_BOTTOM = 13
        private const val PUNCH_FIST_JOIN = 6
        private const val PUNCH_FIST_HIT_WIDTH = 11
        private const val PUNCH_FIST_HIT_HEIGHT = 9
        private const val PUNCH_SPEED_WIDTH = 22
        private const val PUNCH_SPEED_HEIGHT = 17
        private const val PUNCH_SPEED_BOTTOM = 9
        private const val PUNCH_SPEED_FROM_TIP = 12

        private const val THROW_REWIND_DUR = 0.3f
        private const val THROW_MIN_X = 3f
        private const val THROW_MAX_AIM_ANGLE = 60f
        private const val THROW_FLASH_FRAME_DUR = 0.06f
        private const val THROW_FIST_X = 6
        private const val THROW_FIST_Y = 18
        private const val THROW_BURST_X = 3
        private const val THROW_BURST_SIZE = 14
        private const val THROW_BURST_FRAMES = 3
        private const val THROW_BURST_FRAME_DUR = 0.05f
        private const val THROW_BURST_DUR = THROW_BURST_FRAMES * THROW_BURST_FRAME_DUR

        private const val BODY_WIDTH = 1.5f
        private const val BODY_HEIGHT = 1.75f
        private const val VEL_CLAMP_X = 20f
        private const val VEL_CLAMP_Y = 30f
        private const val DEFAULT_FRICTION_X = 5f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val SAND_FRICTION_X = 10f
        private const val GRAVITY = -22.5f

        private const val ARM_HEIGHT = 0.5f

        private const val NEEDLE_GRAV = -15.0f
        private const val NEEDLE_IMPULSE = 15f
        private const val NEEDLE_Y_OFFSET = 0.1f

        private val NEEDLE_ANGLES = gdxArrayOf(90f, 70f, 45f, 15f, 0f, 345f, 315f, 290f, 270f)
        private val NEEDLE_X_OFFSETS = gdxArrayOf(-0.2f, -0.15f, -0.1f, -0.05f, 0f, 0.05f, 0.1f, 0.15f, 0.2f)

        private const val NEEDLE_SPIN_JUMP_IMPULSE_Y = 12f
        private const val NEEDLE_SPIN_HOVER_IMPULSE_Y = 5f
        private const val NEEDLE_SPIN_KEEP_DISTANCE = 3f
        private const val NEEDLE_SPIN_MAX_APPROACH = 6f
        private const val NEEDLE_SPIN_END_DUR = 0.3f
        private const val NEEDLE_SPIN_TELL_FLASH_DUR = 0.08f
        private const val NEEDLE_SPIN_FRAME_DUR = 0.06f
        private const val NEEDLE_SPIN_FRAMES = 4
        private const val NEEDLE_SPIN_Y_OFFSET = 0.25f

        private val NEEDLE_SPIN_ANGLES = gdxArrayOf(
            gdxArrayOf(90f, 60f, 30f, 0f, 330f, 300f, 270f),
            gdxArrayOf(75f, 45f, 15f, 345f, 315f, 285f)
        )

        private const val LIGHT_SOURCE_RADIUS = 2
        private const val LIGHT_SOURCE_RADIANCE = 1.5f

        private val REGION_KEYS = gdxArrayOf(
            "stand", "jump", "wall_slide", "dance", "dance_flash", "punch_body", "punch_arm", "punch_fist",
            "punch_speed", "tornado", "tornado_start", "tornado_punch", "burrow", "erupt", "defeated", "needle_spin",
            "throw_windup", "throw", "throw_burst", "needle_spin_tell", "needle_spin_tuck", "intro"
        )

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
            MoonScythe::class pairTo dmgNeg(8),
            ReactorManProjectile::class pairTo dmgNeg(6),
            SmallIceCube::class pairTo dmgNeg(2),
            MagmaWave::class pairTo dmgNeg(2),
            Fireball::class pairTo dmgNeg(2),
            PreciousGem::class pairTo dmgNeg(2),
            Axe::class pairTo dmgNeg(2),
            Needle::class pairTo dmgNeg(2),
            SlashWave::class pairTo dmgNeg(2),
            IceBombExplosion::class pairTo dmgNeg(2),
            MagmaFlame::class pairTo dmgNeg(2),
            ReactorExplosion::class pairTo dmgNeg(2)
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    internal enum class DesertManState {
        INIT, STAND, JUMP, WALL_SLIDE, DANCE, PUNCH, TORNADO, BURROW, BURROW_TRAVEL, BURROW_TELL, ERUPT,
        NEEDLE_SPIN, THROW
    }

    private enum class PunchPhase { WIND_UP, EXTEND, HOLD, RETRACT }

    private enum class ThrowPhase { WIND_UP, THROW }

    override val damageNegotiator: IDamageNegotiator = object : BossDamageNegotiator() {
        override fun get(damager: IDamagerEntity) = DAMAGE[damager::class]?.get(damager) ?: super.get(damager)
    }
    override val healthPhases = 2
    override val healthBarRegions = gdxArrayOf(ConstKeys.STANDARD, BossBitsBar.PHASE_2_BITS)

    override lateinit var facing: Facing
    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = body.getCenter()
    override var lightSourceRadius = LIGHT_SOURCE_RADIUS
    override var lightSourceRadiance = LIGHT_SOURCE_RADIANCE

    private val timers = OrderedMap<String, Timer>()

    private lateinit var stateMachine: StateMachine<DesertManState>
    internal val currentState: DesertManState
        get() = stateMachine.getCurrentElement()
    private var previousAttackState: DesertManState? = null

    private val tuning: DesertManTuning
        get() = DesertManTuning.of(services.gameState.hardMode, healthPhase)
    private var attackTuning = DesertManTuning.NORMAL

    private val leftWallBounds = GameRectangle()
    private val rightWallBounds = GameRectangle()

    private var punchPhase = PunchPhase.WIND_UP
    private var punchLength = 0f
    private val armFixtures = Array<Fixture>()
    private val fistFixtures = Array<Fixture>()
    private var punchReach = 0f
    private var punchHitsWall = false

    private var danceFlash = false

    private var needleSpinTelling = false
    private var needleSpinStarted = false
    private var needleSpinning = false
    private var needleSpinBurstsFired = 0
    private var needleSpinVelocityX = 0f

    private var throwPhase = ThrowPhase.WIND_UP
    private var throwVolleysLeft = 0
    private var throwVolley = 0
    private val throwPoint = Vector2()

    private val phaseFlashTimer = Timer(PHASE_FLASH_DUR)
    private var phaseFlashPending = false

    private var tornadoTurnsLeft = 0

    private var burrowX = 0f
    private var burrowY = 0f
    private var burrowTargetX = 0f
    private val burrowLightCenter = Vector2()
    private val burrowSurface = Vector2()

    private var inDarkness = false

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_2.source)
            REGION_KEYS.forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        buildTimers()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        stateMachine.reset()
        timers.forEach { t -> t.value.reset() }

        facing = if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        previousAttackState = null
        attackTuning = tuning
        punchPhase = PunchPhase.WIND_UP
        punchLength = 0f
        punchReach = 0f
        punchHitsWall = false
        danceFlash = false
        throwPhase = ThrowPhase.WIND_UP
        throwVolleysLeft = 0
        throwVolley = 0
        phaseFlashTimer.setToEnd()
        phaseFlashPending = false
        tornadoTurnsLeft = 0
        needleSpinTelling = false
        needleSpinStarted = false
        needleSpinning = false
        needleSpinBurstsFired = 0
        needleSpinVelocityX = 0f
        burrowX = 0f
        burrowY = 0f
        burrowTargetX = 0f

        body.physics.gravityOn = true
        body.physics.applyFrictionX = true
        body.physics.velocity.setZero()
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y

        leftWallBounds.set(
            spawnProps.get("${ConstKeys.LEFT}_${ConstKeys.WALL}", RectangleMapObject::class)!!.rectangle
        )

        rightWallBounds.set(
            spawnProps.get("${ConstKeys.RIGHT}_${ConstKeys.WALL}", RectangleMapObject::class)!!.rectangle
        )

        inDarkness = spawnProps.getOrDefault(ConstKeys.DARKNESS, false, Boolean::class)
        if (inDarkness) lightSourceKeys.addAll(
            spawnProps.get("${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}", String::class)!!
                .replace("\\s+", "")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.toInt() }
                .toObjectSet()
        )
    }

    override fun isReady(delta: Float) = timers[ConstKeys.INIT].isFinished()

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        lightSourceKeys.clear()
    }

    override fun canDamage(damageable: IDamageableEntity) = !isUnderSand() && super.canDamage(damageable)

    override fun canBeDamagedBy(damager: IDamagerEntity) = !isUnderSand() && super.canBeDamagedBy(damager)

    override fun onHealthPhaseStart(phase: Int) {
        GameLogger.debug(TAG, "onHealthPhaseStart(): phase=$phase")
        shakeCamera(PHASE_SHAKE_DUR)
        requestToPlaySound(SoundAsset.QUAKE_SOUND, loop = false)
        phaseFlashPending = true
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (bossComponent.phase == BossPhase.INTRO) return@add

            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            if (phaseFlashPending && currentState == DesertManState.STAND) {
                phaseFlashPending = false
                phaseFlashTimer.reset()
            }

            when (currentState) {
                DesertManState.INIT -> {
                    if (body.isSensing(BodySense.FEET_ON_SAND)) {
                        val timer = timers["init"]
                        timer.update(delta)
                        if (timer.isFinished()) stateMachine.next()
                    }
                }
                DesertManState.DANCE -> {
                    updateFacing()

                    if (danceFlash) {
                        val danceNeedlesTimer = timers["dance_needles"]
                        danceNeedlesTimer.update(delta)

                        if (danceNeedlesTimer.isFinished()) {
                            spawnNeedles()
                            danceNeedlesTimer.reset()
                        }

                        if (inDarkness) LightSourceUtils.sendLightSourceEvent(this)
                    }

                    val danceTimer = timers["dance"]
                    danceTimer.update(delta)
                    if (danceTimer.isFinished()) stateMachine.next()
                }
                DesertManState.STAND -> {
                    updateFacing()

                    if (!phaseFlashTimer.isFinished()) {
                        phaseFlashTimer.update(delta)
                        return@add
                    }

                    if (!body.isSensing(BodySense.FEET_ON_SAND)) return@add

                    val timer = timers["stand"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }
                DesertManState.PUNCH -> {
                    body.physics.velocity.x = 0f
                    updatePunch(delta)
                    updatePunchFixtures()
                }
                DesertManState.THROW -> {
                    body.physics.velocity.x = 0f
                    updateThrow(delta)
                }
                DesertManState.NEEDLE_SPIN -> {
                    updateFacing()
                    updateNeedleSpin(delta)
                    body.physics.velocity.x =
                        if (needleSpinTelling || needleSpinStarted) 0f else needleSpinVelocityX
                }
                DesertManState.JUMP -> {
                    facing = if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                    if (shouldFinishJumping()) stateMachine.next()
                }
                DesertManState.WALL_SLIDE -> {
                    body.physics.velocity.x = 0f

                    if (shouldGoToStandState()) stateMachine.next()

                    val timer = timers["wall_slide"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }
                DesertManState.TORNADO -> {
                    val tornadoSandTimer = timers["tornado_sand"]
                    tornadoSandTimer.update(delta)
                    if (tornadoSandTimer.isFinished()) {
                        spawnSandSplash(body.getCenter().x, body.getY())
                        tornadoSandTimer.reset()
                    }

                    val tornadoStartTimer = timers["tornado_start"]
                    if (!tornadoStartTimer.isFinished()) {
                        updateFacing()
                        body.physics.velocity.x = 0f
                        tornadoStartTimer.update(delta)
                        return@add
                    }

                    if (isHittingAgainstLeftWall() || isHittingAgainstRightWall()) {
                        if (tornadoTurnsLeft > 0) turnTornado() else stateMachine.next()
                        return@add
                    }

                    if (tornadoTurnsLeft > 0 && isTargetBehind()) turnTornado()

                    body.physics.velocity.x += TORNADO_IMPULSE_X * ConstVals.PPM * facing.value * delta

                    val tornadoTimer = timers["tornado"]
                    tornadoTimer.update(delta)
                    if (tornadoTimer.isFinished()) stateMachine.next()
                }
                DesertManState.BURROW -> {
                    body.physics.velocity.setZero()
                    updateBurrowSand(delta, burrowX)

                    val timer = timers["burrow"]
                    timer.update(delta)
                    body.setY(burrowY - timer.getRatio() * BURROW_SINK_DEPTH * ConstVals.PPM)
                    if (timer.isFinished()) stateMachine.next()
                }
                DesertManState.BURROW_TRAVEL -> {
                    body.physics.velocity.setZero()

                    val step = attackTuning.burrowTrailSpeed * ConstVals.PPM * delta
                    val distance = burrowTargetX - burrowX
                    val arrived = abs(distance) <= step
                    burrowX = if (arrived) burrowTargetX else burrowX + step * sign(distance)
                    updateBurrowSand(delta, burrowX)
                    lightBurrowSpot()

                    if (arrived) stateMachine.next()
                }
                DesertManState.BURROW_TELL -> {
                    body.physics.velocity.setZero()
                    updateBurrowSand(delta, burrowX + services.random.nextFloat(-BURROW_TELL_SPREAD, BURROW_TELL_SPREAD) * ConstVals.PPM)
                    lightBurrowSpot()

                    val timer = timers["burrow_tell"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }
                DesertManState.ERUPT -> {
                    if (inDarkness) LightSourceUtils.sendLightSourceEvent(this)

                    val riseTimer = timers["erupt_rise"]
                    if (!riseTimer.isFinished()) {
                        body.physics.velocity.setZero()
                        body.setY(burrowY)
                        riseTimer.update(delta)
                        if (riseTimer.isFinished()) launchEruption()
                        return@add
                    }

                    if (shouldGoToStandState()) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP_X * ConstVals.PPM, VEL_CLAMP_Y * ConstVals.PPM)
        body.physics.receiveFrictionX = false
        body.putProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SAND}", false)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        feetFixture.setReceiver(Receivers.STICK_TO_BLOCK) { _, _ -> false }
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        val bodyFixture =
            Fixture(body, FixtureType.BODY, GameRectangle().setWidth(BODY_WIDTH * ConstVals.PPM))
        bodyFixture.attachedToBody = false
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setWidth(BODY_WIDTH * ConstVals.PPM))
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setWidth(BODY_WIDTH * ConstVals.PPM))
        damageableFixture.attachedToBody = false
        body.addFixture(damageableFixture)

        val armDamageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setHeight(ARM_HEIGHT * ConstVals.PPM))
        armDamageableFixture.attachedToBody = false
        body.addFixture(armDamageableFixture)
        debugShapes.add { if (isPunching()) armDamageableFixture.getShape() else null }

        val armDamagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(ARM_HEIGHT * ConstVals.PPM))
        armDamagerFixture.attachedToBody = false
        body.addFixture(armDamagerFixture)
        debugShapes.add { if (isPunching()) armDamagerFixture.getShape() else null }

        val fistDamageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        fistDamageableFixture.attachedToBody = false
        body.addFixture(fistDamageableFixture)

        val fistDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        fistDamagerFixture.attachedToBody = false
        body.addFixture(fistDamagerFixture)
        debugShapes.add { if (isLongPunching()) fistDamagerFixture.getShape() else null }

        val dynamicHeightFixtures = gdxArrayOf(bodyFixture, damagerFixture, damageableFixture)
        armFixtures.addAll(armDamageableFixture, armDamagerFixture)
        fistFixtures.addAll(fistDamageableFixture, fistDamagerFixture)
        val terrainFixtures = gdxArrayOf(feetFixture, headFixture, leftFixture, rightFixture)
        body.preProcess.put(ConstKeys.DEFAULT) preProcess@{
            if (defeated) return@preProcess

            val underSand = isUnderSand()
            terrainFixtures.forEach { it.setActive(!underSand) }
            body.physics.collisionOn = !underSand
            val height = if (isSpinning()) TORNADO_HEIGHT else BODY_HEIGHT
            dynamicHeightFixtures.forEach {
                it.setActive(!underSand)
                val shape = it.rawShape as GameRectangle
                shape.setHeight(height * ConstVals.PPM)
                shape.setBottomCenterToPoint(body.getPositionPoint(Position.BOTTOM_CENTER))
            }

            updatePunchFixtures()

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.gravity.y = if (currentState == DesertManState.WALL_SLIDE) 0f else GRAVITY * ConstVals.PPM

            when {
                underSand -> body.physics.gravityOn = false
                body.isSensing(BodySense.FEET_ON_SAND) -> {
                    body.physics.gravityOn = false

                    body.physics.defaultFrictionOnSelf.x = when (currentState) {
                        DesertManState.TORNADO -> DEFAULT_FRICTION_X
                        else -> SAND_FRICTION_X
                    }

                    if (body.physics.velocity.y < 0f) body.physics.velocity.y = 0f
                }
                else -> {
                    body.physics.gravityOn = true
                    body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprites = OrderedMap<Any, GameSprite>()
        val updateFunctions = ObjectMap<Any, UpdateFunction<GameSprite>>()

        val mainSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        mainSprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        sprites.put(ConstKeys.MAIN, mainSprite)
        updateFunctions.put(ConstKeys.MAIN) { _, sprite ->
            val anchor = if (currentState == DesertManState.BURROW) burrowSurface.set(burrowX, burrowY)
            else body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(anchor, Position.BOTTOM_CENTER)
            sprite.translate(punchTrembleX(), SPRITE_Y_OFFSET * ConstVals.PPM)
            val flipX =
                if (currentState == DesertManState.WALL_SLIDE) body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                else isFacing(Facing.RIGHT)
            sprite.setFlip(flipX, false)
            sprite.hidden = damageBlink || services.levelState.roomTransition ||
                currentState == DesertManState.BURROW_TRAVEL || currentState == DesertManState.BURROW_TELL
        }

        val armRegion = regions["punch_arm"]
        val armSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprites.put(ConstKeys.ARM, armSprite)
        updateFunctions.put(ConstKeys.ARM) { _, sprite ->
            val artWidth = ceil((punchLength - PUNCH_FIST_JOIN * ART_PX) / ART_PX).toInt()
                .coerceIn(0, armRegion.regionWidth)
            sprite.hidden = isPunchPartHidden() || artWidth == 0
            if (sprite.hidden) return@put
            sprite.setRegion(armRegion)
            sprite.setRegion(armRegion.regionX, armRegion.regionY, artWidth, armRegion.regionHeight)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            val width = artWidth * ART_PX
            setPunchSpriteBounds(sprite, punchLength - width, width, PUNCH_ARM_BOTTOM, armRegion.regionHeight)
        }

        val fistRegion = regions["punch_fist"]
        val fistFrames = gdxArrayOf(
            TextureRegion(fistRegion, 0, 0, PUNCH_FIST_WIDTH, PUNCH_FIST_HEIGHT),
            TextureRegion(fistRegion, 0, PUNCH_FIST_HEIGHT, PUNCH_FIST_WIDTH, PUNCH_FIST_HEIGHT)
        )
        val fistSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        sprites.put(ConstKeys.HAND, fistSprite)
        updateFunctions.put(ConstKeys.HAND) { _, sprite ->
            sprite.hidden = isPunchPartHidden()
            if (sprite.hidden) return@put
            sprite.setRegion(fistFrames[if (isPunchFistFlashing()) 1 else 0])
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            setPunchSpriteBounds(sprite, 0f, PUNCH_FIST_WIDTH * ART_PX, PUNCH_FIST_BOTTOM, PUNCH_FIST_HEIGHT)
        }

        val speedRegion = regions["punch_speed"]
        val speedFrames = gdxArrayOf(
            TextureRegion(speedRegion, 0, 0, PUNCH_SPEED_WIDTH, PUNCH_SPEED_HEIGHT),
            TextureRegion(speedRegion, 0, PUNCH_SPEED_HEIGHT, PUNCH_SPEED_WIDTH, PUNCH_SPEED_HEIGHT)
        )
        val speedSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        sprites.put(ConstKeys.SPEED, speedSprite)
        updateFunctions.put(ConstKeys.SPEED) { _, sprite ->
            val timer = timers["punch"]
            sprite.hidden = isPunchPartHidden() || punchPhase != PunchPhase.EXTEND ||
                timer.getRatio() >= PUNCH_SPEED_LINES_RATIO
            if (sprite.hidden) return@put
            sprite.setRegion(speedFrames[(timer.time / PUNCH_SPEED_LINES_FRAME_DUR).toInt() % 2])
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            setPunchSpriteBounds(
                sprite,
                PUNCH_SPEED_FROM_TIP * ART_PX,
                PUNCH_SPEED_WIDTH * ART_PX,
                PUNCH_SPEED_BOTTOM,
                PUNCH_SPEED_HEIGHT
            )
        }

        val burstRegion = regions["throw_burst"]
        val burstFrames = Array<TextureRegion>()
        for (i in 0 until THROW_BURST_FRAMES) burstFrames.add(
            TextureRegion(burstRegion, 0, i * THROW_BURST_SIZE, THROW_BURST_SIZE, THROW_BURST_SIZE)
        )
        val burstSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        sprites.put(ConstKeys.BURST, burstSprite)
        updateFunctions.put(ConstKeys.BURST) { _, sprite ->
            val timer = timers["throw_burst"]
            sprite.hidden = defeated || currentState != DesertManState.THROW || timer.isFinished() ||
                damageBlink || services.levelState.roomTransition
            if (sprite.hidden) return@put
            val frame = (timer.time / THROW_BURST_FRAME_DUR).toInt().coerceAtMost(THROW_BURST_FRAMES - 1)
            sprite.setRegion(burstFrames[frame])
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            val size = THROW_BURST_SIZE * ART_PX
            val center = throwArtPoint(THROW_BURST_X, THROW_FIST_Y)
            sprite.setBounds(center.x - size / 2f, center.y - size / 2f, size, size)
        }

        return SpritesComponent(sprites, updateFunctions)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val mainSprite = sprites["main"]
        val mainSpriteKeySupplier: (String?) -> String? = {
            if (defeated) "defeated"
            else when (currentState) {
                DesertManState.INIT -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "intro" else "jump"
                DesertManState.STAND -> if (!phaseFlashTimer.isFinished()) "dance_flash" else "stand"
                DesertManState.DANCE -> if (danceFlash) "dance_flash" else "dance"
                DesertManState.NEEDLE_SPIN -> when {
                    needleSpinTelling -> "needle_spin_tell"
                    !needleSpinStarted -> "needle_spin_tuck"
                    needleSpinning -> "needle_spin"
                    else -> "jump"
                }
                DesertManState.PUNCH -> "punch"
                DesertManState.THROW -> if (throwPhase == ThrowPhase.WIND_UP) "throw_windup" else "throw"
                DesertManState.TORNADO -> when {
                    timers["tornado_start"].time < TORNADO_RISE_DUR -> "tornado_start"
                    !timers["tornado_start"].isFinished() -> "tornado"
                    else -> "tornado_punch"
                }
                DesertManState.BURROW, DesertManState.BURROW_TRAVEL, DesertManState.BURROW_TELL -> "burrow"
                DesertManState.ERUPT -> when {
                    !timers["erupt_rise"].isFinished() -> "erupt"
                    body.physics.velocity.y > 0f -> "tornado_punch"
                    else -> "jump"
                }

                else -> currentState.name.lowercase()
            }
        }
        val mainSpriteAnimations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(0.5f, 0.1f), true),
            "jump" pairTo Animation(regions["jump"]),
            "wall_slide" pairTo Animation(regions["wall_slide"]),
            "dance" pairTo Animation(regions["dance"], 2, 1, 0.1f, true),
            "intro" pairTo Animation(regions["intro"], 1, INTRO_FRAME_DURS.size, INTRO_FRAME_DURS, false),
            "dance_flash" pairTo Animation(regions["dance_flash"], 2, 2, 0.05f, true),
            "tornado" pairTo Animation(regions["tornado"], 2, 1, 0.1f, true),
            "tornado_punch" pairTo Animation(regions["tornado_punch"], 2, 2, 0.05f, true),
            "tornado_start" pairTo Animation(regions["tornado_start"], 2, 1, TORNADO_RISE_DUR / 2f, false),
            "burrow" pairTo Animation(
                regions["burrow"], 1, BURROW_FRAMES, DesertManTuning.BURROW_SINK_DUR / BURROW_FRAMES, false
            ),
            "erupt" pairTo Animation(regions["erupt"], 1, ERUPT_FRAMES, ERUPT_RISE_DUR / ERUPT_FRAMES, false),
            "punch" pairTo Animation(regions["punch_body"]),
            "needle_spin_tell" pairTo Animation(regions["needle_spin_tell"], 2, 1, NEEDLE_SPIN_TELL_FLASH_DUR, true),
            "needle_spin_tuck" pairTo Animation(regions["needle_spin_tuck"]),
            "needle_spin" pairTo Animation(regions["needle_spin"], 1, NEEDLE_SPIN_FRAMES, NEEDLE_SPIN_FRAME_DUR, true),
            "throw_windup" pairTo Animation(regions["throw_windup"], 2, 1, THROW_FLASH_FRAME_DUR, true),
            "throw" pairTo Animation(regions["throw"]),
            "defeated" pairTo Animation(regions["defeated"], 3, 1, 0.1f, true)
        )
        val mainSpriteAnimator = Animator(mainSpriteKeySupplier, mainSpriteAnimations)
        animators.add({ mainSprite } pairTo mainSpriteAnimator)

        return AnimationsComponent(animators)
    }

    private fun buildStateMachine(): StateMachine<DesertManState> {
        val builder = StateMachineBuilder<DesertManState>()
        DesertManState.entries.forEach { t -> builder.state(t.name, t) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(DesertManState.INIT.name)
            .transition(DesertManState.INIT.name, DesertManState.STAND.name) { bossComponent.phase != BossPhase.ENTRANCE }
            .transition(DesertManState.STAND.name, DesertManState.BURROW.name) {
                previousAttackState != DesertManState.BURROW && services.random.nextFloat(0f, 1f) < tuning.burrowChance
            }
            .transition(DesertManState.STAND.name, DesertManState.PUNCH.name) {
                previousAttackState != DesertManState.PUNCH &&
                    isTargetInPunchXRange() &&
                    isMegamanInPunchYRange() &&
                    services.random.nextFloat(0f, 1f) < tuning.punchChance
            }
            .transition(DesertManState.STAND.name, DesertManState.TORNADO.name) {
                previousAttackState != DesertManState.TORNADO && isMegamanInTornadoYRange() &&
                    !isHittingAgainstLeftWall() && !isHittingAgainstRightWall() &&
                    services.random.nextFloat(0f, 1f) < tuning.tornadoChance
            }
            .transition(DesertManState.STAND.name, DesertManState.NEEDLE_SPIN.name) {
                previousAttackState != DesertManState.NEEDLE_SPIN && services.random.nextFloat(0f, 1f) < tuning.needleSpinChance
            }
            .transition(DesertManState.STAND.name, DesertManState.THROW.name) {
                previousAttackState != DesertManState.THROW && isTargetInThrowXRange() &&
                    services.random.nextFloat(0f, 1f) < tuning.throwChance
            }
            .transition(DesertManState.STAND.name, DesertManState.DANCE.name) {
                previousAttackState != DesertManState.DANCE && services.random.nextFloat(0f, 1f) < tuning.danceChance
            }
            .transition(DesertManState.STAND.name, DesertManState.JUMP.name) { true }
            .transition(DesertManState.BURROW.name, DesertManState.BURROW_TRAVEL.name) { true }
            .transition(DesertManState.BURROW_TRAVEL.name, DesertManState.BURROW_TELL.name) { true }
            .transition(DesertManState.BURROW_TELL.name, DesertManState.ERUPT.name) { true }
            .transition(DesertManState.ERUPT.name, DesertManState.STAND.name) { true }
            .transition(DesertManState.PUNCH.name, DesertManState.STAND.name) { true }
            .transition(DesertManState.JUMP.name, DesertManState.STAND.name) { shouldGoToStandState() }
            .transition(DesertManState.JUMP.name, DesertManState.WALL_SLIDE.name) { targetPlayer.existenceState.liveAfterUpdate && isWallSliding() }
            .transition(DesertManState.WALL_SLIDE.name, DesertManState.STAND.name) { shouldGoToStandState() }
            .transition(DesertManState.WALL_SLIDE.name, DesertManState.JUMP.name) { true }
            .transition(DesertManState.TORNADO.name, DesertManState.STAND.name) { true }
            .transition(DesertManState.DANCE.name, DesertManState.STAND.name) { true }
            .transition(DesertManState.NEEDLE_SPIN.name, DesertManState.STAND.name) { true }
            .transition(DesertManState.THROW.name, DesertManState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: DesertManState, previous: DesertManState) {
        GameLogger.debug(TAG, "onChangeState: current=$current, previous=$previous")
        attackTuning = tuning

        when (previous) {
            DesertManState.STAND -> previousAttackState = current
            DesertManState.INIT -> {
                body.physics.gravityOn = true
                GameLogger.debug(
                    TAG, "onChangeState(): ending INIT state: " +
                        "body gravity turned on"
                )
            }

            DesertManState.JUMP -> {
                body.physics.applyFrictionX = true
                GameLogger.debug(
                    TAG, "onChangeState(): ending JUMP state: " +
                        "body.physics.applyFrictionX=${body.physics.applyFrictionX}"
                )
            }

            DesertManState.ERUPT -> body.physics.applyFrictionX = true

            DesertManState.WALL_SLIDE -> {
                body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
                GameLogger.debug(
                    TAG, "onChangeState(): ending WALL_SLIDE state: " +
                        "body.physics.defaultFrictionOnSelf.y=${body.physics.defaultFrictionOnSelf.y}"
                )
            }

            else -> GameLogger.debug(TAG, "onChangeState: ending $previous state: nothing to do")
        }

        when (current) {
            DesertManState.STAND -> {
                timers["stand"].resetDuration(attackTuning.standDur)
                GameLogger.debug(
                    TAG, "onChangeState(): setting up STAND state: " +
                        "stand timer reset"
                )
            }

            DesertManState.JUMP -> {
                body.physics.applyFrictionX = false

                val impulse = jump(targetPlayer.body.getCenter())
                if (previous == DesertManState.WALL_SLIDE) {
                    var impulseX = WALL_JUMP_IMPULSE_X * ConstVals.PPM
                    if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) impulseX *= -1f
                    body.physics.velocity.x = impulseX
                }

                facing = if (impulse.x < 0f) Facing.LEFT else Facing.RIGHT

                GameLogger.debug(
                    TAG, "onChangeState(): setting up JUMP state: " +
                        "body.physics.applyFrictionX=${body.physics.applyFrictionX}, " +
                        "body.physics.velocity=${body.physics.velocity}, " +
                        "facing=$facing"
                )
            }

            DesertManState.WALL_SLIDE -> {
                timers["wall_slide"].reset()
                body.physics.defaultFrictionOnSelf.y = WALL_SLIDE_FRICTION_Y
                GameLogger.debug(
                    TAG, "onChangeState(): setting up WALL_SLIDE state: " +
                        "wall_slide timer reset, " +
                        "body.physics.defaultFrictionOnSelf.y=${body.physics.defaultFrictionOnSelf.y}"
                )
            }

            DesertManState.PUNCH -> {
                updateFacing()
                body.physics.velocity.x = 0f
                punchLength = PUNCH_REST_LENGTH * ART_PX
                punchReach = 0f
                punchHitsWall = false
                startPunchPhase(PunchPhase.WIND_UP, attackTuning.windUpDur)
                requestToPlaySound(SoundAsset.REV_SOUND, loop = false)
                GameLogger.debug(
                    TAG,
                    "onChangeState(): setting up PUNCH state: facing=$facing, wind-up=${attackTuning.windUpDur}"
                )
            }

            DesertManState.TORNADO -> {
                timers["tornado"].reset()
                timers["tornado_start"].resetDuration(attackTuning.tornadoSpinUpDur)
                timers["tornado_sand"].reset()
                tornadoTurnsLeft = attackTuning.tornadoTurns

                requestToPlaySound(SoundAsset.WIND_1_SOUND, loop = false)

                GameLogger.debug(
                    TAG,
                    "onChangeState(): setting up TORNADO state: " +
                        "tornado timer reset, " +
                        "tornado_punch timer reset, " +
                        "tornado_start timer reset, " +
                        "tornado_sand timer reset, " +
                        "play wind 1 sound"
                )
            }

            DesertManState.DANCE -> {
                danceFlash = false
                timers["dance"].reset()
                timers["dance_needles"].reset()
                GameLogger.debug(
                    TAG, "onChangeState(): setting up DANCE state: " +
                        "dance timer reset, " +
                        "dance_needles timer reset"
                )
            }

            DesertManState.THROW -> {
                updateFacing()
                body.physics.velocity.x = 0f
                throwVolleysLeft = attackTuning.throwVolleys
                throwVolley = 0
                timers["throw_burst"].setToEnd()
                startThrowWindUp(attackTuning.throwWindUpDur)
                GameLogger.debug(
                    TAG,
                    "onChangeState(): setting up THROW state: facing=$facing, volleys=$throwVolleysLeft"
                )
            }

            DesertManState.NEEDLE_SPIN -> {
                updateFacing()
                body.physics.velocity.setZero()
                needleSpinTelling = true
                timers["needle_spin_tell"].resetDuration(attackTuning.needleSpinTellDur)
                requestToPlaySound(SoundAsset.REV_SOUND, loop = false)
                needleSpinStarted = false
                needleSpinning = false
                needleSpinBurstsFired = 0
                needleSpinVelocityX = 0f
                timers["needle_spin_end"].reset()
                GameLogger.debug(
                    TAG,
                    "onChangeState(): setting up NEEDLE_SPIN state: bursts=${attackTuning.needleSpinBursts}"
                )
            }

            DesertManState.BURROW -> {
                body.physics.velocity.setZero()
                burrowX = body.getCenter().x
                burrowY = body.getY()
                timers["burrow"].resetDuration(attackTuning.burrowSinkDur)
                timers["burrow_sand"].reset()
                spawnDustPuff(burrowX, burrowY)
                requestToPlaySound(SoundAsset.DIG_SOUND, loop = false)
                GameLogger.debug(TAG, "onChangeState(): setting up BURROW state: burrowX=$burrowX, burrowY=$burrowY")
            }

            DesertManState.BURROW_TRAVEL -> {
                burrowTargetX = clampToArena(targetPlayer.body.getCenter().x)
                GameLogger.debug(TAG, "onChangeState(): setting up BURROW_TRAVEL state: burrowTargetX=$burrowTargetX")
            }

            DesertManState.BURROW_TELL -> {
                timers["burrow_tell"].resetDuration(attackTuning.burrowTellDur)
                requestToPlaySound(SoundAsset.DIG_SOUND, loop = false)
                GameLogger.debug(TAG, "onChangeState(): setting up BURROW_TELL state")
            }

            DesertManState.ERUPT -> {
                body.setPosition(burrowX - body.getWidth() / 2f, burrowY)
                body.physics.velocity.setZero()
                facing = if (targetPlayer.body.getCenter().x < burrowX) Facing.LEFT else Facing.RIGHT
                timers["erupt_rise"].reset()
                spawnSandSplash(burrowX, burrowY)
                spawnDustPuff(burrowX, burrowY)
                requestToPlaySound(SoundAsset.DIG_SOUND, loop = false)
                GameLogger.debug(TAG, "onChangeState(): setting up ERUPT state: x=$burrowX, facing=$facing")
            }

            else -> GameLogger.debug(TAG, "onChangeState(): setting up $current state: nothing to do")
        }
    }

    private fun spawnNeedles() {
        for (i in 0 until NEEDLE_ANGLES.size) {
            val angle = NEEDLE_ANGLES[i]
            val xOffset = NEEDLE_X_OFFSETS[i]
            val position = body.getCenter().add(xOffset * ConstVals.PPM, NEEDLE_Y_OFFSET * ConstVals.PPM)
            val impulse = Vector2(0f, NEEDLE_IMPULSE * ConstVals.PPM).rotateDeg(angle)

            val needle = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.NEEDLE)!!
            needle.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.GRAVITY pairTo NEEDLE_GRAV * ConstVals.PPM
                )
            )
        }

        requestToPlaySound(SoundAsset.THUMP_SOUND, false)
    }

    private fun updateThrow(delta: Float) {
        timers["throw_burst"].update(delta)
        if (throwPhase == ThrowPhase.WIND_UP) updateFacing()

        val timer = timers["throw"]
        timer.update(delta)
        if (!timer.isFinished()) return

        when (throwPhase) {
            ThrowPhase.WIND_UP -> releaseThrow()
            ThrowPhase.THROW -> if (throwVolleysLeft > 0) startThrowWindUp(THROW_REWIND_DUR) else stateMachine.next()
        }
    }

    private fun startThrowWindUp(duration: Float) {
        throwPhase = ThrowPhase.WIND_UP
        timers["throw"].resetDuration(duration)
        requestToPlaySound(SoundAsset.REV_SOUND, loop = false)
        GameLogger.debug(TAG, "startThrowWindUp(): duration=$duration, volleysLeft=$throwVolleysLeft")
    }

    private fun releaseThrow() {
        throwPhase = ThrowPhase.THROW
        timers["throw"].resetDuration(attackTuning.throwHoldDur)
        timers["throw_burst"].reset()
        spawnThrowNeedles()
        throwVolleysLeft--
        throwVolley++
        requestToPlaySound(SoundAsset.BURST_SOUND, loop = false)
        GameLogger.debug(TAG, "releaseThrow(): volley=$throwVolley, volleysLeft=$throwVolleysLeft")
    }

    private fun spawnThrowNeedles() {
        val origin = Vector2(throwArtPoint(THROW_FIST_X, THROW_FIST_Y))
        val forward = if (isFacing(Facing.LEFT)) 180f else 0f
        val toTarget = Vector2(targetPlayer.body.getCenter()).sub(origin).angleDeg()
        val offset = ((toTarget - forward + 540f) % 360f) - 180f
        val aim = forward + offset.coerceIn(-THROW_MAX_AIM_ANGLE, THROW_MAX_AIM_ANGLE)
        val spread = attackTuning.throwNeedleSpread
        val count = if (throwVolley % 2 == 1) attackTuning.throwNeedles + 1 else attackTuning.throwNeedles
        for (i in 0 until count) {
            val angle = aim + (i - (count - 1) / 2f) * spread
            val impulse = Vector2(attackTuning.throwNeedleSpeed * ConstVals.PPM, 0f).rotateDeg(angle)
            MegaEntityFactory.fetch(Needle::class)!!.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo Vector2(origin),
                    ConstKeys.IMPULSE pairTo impulse
                )
            )
        }
    }

    private fun throwArtPoint(artX: Int, artBottom: Int): Vector2 = throwPoint.set(
        body.getCenter().x + facing.value * (ART_SIZE / 2f - artX) * ART_PX,
        punchPartY(artBottom)
    )

    private fun updateNeedleSpin(delta: Float) {
        if (needleSpinTelling) {
            val tellTimer = timers["needle_spin_tell"]
            tellTimer.update(delta)
            if (!tellTimer.isFinished()) return
            needleSpinTelling = false
            needleSpinVelocityX = needleSpinApproachVelocityX()
            body.physics.velocity.set(needleSpinVelocityX, NEEDLE_SPIN_JUMP_IMPULSE_Y * ConstVals.PPM)
            return
        }

        if (!needleSpinStarted) {
            if (body.physics.velocity.y > 0f) return
            needleSpinStarted = true
            needleSpinning = true
            fireNeedleSpinBurst()
            return
        }

        if (needleSpinBurstsFired < attackTuning.needleSpinBursts) {
            val burstTimer = timers["needle_spin"]
            burstTimer.update(delta)
            if (burstTimer.isFinished()) fireNeedleSpinBurst()
            return
        }

        if (needleSpinning) {
            val endTimer = timers["needle_spin_end"]
            endTimer.update(delta)
            if (endTimer.isFinished()) needleSpinning = false
            return
        }

        if (shouldGoToStandState()) stateMachine.next()
    }

    private fun needleSpinApproachVelocityX(): Float {
        val x = body.getCenter().x
        val dx = targetPlayer.body.getCenter().x - x
        val travel = (abs(dx) - NEEDLE_SPIN_KEEP_DISTANCE * ConstVals.PPM)
            .coerceIn(0f, NEEDLE_SPIN_MAX_APPROACH * ConstVals.PPM)
        val landX = clampToArena(x + if (dx < 0f) -travel else travel)
        val riseDur = NEEDLE_SPIN_JUMP_IMPULSE_Y / -GRAVITY
        return (landX - x) / riseDur
    }

    private fun fireNeedleSpinBurst() {
        val angles = NEEDLE_SPIN_ANGLES[needleSpinBurstsFired % NEEDLE_SPIN_ANGLES.size]
        val center = body.getCenter()
        for (i in 0 until angles.size) {
            val position = Vector2(center.x, center.y + NEEDLE_SPIN_Y_OFFSET * ConstVals.PPM)
            val impulse = Vector2(0f, NEEDLE_IMPULSE * ConstVals.PPM).rotateDeg(angles[i])
            val needle = MegaEntityFactory.fetch(Needle::class)!!
            needle.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.GRAVITY pairTo NEEDLE_GRAV * ConstVals.PPM
                )
            )
        }

        needleSpinBurstsFired++
        body.physics.velocity.y = NEEDLE_SPIN_HOVER_IMPULSE_Y * ConstVals.PPM
        timers["needle_spin"].resetDuration(attackTuning.needleSpinBurstDelay)
        requestToPlaySound(SoundAsset.WHOOSH_SOUND, loop = false)
        GameLogger.debug(TAG, "fireNeedleSpinBurst(): burst=$needleSpinBurstsFired, angles=$angles")
    }

    private fun launchEruption() {
        val center = body.getCenter()
        val target = targetPlayer.body.getCenter()
        val gravity = -GRAVITY * ConstVals.PPM
        val rise = max(0f, target.y + ERUPT_OVERSHOOT_Y * ConstVals.PPM - center.y)
        val velocityY = max(attackTuning.eruptImpulseY * ConstVals.PPM, sqrt(2f * gravity * rise))
        val maxSpeedX = ERUPT_MAX_SPEED_X * ConstVals.PPM
        val velocityX = ((clampToArena(target.x) - center.x) / (velocityY / gravity)).coerceIn(-maxSpeedX, maxSpeedX)
        body.physics.applyFrictionX = false
        body.physics.velocity.set(velocityX, velocityY)
        spawnSandSplash(burrowX, burrowY)
        requestToPlaySound(SoundAsset.WIND_1_SOUND, loop = false)
        GameLogger.debug(TAG, "launchEruption(): x=$burrowX")
    }

    private fun spawnDustPuff(x: Float, y: Float) {
        val puff = MegaEntityFactory.fetch(DustPuff::class)!!
        puff.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo Vector2(x, y)))
    }

    private fun spawnSandSplash(x: Float, y: Float) {
        val splash = MegaEntityFactory.fetch(Splash::class)!!
        splash.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(x, y),
                ConstKeys.TYPE pairTo SplashType.SAND,
            )
        )
        GameLogger.debug(TAG, "spawnSandSplash(): x=$x, y=$y")
    }

    private fun updateBurrowSand(delta: Float, splashX: Float) {
        val timer = timers["burrow_sand"]
        timer.update(delta)
        if (!timer.isFinished()) return
        spawnSandSplash(splashX, burrowY)
        timer.reset()
    }

    private fun lightBurrowSpot() {
        if (!inDarkness) return
        burrowLightCenter.set(burrowX, burrowY + BURROW_LIGHT_Y_OFFSET * ConstVals.PPM)
        LightSourceUtils.sendLightSourceEvent(
            this, lightSourceKeys, burrowLightCenter, lightSourceRadiance, lightSourceRadius
        )
    }

    private fun clampToArena(x: Float): Float {
        val halfWidth = BODY_WIDTH * ConstVals.PPM / 2f
        return x.coerceIn(leftWallBounds.getMaxX() + halfWidth, rightWallBounds.getX() - halfWidth)
    }

    private fun turnTornado() {
        facing = facing.opposite()
        body.physics.velocity.x = TORNADO_TURN_SPEED * ConstVals.PPM * facing.value
        tornadoTurnsLeft--
        timers["tornado"].reset()
        requestToPlaySound(SoundAsset.WIND_1_SOUND, loop = false)
        GameLogger.debug(TAG, "turnTornado(): facing=$facing, tornadoTurnsLeft=$tornadoTurnsLeft")
    }

    private fun isTargetBehind() = when (facing) {
        Facing.LEFT -> targetPlayer.body.getX() > body.getMaxX() + TORNADO_TURN_MARGIN * ConstVals.PPM
        Facing.RIGHT -> targetPlayer.body.getMaxX() < body.getX() - TORNADO_TURN_MARGIN * ConstVals.PPM
    }

    private fun shakeCamera(duration: Float) = services.events.submitEvent(
        Event(
            EventType.SHAKE_CAM, props(
                ConstKeys.DURATION pairTo duration,
                ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
            )
        )
    )

    private fun jump(target: Vector2): Vector2 {
        val impulse = MegaUtilMethods.calculateJumpImpulse(body.getCenter(), target, JUMP_IMPULSE_Y * ConstVals.PPM)
        impulse.x = impulse.x.coerceIn(-JUMP_MAX_IMPULSE_X * ConstVals.PPM, JUMP_MAX_IMPULSE_X * ConstVals.PPM)
        body.physics.velocity.set(impulse.x, impulse.y)
        return impulse
    }

    private fun shouldFinishJumping() = isWallSliding() || shouldGoToStandState()

    private fun isHittingAgainstLeftWall() =
        body.physics.velocity.x <= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)

    private fun isHittingAgainstRightWall() =
        body.physics.velocity.x >= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)

    private fun isWallSliding() = body.physics.velocity.y < 0f && !body.isSensing(BodySense.FEET_ON_SAND) &&
        (isHittingAgainstLeftWall() || isHittingAgainstRightWall())

    private fun shouldGoToStandState() = body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_SAND)

    private fun isTargetInThrowXRange() =
        abs(targetPlayer.body.getCenter().x - body.getCenter().x) >= THROW_MIN_X * ConstVals.PPM

    private fun isTargetInPunchXRange() =
        abs(targetPlayer.body.getCenter().x - body.getCenter().x) <= PUNCH_X_THRESHOLD * ConstVals.PPM

    private fun isMegamanInPunchYRange() = abs(targetPlayer.body.getY() - body.getY()) <= PUNCH_Y_THRESHOLD * ConstVals.PPM

    private fun isLongPunching() = currentState == DesertManState.PUNCH && punchPhase != PunchPhase.WIND_UP

    private fun isPunching() = isLongPunching() || isTornadoPunching()

    private fun isUnderSand() = currentState == DesertManState.BURROW ||
        currentState == DesertManState.BURROW_TRAVEL || currentState == DesertManState.BURROW_TELL

    private fun isErupting() = currentState == DesertManState.ERUPT && body.physics.velocity.y > 0f

    private fun isSpinning() = currentState == DesertManState.TORNADO || isErupting()

    private fun isMegamanInTornadoYRange() =
        abs(targetPlayer.body.getY() - body.getY()) <= TORNADO_Y_THRESHOLD * ConstVals.PPM

    private fun isTornadoPunching() =
        (currentState == DesertManState.TORNADO && timers["tornado_start"].isFinished()) || isErupting()

    private fun updateFacing() {
        when {
            targetPlayer.body.getMaxX() < body.getX() -> facing = Facing.LEFT
            targetPlayer.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    private fun buildTimers() {
        timers.put("init", Timer(INIT_DUR))
        timers.put("stand", Timer(DesertManTuning.STAND_DUR))
        timers.put("punch", Timer(DesertManTuning.WIND_UP_DUR))
        timers.put("tornado", Timer(TORNADO_DUR))
        timers.put("tornado_start", Timer(DesertManTuning.TORNADO_SPIN_UP_DUR))
        timers.put("tornado_sand", Timer(TORNADO_SAND_DELAY))
        timers.put("burrow", Timer(DesertManTuning.BURROW_SINK_DUR))
        timers.put("burrow_sand", Timer(BURROW_SAND_DELAY))
        timers.put("burrow_tell", Timer(DesertManTuning.BURROW_TELL_DUR))
        timers.put("erupt_rise", Timer(ERUPT_RISE_DUR))
        timers.put("wall_slide", Timer(WALL_SLIDE_DUR))
        timers.put("dance", Timer(DANCE_DUR, TimeMarkedRunnable(DANCE_FLASH_DELAY) { danceFlash = true }))
        timers.put("dance_needles", Timer(DANCE_SPAWN_NEEDLES_DELAY))
        timers.put("needle_spin", Timer(DesertManTuning.NEEDLE_SPIN_BURST_DELAY))
        timers.put("needle_spin_end", Timer(NEEDLE_SPIN_END_DUR))
        timers.put("needle_spin_tell", Timer(DesertManTuning.NEEDLE_SPIN_TELL_DUR))
        timers.put("throw", Timer(DesertManTuning.THROW_WIND_UP_DUR))
        timers.put("throw_burst", Timer(THROW_BURST_DUR))
    }

    private fun updatePunch(delta: Float) {
        val timer = timers["punch"]
        timer.update(delta)
        val ratio = timer.getRatio()
        val minLength = (PUNCH_REST_LENGTH - PUNCH_PULL_BACK) * ART_PX
        when (punchPhase) {
            PunchPhase.WIND_UP -> {
                punchLength = (PUNCH_REST_LENGTH - PUNCH_PULL_BACK * Interpolation.pow2Out.apply(ratio)) * ART_PX
                if (timer.isFinished()) startPunchExtend(minLength)
            }
            PunchPhase.EXTEND -> {
                punchLength = minLength + (punchReach - minLength) * Interpolation.sineOut.apply(ratio)
                if (!timer.isFinished()) return
                if (punchHitsWall) onPunchHitWall()
                startPunchPhase(PunchPhase.HOLD, attackTuning.punchHoldDur)
            }
            PunchPhase.HOLD -> if (timer.isFinished()) startPunchPhase(
                PunchPhase.RETRACT, punchMoveDur(punchReach - minLength, PUNCH_RETRACT_SPEED)
            )
            PunchPhase.RETRACT -> {
                punchLength = punchReach - (punchReach - minLength) * Interpolation.sineIn.apply(ratio)
                if (timer.isFinished()) stateMachine.next()
            }
        }
    }

    private fun updatePunchFixtures() {
        armFixtures.forEach { t ->
            t.setActive(isPunching())
            if (!isPunching()) return@forEach
            val shape = t.rawShape as GameRectangle
            if (isLongPunching()) {
                setPunchPartBounds(shape, 0f, punchLength, PUNCH_ARM_BOTTOM, PUNCH_ARM_HEIGHT)
                return@forEach
            }
            shape.setSize(SHORT_PUNCH_WIDTH * ConstVals.PPM, ARM_HEIGHT * ConstVals.PPM)
            when {
                isFacing(Facing.LEFT) -> shape.setCenterRightToPoint(body.getPositionPoint(Position.TOP_LEFT))
                else -> shape.setCenterLeftToPoint(body.getPositionPoint(Position.TOP_RIGHT))
            }
        }
        fistFixtures.forEach { t ->
            t.setActive(isLongPunching())
            if (!isLongPunching()) return@forEach
            setPunchPartBounds(
                t.rawShape as GameRectangle,
                0f,
                PUNCH_FIST_HIT_WIDTH * ART_PX,
                PUNCH_FIST_BOTTOM,
                PUNCH_FIST_HIT_HEIGHT
            )
        }
    }

    private fun startPunchPhase(phase: PunchPhase, duration: Float) {
        punchPhase = phase
        timers["punch"].resetDuration(duration)
        GameLogger.debug(TAG, "startPunchPhase(): phase=$phase, duration=$duration, reach=$punchReach")
    }

    private fun startPunchExtend(minLength: Float) {
        val wallLimit = when (facing) {
            Facing.LEFT -> restShoulderX() - leftWallBounds.getMaxX()
            Facing.RIGHT -> rightWallBounds.getX() - restShoulderX()
        }
        val maxReach = PUNCH_MAX_REACH * ConstVals.PPM
        punchHitsWall = wallLimit <= maxReach
        punchReach = min(maxReach, wallLimit).coerceAtLeast(minLength)
        startPunchPhase(PunchPhase.EXTEND, punchMoveDur(punchReach - minLength, attackTuning.punchExtendSpeed))
        requestToPlaySound(SoundAsset.WHOOSH_SOUND, loop = false)
    }

    private fun punchMoveDur(distance: Float, averageSpeed: Float) = max(
        distance / (averageSpeed * ConstVals.PPM),
        distance * MathUtils.HALF_PI / (PUNCH_MAX_SPEED * ConstVals.PPM)
    )

    private fun onPunchHitWall() {
        spawnSandSplash(restShoulderX() + facing.value * punchReach, punchPartY(PUNCH_FIST_BOTTOM))
        shakeCamera(PUNCH_WALL_SHAKE_DUR)
        requestToPlaySound(SoundAsset.POUND_SOUND, loop = false)
    }

    private fun punchTrembleX(): Float {
        if (defeated) return 0f
        val timer = when {
            currentState == DesertManState.PUNCH && punchPhase == PunchPhase.WIND_UP -> timers["punch"]
            currentState == DesertManState.THROW && throwPhase == ThrowPhase.WIND_UP -> timers["throw"]
            else -> return 0f
        }
        val side = if ((timer.time / PUNCH_TREMBLE_INTERVAL).toInt() % 2 == 0) 1 else -1
        return side * PUNCH_TREMBLE_X * ART_PX
    }

    private fun isPunchFistFlashing() = punchPhase == PunchPhase.WIND_UP &&
        (timers["punch"].time / PUNCH_FLASH_INTERVAL).toInt() % 2 == 0

    private fun isPunchPartHidden() = defeated || currentState != DesertManState.PUNCH || damageBlink ||
        services.levelState.roomTransition

    private fun punchShoulderX() = restShoulderX() + punchTrembleX()

    private fun restShoulderX() = body.getCenter().x + facing.value * (ART_SIZE / 2f - PUNCH_SHOULDER_X) * ART_PX

    private fun punchPartY(artBottom: Int) = body.getY() + SPRITE_Y_OFFSET * ConstVals.PPM + artBottom * ART_PX

    private fun punchPartX(fromTip: Float, width: Float): Float {
        val tipX = punchShoulderX() + facing.value * punchLength
        return when (facing) {
            Facing.LEFT -> tipX + fromTip
            Facing.RIGHT -> tipX - fromTip - width
        }
    }

    private fun setPunchPartBounds(shape: GameRectangle, fromTip: Float, width: Float, artBottom: Int, artHeight: Int) {
        shape.set(punchPartX(fromTip, width), punchPartY(artBottom), width, artHeight * ART_PX)
    }

    private fun setPunchSpriteBounds(sprite: GameSprite, fromTip: Float, width: Float, artBottom: Int, artHeight: Int) {
        sprite.setBounds(punchPartX(fromTip, width), punchPartY(artBottom), width, artHeight * ART_PX)
    }
}
