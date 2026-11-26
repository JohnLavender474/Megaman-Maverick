package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.events.Event
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import com.megaman.maverick.game.entities.bosses.reactormonkey.ReactorMonkeyTuning
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.items.HeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
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
import com.megaman.maverick.game.entities.projectiles.ReactorMonkeyBall
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass
import com.badlogic.gdx.utils.FloatArray as GdxFloatArray
import com.badlogic.gdx.utils.IntArray as GdxIntArray

private class ReactorMonkeyEarPoints(val leftX: Float, val leftY: Float, val rightX: Float, val rightY: Float)

class ReactorMonkey(services: MegaServices) :
    AbstractBoss(services, dmgDuration = DEFAULT_MINI_BOSS_DMG_DURATION, size = Size.LARGE), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "ReactorMonkey"
        const val BALL_SPAWN_Y_KEY = "${ConstKeys.BALL}_${ConstKeys.SPAWN}_${ConstKeys.Y}"

        const val CHEST_BEAT_PASSES = 3

        private const val THROW_DUR = 0.3f
        private const val THROW_RELEASE_TIME = 0.2f

        private const val BALL_CATCH_RADIUS = 0.25f
        private const val BALL_IMPULSE_Y = 6f

        private const val HORIZONTAL_SCALAR = 1.25f
        private const val VERTICAL_SCALAR = 1f

        private const val DEFAULT_BALL_SPAWN_Y = 8f

        private const val SKIP_FLOOR_AHEAD = 2f

        private const val INTRO_DROP_SPEED = 10f
        private const val INTRO_DROP_GRAVITY = -22.5f
        private val INTRO_LAND_DURS = gdxArrayOf(0.06f, 0.08f, 0.12f)
        private val INTRO_ROAR_DURS = gdxArrayOf(0.12f, 0.08f, 0.3f, 0.3f, 0.12f, 0.12f)
        private val INTRO_LAND_DUR = INTRO_LAND_DURS.sum()
        private val INTRO_ROAR_DUR = INTRO_ROAR_DURS.sum()
        private const val INTRO_ROAR_SOUND_TIME = 0.12f
        private const val LAND_SHAKE_DUR = 0.4f

        private val CHEST_BEAT_DURS = gdxArrayOf(0.1f, 0.1f, 0.3f, 0.06f, 0.06f, 0.08f, 0.06f, 0.06f, 0.08f, 0.12f)
        private const val CHEST_BEAT_LOOP_START = 3
        private const val CHEST_BEAT_LOOP_END = 8
        private const val CHEST_BEAT_LEFT_HIT_CELL = 3
        private const val CHEST_BEAT_RIGHT_HIT_CELL = 6
        private const val CHEST_BEAT_ROAR_TIME = 0.2f
        private const val CHEST_BEAT_SHAKE_DUR = 0.15f
        private const val CHEST_BEAT_SHAKE_INTERVAL = 0.05f
        private const val MAD_FLASH_DUR = 0.5f
        private const val MAD_FLASH_BLINK = 0.05f
        private val MAD_FLASH_COLOR: Color = Color.RED

        private const val SHAKE_INTERVAL = 0.1f
        private const val SHAKE_X = 0.001f
        private const val SHAKE_Y = 0.005f

        private const val SPRITE_SIZE = 10f
        private const val CELL_SIZE = 160f

        private const val STEAM_SIZE = 2f
        private const val STEAM_JET_SIZE = 1f
        private const val STEAM_OFFSET = 8f
        private const val STEAM_FRAMES = 5
        private const val STEAM_FRAME_DUR = 0.09f
        private const val STEAM_JET_FRAMES = 3
        private const val STEAM_JET_FRAME_DUR = 0.06f
        private const val STEAM_JET_DUR = STEAM_JET_FRAMES * STEAM_JET_FRAME_DUR
        private const val STEAM_PUFF_END = STEAM_JET_DUR + STEAM_FRAMES * STEAM_FRAME_DUR
        private const val STEAM_CYCLE = 0.8f
        private const val STEAM_KEY = "steam"
        private const val STEAM_JET_KEY = "steam_jet"
        private const val STEAM_LEFT_SPRITE = "steam_left"
        private const val STEAM_RIGHT_SPRITE = "steam_right"

        private val MAD_STAND_EARS = gdxArrayOf(
            ReactorMonkeyEarPoints(45f, 89f, 66f, 89f),
            ReactorMonkeyEarPoints(44f, 89f, 65f, 89f)
        )
        private val MAD_THROW_EARS = gdxArrayOf(
            ReactorMonkeyEarPoints(65f, 81f, 86f, 81f),
            ReactorMonkeyEarPoints(88f, 63f, 101f, 63f),
            ReactorMonkeyEarPoints(42f, 98f, 63f, 98f)
        )
        private val MAD_THROW_HOLD_EARS = gdxArrayOf(ReactorMonkeyEarPoints(65f, 81f, 86f, 81f))
        private val EAR_POINTS = objectMapOf(
            "mad_stand" pairTo MAD_STAND_EARS,
            "mad_stand_damaged" pairTo MAD_STAND_EARS,
            "mad_throw" pairTo MAD_THROW_EARS,
            "mad_throw_damaged" pairTo MAD_THROW_EARS,
            "mad_throw_hold" pairTo MAD_THROW_HOLD_EARS,
            "mad_throw_hold_damaged" pairTo MAD_THROW_HOLD_EARS
        )

        private val DAMAGED_KEYS = objectMapOf(
            "stand" pairTo "stand_damaged",
            "throw" pairTo "throw_damaged",
            "throw_hold" pairTo "throw_hold_damaged",
            "chest_beat" pairTo "chest_beat_damaged",
            "mad_stand" pairTo "mad_stand_damaged",
            "mad_throw" pairTo "mad_throw_damaged",
            "mad_throw_hold" pairTo "mad_throw_hold_damaged"
        )
        private val TIME_SYNCED_KEYS = objectMapOf(
            "throw" pairTo "throw_damaged",
            "throw_damaged" pairTo "throw",
            "mad_throw" pairTo "mad_throw_damaged",
            "mad_throw_damaged" pairTo "mad_throw",
            "chest_beat" pairTo "chest_beat_damaged",
            "chest_beat_damaged" pairTo "chest_beat"
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
            Fireball::class pairTo dmgNeg(8),
            MagmaWave::class pairTo dmgNeg(8),
            MagmaFlame::class pairTo dmgNeg(8),
            MoonScythe::class pairTo dmgNeg(4),
            SmallIceCube::class pairTo dmgNeg(2),
            Axe::class pairTo dmgNeg(2),
            Needle::class pairTo dmgNeg(2),
            SlashWave::class pairTo dmgNeg(2),
            PreciousGem::class pairTo dmgNeg(2),
            ReactorManProjectile::class pairTo dmgNeg(2),
            IceBombExplosion::class pairTo dmgNeg(2),
            ReactorExplosion::class pairTo dmgNeg(2)
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class ReactorMonkeyState { INTRO_DROP, INTRO_LAND, INTRO_ROAR, STAND, HOLD, THROW, MAD_BEAT }

    // TODO: the Bank throw (needs the `throw_up` strips) joins the deck here (item 077, branch 077i later)
    private enum class ReactorMonkeyThrowType { LOB, SKIP, FAKE }

    override val damageNegotiator: IDamageNegotiator = object : BossDamageNegotiator() {
        override fun get(damager: IDamagerEntity) = DAMAGE[damager::class]?.get(damager) ?: 0
    }
    override val healthPhases = 2
    override val healthBarRegions = gdxArrayOf(ConstKeys.STANDARD, BossBitsBar.PHASE_2_BITS)

    override lateinit var facing: Facing

    private lateinit var state: ReactorMonkeyState
    private val position = Vector2()

    private val tuning: ReactorMonkeyTuning
        get() = ReactorMonkeyTuning.of(services.gameState.hardMode, healthPhase)

    private var monkeyBall: ReactorMonkeyBall? = null

    private val throwTimer = Timer(THROW_DUR)
    private val throwDelayTimer = Timer()
    private val holdTimer = Timer()
    private val introTimer = Timer()
    private val madBeatTimer = Timer()

    private var ballSpawnY = DEFAULT_BALL_SPAWN_Y
    private val ballCatchArea = GameCircle().setRadius(BALL_CATCH_RADIUS * ConstVals.PPM)

    private var throwType = ReactorMonkeyThrowType.LOB
    private var lastThrowType: ReactorMonkeyThrowType? = null
    private val aimCycle = Array<OrbAim>()
    private var aimIndex = 0
    private var throwAim = OrbAim.CURRENT
    private val aimTarget = Vector2()
    private val arena = GameRectangle()
    private val randomRange: (Float, Float) -> Float = { min, max -> services.random.nextFloat(min, max) }

    private var introDropOffsetY = 0f
    private var introDropVelocity = 0f
    private var introRoared = false

    private val chestBeatHitTimes = GdxFloatArray()
    private var chestBeatSteamTime = 0f
    private var madBeatPending = false
    private var madBeatHitIndex = 0
    private var madBeatRoared = false
    private var enraged = false

    private var steamOn = false
    private var steamTime = 0f

    private lateinit var animator: Animator

    private var megaHeartTank: MegaHeartTank? = null

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.REACTOR_MONKEY.source)
            gdxArrayOf(
                "stand",
                "stand_damaged",
                "throw",
                "throw_damaged",
                "intro_drop",
                "intro_land",
                "intro_roar",
                "chest_beat",
                "chest_beat_damaged",
                "mad_stand",
                "mad_stand_damaged",
                "mad_throw",
                "mad_throw_damaged",
                STEAM_KEY,
                STEAM_JET_KEY
            ).forEach { key ->
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        OrbAim.entries.forEach { aimCycle.add(it) }
        defineChestBeatTimes()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ORB, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        position.set(spawn)

        throwDelayTimer.resetDuration(tuning.throwDelayMax)
        throwTimer.reset()
        holdTimer.reset()
        madBeatTimer.reset()

        state = ReactorMonkeyState.INTRO_DROP
        services.gameCamera.toGameRectangle(arena)
        introDropOffsetY = maxOf(arena.getMaxY() - position.y, 0f)
        introDropVelocity = -INTRO_DROP_SPEED * ConstVals.PPM
        introRoared = false

        throwType = ReactorMonkeyThrowType.LOB
        lastThrowType = null
        services.random.shuffle(aimCycle)
        aimIndex = 0
        throwAim = OrbAim.CURRENT

        madBeatPending = false
        madBeatHitIndex = 0
        madBeatRoared = false
        enraged = false
        steamOn = false
        steamTime = 0f

        facing = if (targetPlayer.body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT
        ballSpawnY = spawnProps.getOrDefault(BALL_SPAWN_Y_KEY, DEFAULT_BALL_SPAWN_Y, Float::class)

        megaHeartTank = when {
            spawnProps.containsKey("${ConstKeys.HEART}_${ConstKeys.TANK}") ->
                MegaHeartTank.valueOf(
                    spawnProps.get("${ConstKeys.HEART}_${ConstKeys.TANK}", String::class)!!.uppercase()
                )

            else -> null
        }
    }

    override fun isReady(delta: Float) = state == ReactorMonkeyState.STAND

    override fun preReady(delta: Float) {
        when (state) {
            ReactorMonkeyState.INTRO_DROP -> {
                introDropVelocity += INTRO_DROP_GRAVITY * ConstVals.PPM * delta
                introDropOffsetY += introDropVelocity * delta
                if (introDropOffsetY <= 0f) {
                    introDropOffsetY = 0f
                    state = ReactorMonkeyState.INTRO_LAND
                    introTimer.resetDuration(INTRO_LAND_DUR)
                    shakeCamera(LAND_SHAKE_DUR, SHAKE_INTERVAL)
                    requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
                }
            }

            ReactorMonkeyState.INTRO_LAND -> {
                introTimer.update(delta)
                if (introTimer.isFinished()) {
                    state = ReactorMonkeyState.INTRO_ROAR
                    introTimer.resetDuration(INTRO_ROAR_DUR)
                }
            }

            ReactorMonkeyState.INTRO_ROAR -> {
                introTimer.update(delta)
                if (!introRoared && introTimer.time >= INTRO_ROAR_SOUND_TIME) {
                    introRoared = true
                    requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
                }
                if (introTimer.isFinished()) state = ReactorMonkeyState.STAND
            }

            else -> {}
        }
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        bossComponent.phase != BossPhase.ENTRANCE && super.canBeDamagedBy(damager)

    override fun onHealthPhaseStart(phase: Int) {
        GameLogger.debug(TAG, "onHealthPhaseStart(): phase=$phase")
        madBeatPending = true
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        monkeyBall?.let { ball -> if (ball.existenceState.liveAfterUpdate) ball.destroy() }
        monkeyBall = null

        if (megaHeartTank != null && !services.gameState.containsHeartTank(megaHeartTank!!)) {
            val heartTank = MegaEntityFactory.fetch(HeartTank::class)!!
            heartTank.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.VALUE pairTo megaHeartTank!!))
        }
    }

    private fun spawnNewMonkeyBall() {
        if (this.monkeyBall != null) throw IllegalStateException("Monkey ball should be null when a new one is spawned")

        val spawn = body.getPositionPoint(Position.TOP_CENTER).add(0f, ballSpawnY * ConstVals.PPM)

        val monkeyBall = MegaEntityFactory.fetch(ReactorMonkeyBall::class)!!
        monkeyBall.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.COLLIDE pairTo false,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.RED pairTo enraged
            )
        )

        this.monkeyBall = monkeyBall
    }

    private fun catchMonkeyBall(ball: ReactorMonkeyBall) {
        ball.body.physics.velocity.setZero()
        ball.body.physics.gravityOn = false
        ball.hidden = true
    }

    private fun hurlMonkeyBall(ball: ReactorMonkeyBall) {
        services.gameCamera.toGameRectangle(arena)
        arena.set(arena.getX(), body.getY(), arena.getWidth(), arena.getMaxY() - body.getY())
        OrbAim.point(
            throwAim,
            targetPlayer.body.getPosition(),
            targetPlayer.body.physics.velocity,
            tuning.leadTime,
            arena,
            randomRange,
            aimTarget
        )
        GameLogger.debug(TAG, "hurlMonkeyBall(): type=$throwType, aim=$throwAim, target=$aimTarget")

        when (throwType) {
            ReactorMonkeyThrowType.SKIP -> {
                val ahead = body.getWidth() / 2f + SKIP_FLOOR_AHEAD * ConstVals.PPM
                val floorX = body.getCenter().x + facing.value * ahead
                val floorTarget = GameObjectPools.fetch(Vector2::class).set(floorX, body.getY())
                ball.skip(floorTarget, aimTarget.x, tuning.skipBounces)
            }

            else -> {
                val impulse = MegaUtilMethods.calculateJumpImpulse(
                    ballCatchArea.getCenter(),
                    aimTarget,
                    BALL_IMPULSE_Y * ConstVals.PPM,
                    HORIZONTAL_SCALAR,
                    VERTICAL_SCALAR
                )
                ball.lob(impulse)
            }
        }
    }

    private fun nextThrowType(): ReactorMonkeyThrowType {
        val types = ReactorMonkeyThrowType.entries
        val last = lastThrowType
        if (last == null) return types[services.random.nextInt(0, types.size - 1)]
        var index = services.random.nextInt(0, types.size - 2)
        if (index >= last.ordinal) index++
        return types[index]
    }

    private fun nextAim(): OrbAim {
        if (aimIndex >= aimCycle.size) {
            services.random.shuffle(aimCycle)
            aimIndex = 0
        }
        return aimCycle[aimIndex++]
    }

    private fun resetThrowDelay() {
        val row = tuning
        val phaseHealthRatio = getCurrentHealth().toFloat() / getMaxHealth().toFloat()
        throwDelayTimer.resetDuration(UtilMethods.interpolate(row.throwDelayMin, row.throwDelayMax, phaseHealthRatio))
    }

    private fun startThrow() {
        throwType = nextThrowType()
        lastThrowType = throwType
        throwAim = nextAim()
        resetThrowDelay()
        throwTimer.reset()
        if (throwType == ReactorMonkeyThrowType.FAKE) {
            val row = tuning
            holdTimer.resetDuration(services.random.nextFloat(row.fakeHoldMin, row.fakeHoldMax))
            state = ReactorMonkeyState.HOLD
        } else state = ReactorMonkeyState.THROW
    }

    private fun startMadBeat() {
        GameLogger.debug(TAG, "startMadBeat(): passes=$CHEST_BEAT_PASSES")
        madBeatPending = false
        enraged = true
        madBeatTimer.reset()
        madBeatHitIndex = 0
        madBeatRoared = false
        steamOn = false
        steamTime = 0f
        state = ReactorMonkeyState.MAD_BEAT
    }

    private fun updateStand(delta: Float) {
        if (madBeatPending && monkeyBall == null) {
            startMadBeat()
            return
        }

        throwDelayTimer.update(delta)
        if (!throwDelayTimer.isFinished()) return

        val ball = monkeyBall
        if (ball == null) {
            // TODO: phase-2 moves (Chest-Beat Shockwave, Gorilla Leap, Reactor Shake, Knuckle Charge) plug in here (item 077, branch 077i later)
            spawnNewMonkeyBall()
            return
        }

        if (ballCatchArea.contains(ball.body.getCenter()) || ball.body.getY() < ballCatchArea.getY()) {
            catchMonkeyBall(ball)
            startThrow()
        }
    }

    private fun updateHold(delta: Float) {
        holdTimer.update(delta)
        if (holdTimer.isFinished()) state = ReactorMonkeyState.THROW
    }

    private fun updateThrow(delta: Float) {
        throwTimer.update(delta)
        val ball = monkeyBall
        if (throwTimer.time >= THROW_RELEASE_TIME && ball != null) {
            hurlMonkeyBall(ball)
            monkeyBall = null
        }
        if (throwTimer.isFinished()) {
            state = ReactorMonkeyState.STAND
            throwTimer.reset()
        }
    }

    private fun updateMadBeat(delta: Float) {
        madBeatTimer.update(delta)
        val time = madBeatTimer.time

        if (!madBeatRoared && time >= CHEST_BEAT_ROAR_TIME) {
            madBeatRoared = true
            requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
        }

        while (madBeatHitIndex < chestBeatHitTimes.size && time >= chestBeatHitTimes[madBeatHitIndex]) {
            madBeatHitIndex++
            shakeCamera(CHEST_BEAT_SHAKE_DUR, CHEST_BEAT_SHAKE_INTERVAL)
            requestToPlaySound(SoundAsset.POUND_SOUND, false)
        }

        if (!steamOn && time >= chestBeatSteamTime) {
            steamOn = true
            steamTime = 0f
        }

        // TODO: the Chest-Beat Shockwave (needs `RadWave`, a slam frame) ends this pose (item 077, branch 077i later)
        if (madBeatTimer.isFinished()) {
            state = ReactorMonkeyState.STAND
            resetThrowDelay()
        }
    }

    private fun shakeCamera(duration: Float, interval: Float) {
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo duration,
                    ConstKeys.INTERVAL pairTo interval,
                    ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )
    }

    private fun isMadFlashOn() = state == ReactorMonkeyState.MAD_BEAT && !defeated &&
        madBeatTimer.time < MAD_FLASH_DUR && (madBeatTimer.time / MAD_FLASH_BLINK).toInt() % 2 == 0

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            if (bossComponent.phase != BossPhase.FIGHT) return@add

            ballCatchArea.setCenter(body.getPositionPoint(Position.TOP_CENTER).add(0f, 1.75f * ConstVals.PPM))
            facing = if (targetPlayer.body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT

            if (steamOn) {
                steamTime += delta
                if (steamTime >= STEAM_CYCLE) steamTime -= STEAM_CYCLE
            }

            when (state) {
                ReactorMonkeyState.STAND -> updateStand(delta)
                ReactorMonkeyState.HOLD -> updateHold(delta)
                ReactorMonkeyState.THROW -> updateThrow(delta)
                ReactorMonkeyState.MAD_BEAT -> updateMadBeat(delta)
                else -> throw IllegalStateException("$TAG: the intro state $state cannot run in the fight")
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3.5f * ConstVals.PPM, 5f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.putProperty("${ConstKeys.RECEIVE}_${ConstKeys.FORCE}", false)
        body.addFixture(bodyFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()
            body.setBottomCenterToPoint(position)
            body.forEachFixture { fixture -> fixture.setActive(!defeated) }
        }

        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body.getBounds() }, { ballCatchArea }), debug = true
            )
        )

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val leftSteam = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val rightSteam = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val component = SpritesComponent(
            SpritesComponent.DEFAULT_KEY pairTo sprite,
            STEAM_LEFT_SPRITE pairTo leftSteam,
            STEAM_RIGHT_SPRITE pairTo rightSteam
        )
        component.putPreProcess { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.5f * ConstVals.PPM * -facing.value)
            sprite.translateY(introDropOffsetY)

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.hidden = damageBlink
            sprite.color = if (isMadFlashOn()) MAD_FLASH_COLOR else Color.WHITE
            sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
        }
        component.putPreProcess(STEAM_LEFT_SPRITE) { _, steam -> updateSteamSprite(steam, leftEar = true) }
        component.putPreProcess(STEAM_RIGHT_SPRITE) { _, steam -> updateSteamSprite(steam, leftEar = false) }
        return component
    }

    private fun steamKey(): String? = when {
        !steamOn -> null
        steamTime < STEAM_JET_DUR -> STEAM_JET_KEY
        steamTime < STEAM_PUFF_END -> STEAM_KEY
        else -> null
    }

    private fun currentEarPoints(): ReactorMonkeyEarPoints? {
        if (state == ReactorMonkeyState.MAD_BEAT) return MAD_STAND_EARS[0]
        val key = animator.currentKey ?: return null
        val points = EAR_POINTS[key] ?: return null
        val index = animator.currentAnimation?.getIndex() ?: 0
        return points[index.coerceIn(0, points.size - 1)]
    }

    private fun updateSteamSprite(steam: GameSprite, leftEar: Boolean) {
        val kind = steamKey()
        val ears = currentEarPoints()
        if (kind == null || ears == null || defeated || damageBlink) {
            steam.hidden = true
            return
        }
        steam.hidden = false

        val jet = kind == STEAM_JET_KEY
        steam.setSize((if (jet) STEAM_JET_SIZE else STEAM_SIZE) * ConstVals.PPM)

        val earX = if (leftEar) ears.leftX else ears.rightX
        val earY = if (leftEar) ears.leftY else ears.rightY
        val outward = if (leftEar) -1f else 1f
        val artX = if (jet) earX else earX + outward * STEAM_OFFSET
        val artY = if (jet) earY else earY - STEAM_OFFSET
        val point = artPoint(artX, artY, GameObjectPools.fetch(Vector2::class))

        val flip = leftEar == isFacing(Facing.RIGHT)
        steam.setFlip(flip, false)
        val anchor = when {
            !jet -> Position.BOTTOM_CENTER
            flip -> Position.BOTTOM_LEFT
            else -> Position.BOTTOM_RIGHT
        }
        steam.setPosition(point, anchor)
    }

    private fun artPoint(artX: Float, artY: Float, out: Vector2): Vector2 {
        val artPixel = SPRITE_SIZE * ConstVals.PPM / CELL_SIZE
        val left = body.getX() + body.getWidth() / 2f - SPRITE_SIZE * ConstVals.PPM / 2f +
            0.5f * ConstVals.PPM * -facing.value
        val bottom = body.getY() + introDropOffsetY
        val cellX = if (isFacing(Facing.RIGHT)) CELL_SIZE - artX - 0.5f else artX + 0.5f
        return out.set(left + cellX * artPixel, bottom + (CELL_SIZE - artY - 0.5f) * artPixel)
    }

    private fun stateKey(): String = when (state) {
        ReactorMonkeyState.INTRO_DROP -> "intro_drop"
        ReactorMonkeyState.INTRO_LAND -> "intro_land"
        ReactorMonkeyState.INTRO_ROAR -> "intro_roar"
        ReactorMonkeyState.STAND -> if (enraged) "mad_stand" else "stand"
        ReactorMonkeyState.HOLD -> if (enraged) "mad_throw_hold" else "throw_hold"
        ReactorMonkeyState.THROW -> if (enraged) "mad_throw" else "throw"
        ReactorMonkeyState.MAD_BEAT -> "chest_beat"
    }

    private fun chestBeatCells(out: GdxIntArray): GdxIntArray {
        for (i in 0 until CHEST_BEAT_LOOP_START) out.add(i)
        for (pass in 0 until CHEST_BEAT_PASSES) for (i in CHEST_BEAT_LOOP_START..CHEST_BEAT_LOOP_END) out.add(i)
        for (i in CHEST_BEAT_LOOP_END + 1 until CHEST_BEAT_DURS.size) out.add(i)
        return out
    }

    private fun defineChestBeatTimes() {
        val cells = chestBeatCells(GdxIntArray())
        var time = 0f
        for (i in 0 until cells.size) {
            val cell = cells[i]
            if (cell == CHEST_BEAT_LEFT_HIT_CELL || cell == CHEST_BEAT_RIGHT_HIT_CELL) chestBeatHitTimes.add(time)
            if (i == cells.size - 1) chestBeatSteamTime = time
            time += CHEST_BEAT_DURS[cell]
        }
        madBeatTimer.resetDuration(time)
    }

    private fun chestBeatAnimation(region: TextureRegion): Animation {
        val split = region.splitAndFlatten(CHEST_BEAT_DURS.size, 1, Array())
        val cells = chestBeatCells(GdxIntArray())
        val frames = Array<TextureRegion>()
        val durations = Array<Float>()
        for (i in 0 until cells.size) {
            frames.add(split[cells[i]])
            durations.add(CHEST_BEAT_DURS[cells[i]])
        }
        return Animation(frames, durations, loop = false)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = key@{
            val key = stateKey()
            if (!damageTimer.isFinished()) return@key DAMAGED_KEYS[key] ?: key
            return@key key
        }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(1f, 0.1f), true),
            "stand_damaged" pairTo Animation(regions["stand_damaged"]),
            "throw" pairTo Animation(regions["throw"], 3, 1, 0.1f, false),
            "throw_damaged" pairTo Animation(regions["throw_damaged"], 3, 1, 0.1f, false),
            "throw_hold" pairTo Animation(regions["throw"], 3, 1, 0.1f, false).slice(0, 1),
            "throw_hold_damaged" pairTo Animation(regions["throw_damaged"], 3, 1, 0.1f, false).slice(0, 1),
            "intro_drop" pairTo Animation(regions["intro_drop"]),
            "intro_land" pairTo Animation(regions["intro_land"], 3, 1, INTRO_LAND_DURS, false),
            "intro_roar" pairTo Animation(regions["intro_roar"], 6, 1, INTRO_ROAR_DURS, false),
            "chest_beat" pairTo chestBeatAnimation(regions["chest_beat"]),
            "chest_beat_damaged" pairTo chestBeatAnimation(regions["chest_beat_damaged"]),
            "mad_stand" pairTo Animation(regions["mad_stand"], 2, 1, 0.08f, true),
            "mad_stand_damaged" pairTo Animation(regions["mad_stand_damaged"], 2, 1, 0.08f, true),
            "mad_throw" pairTo Animation(regions["mad_throw"], 3, 1, 0.1f, false),
            "mad_throw_damaged" pairTo Animation(regions["mad_throw_damaged"], 3, 1, 0.1f, false),
            "mad_throw_hold" pairTo Animation(regions["mad_throw"], 3, 1, 0.1f, false).slice(0, 1),
            "mad_throw_hold_damaged" pairTo Animation(regions["mad_throw_damaged"], 3, 1, 0.1f, false).slice(0, 1)
        )
        val onChangeKey: (Animator, String?, String?) -> Unit = { _, previous, current ->
            if (previous != null && current != null && TIME_SYNCED_KEYS[previous] == current) {
                val time = animations[previous].getCurrentTime()
                animations[current].setCurrentTime(time)
                GameLogger.debug(TAG, "onChangeKey(): set current to time=$time")
            }
        }
        animator = Animator(keySupplier, animations, onChangeKey = onChangeKey)
        val component = AnimationsComponent(this, animator)

        gdxArrayOf(STEAM_LEFT_SPRITE, STEAM_RIGHT_SPRITE).forEach { spriteKey ->
            val steamAnimations = objectMapOf<String, IAnimation>(
                STEAM_KEY pairTo Animation(regions[STEAM_KEY], STEAM_FRAMES, 1, STEAM_FRAME_DUR, false),
                STEAM_JET_KEY pairTo Animation(regions[STEAM_JET_KEY], STEAM_JET_FRAMES, 1, STEAM_JET_FRAME_DUR, false)
            )
            component.putAnimator(spriteKey, Animator({ steamKey() }, steamAnimations))
        }
        return component
    }
}
