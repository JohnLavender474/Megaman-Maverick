package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.*
import com.maverick.game.engine.common.interfaces.IStateable
import com.maverick.game.engine.common.objects.*
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.facing
import com.maverick.game.engine.players.all
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.drawables.ui.BossBitsBar
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import com.megaman.maverick.game.entities.bosses.elecdevil.ClawPhase
import com.megaman.maverick.game.entities.bosses.elecdevil.ElecDevilTuning
import com.megaman.maverick.game.entities.bosses.elecdevil.EyePattern
import com.megaman.maverick.game.entities.bosses.elecdevil.LiveWireBand
import com.megaman.maverick.game.entities.bosses.elecdevil.LiveWireLinks
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.decorations.WilyGenerator
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.ReactorExplosion
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.ElecBall
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

class ElecDevil(services: MegaServices) : AbstractBoss(services), IStateable<ElecDevilState> {

    companion object {
        const val TAG = "ElecDevil"

        internal const val INIT_DUR = 1f

        internal const val MIN_HEALTH_FOR_LAUNCH_OFFSET = 5f

        internal const val EYE_SPREAD_SHOT_GAP = 0.1f
        internal const val EYE_SHOOT_MIN_DUR = 0.3f

        internal const val RING_BALLS = 10
        internal const val RING_SPEED = 5f

        internal val STALL_ANGLES = floatArrayOf(25f, 50f, 75f)
        internal const val STALL_SPEED = 8f
        internal const val STALL_MOVE_DUR = 0.35f
        internal const val STALL_PAUSE_DUR = 0.3f
        internal const val STALL_DART_SPEED = 14f

        internal const val BOUNCE_SPEED = 12f
        internal const val BOUNCE_SHOT_GAP = 0.15f

        internal const val CLAW_RECOIL_DUR = 0.25f
        internal const val HAND_OFFSET_X = 2.4f
        internal const val HAND_OFFSET_Y = 0.95f

        internal const val PHASE_FLASH_DUR = 0.6f
        internal const val PHASE_FLASH_BLINK = 0.1f
        internal val PHASE_FLASH_COLOR: Color = Color.YELLOW

        private const val FROM_LEFT = "${ConstKeys.FROM}_${ConstKeys.LEFT}"

        private val DAMAGE = objectMapOf<KClass<out IDamagerEntity>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(2),
            ProtoBuster::class pairTo dmgNeg(2),
            PlayerSabreSlash::class pairTo dmgNeg {
                it as PlayerSabreSlash
                if (it.fullStrength) 4 else 2
            },
            Fireball::class pairTo dmgNeg(2),
            MagmaFlame::class pairTo dmgNeg(2),
            MagmaWave::class pairTo dmgNeg(2),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 4 else 2
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 4 else 2
            },
            MoonScythe::class pairTo dmgNeg(2),
            SmallIceCube::class pairTo dmgNeg(2),
            Axe::class pairTo dmgNeg(4),
            Needle::class pairTo dmgNeg(2),
            SlashWave::class pairTo dmgNeg(2),
            PreciousGem::class pairTo dmgNeg(2),
            ReactorManProjectile::class pairTo dmgNeg(2),
            IceBombExplosion::class pairTo dmgNeg(2),
            ReactorExplosion::class pairTo dmgNeg(2)
        )

        private val CYCLE = gdxArrayOf(
            ElecDevilState.LAUNCH,
            ElecDevilState.APPEAR,
            ElecDevilState.STAND,
            ElecDevilState.CHARGE,
            ElecDevilState.STAND,
            ElecDevilState.HAND,
            ElecDevilState.STAND,
            ElecDevilState.TURN_TO_PIECES
        )
    }

    override val damageNegotiator: IDamageNegotiator = object : BossDamageNegotiator() {
        override fun get(damager: IDamagerEntity) = DAMAGE[damager::class]?.get(damager) ?: super.get(damager)
    }
    override val healthPhases = 2
    override val healthBarRegions = gdxArrayOf(ConstKeys.STANDARD, BossBitsBar.PHASE_2_BITS)

    private data class ElecDevilLaunchQueueElement(
        val start: IntPair,
        val target: IntPair,
        val startPosition: Vector2,
        val targetPosition: Vector2,
        val linkBand: LiveWireBand?
    )

    private var leftBody: ElecDevilBody? = null
    private var leftBodyPieces: ElecDevilBodyPieces? = null

    private var rightBody: ElecDevilBody? = null
    private var rightBodyPieces: ElecDevilBodyPieces? = null

    // if true, then the left body should be active, and pieces should come from the left, and vice versa if false
    private var fromLeft = true

    private var cycleIndex = 0
    private var currentState = ElecDevilState.LAUNCH
    private val stateTimers = orderedMapOf(
        ElecDevilState.APPEAR pairTo Timer(ElecDevilTuning.APPEAR_DUR),
        ElecDevilState.STAND pairTo Timer(ElecDevilTuning.STAND_DUR),
        ElecDevilState.CHARGE pairTo Timer(ElecDevilTuning.CHARGE_DUR),
        ElecDevilState.TURN_TO_PIECES pairTo Timer(ElecDevilTuning.TURN_TO_PIECES_DUR)
    )

    private val tuning: ElecDevilTuning
        get() = ElecDevilTuning.of(services.gameState.hardMode, healthPhase)

    private val initTimer = Timer(INIT_DUR)
    private val phaseFlashTimer = Timer(PHASE_FLASH_DUR)

    private var clawPhase = ClawPhase.WINDUP
    private val clawTimer = Timer()
    private var claw: ElecDevilClaw? = null

    private var eyePattern = EyePattern.FAN
    private var lastEyePattern: EyePattern? = null
    private val eyePatternCandidates = Array<EyePattern>()
    private val eyeSpreadTargets = Array<Vector2>()
    private var eyeSpreadGap = EYE_SPREAD_SHOT_GAP
    private var eyeSpreadShots = 0
    private var eyeSpreadShotsFired = 0
    private var eyeSpreadTime = 0f
    private val randomRange: (Float, Float) -> Float = { min, max -> services.random.nextFloat(min, max) }

    private val launchQueue = Queue<ElecDevilLaunchQueueElement>()
    private val launchedPieces = Array<ElecDevilBodyPiece>()
    private val launchDelayTimer = Timer()
    private var launches = 0
    private var lastLaunchedPiece: ElecDevilBodyPiece? = null
    private val liveWireCandidates = IntArray(ElecDevilConstants.PIECE_COUNT)
    private val liveWireLinks = BooleanArray(ElecDevilConstants.PIECE_COUNT)
    private val startRows = IntArray(ElecDevilConstants.PIECE_COUNT)
    private val targetRows = IntArray(ElecDevilConstants.PIECE_COUNT)

    private lateinit var lightSourceKeys: ObjectSet<Int>

    private var startX = 0f

    private val reusableIntMap = OrderedMap<Int, OrderedSet<Int>>()

    private lateinit var eye: IGameShape2D
    private lateinit var hand: IGameShape2D

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
    }

    // On spawn, both bodies should be inactive, all body pieces should be inactive, and body pieces should be queued
    // to spawn from off-screen via the "start" object property.
    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val room = spawnProps.get(ConstKeys.ROOM, RectangleMapObject::class)!!.rectangle
        body.set(room)

        cycleIndex = 0
        currentState = CYCLE[0]
        phaseFlashTimer.setToEnd()
        lastLaunchedPiece = null
        eyeSpreadShots = 0
        eyeSpreadShotsFired = 0
        eyeSpreadTime = 0f
        eyePattern = EyePattern.FAN
        lastEyePattern = null
        clawPhase = ClawPhase.WINDUP
        claw = null

        lightSourceKeys = spawnProps.get("${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}", String::class)!!
            .replace(Regex("\\s+"), "")
            .split(",")
            .filter { it.isNotBlank() }
            .map { it.toInt() }
            .toObjectSet()

        val leftPos = spawnProps.get(
            ConstKeys.LEFT, RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER)

        val leftBody = MegaEntityFactory.fetch(ElecDevilBody::class)!!
        leftBody.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.POSITION pairTo leftPos,
                ConstKeys.FACING pairTo Facing.RIGHT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
        this.leftBody = leftBody

        val leftBodyPieces = MegaEntityFactory.fetch(ElecDevilBodyPieces::class)!!
        leftBodyPieces.spawn(
            props(
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.OWNER pairTo leftBody,
                ConstKeys.FACING pairTo Facing.RIGHT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
        this.leftBodyPieces = leftBodyPieces

        val rightPos = spawnProps.get(
            ConstKeys.RIGHT, RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER)

        val rightBody = MegaEntityFactory.fetch(ElecDevilBody::class)!!
        rightBody.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.POSITION pairTo rightPos,
                ConstKeys.FACING pairTo Facing.LEFT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
        this.rightBody = rightBody

        val rightBodyPieces = MegaEntityFactory.fetch(ElecDevilBodyPieces::class)!!
        rightBodyPieces.spawn(
            props(
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.OWNER pairTo rightBody,
                ConstKeys.FACING pairTo Facing.LEFT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
        this.rightBodyPieces = rightBodyPieces

        fromLeft = spawnProps.get(FROM_LEFT, Boolean::class)!!

        startX = spawnProps.get(
            "${ConstKeys.START}_${ConstKeys.X}", RectangleMapObject::class
        )!!.rectangle.getCenter().x

        launches = 0

        initTimer.reset()
    }

    override fun isReady(delta: Float) = initTimer.isFinished()

    override fun onReady() {
        super.onReady()

        setBothBodiesInactive()
        setBodyPiecesActive(true)

        loadLaunchQueue(true)
        resetLaunchDelay()

        GameLogger.debug(
            TAG,
            "onReady():\n" +
                "\tleftBody=${leftBody},\n" +
                "\trightBody=${rightBody},\n" +
                "\tleftBodyPieces=${leftBodyPieces},\n" +
                "\trightBodyPieces=${rightBodyPieces},\n" +
                "\tlaunchPositionQueue=" + if (launchQueue.isEmpty) "[]" else "\n\t\t${launchQueue.toString("\n\t\t")}"
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        launchQueue.clear()

        destroyClaw()

        launchedPieces.forEach { it.destroy() }
        launchedPieces.clear()
        lastLaunchedPiece = null

        reusableIntMap.clear()

        leftBody?.destroy()
        leftBody = null

        leftBodyPieces?.destroy()
        leftBodyPieces = null

        rightBody?.destroy()
        rightBody = null

        rightBodyPieces?.destroy()
        rightBodyPieces = null
    }

    override fun spawnDefeatExplosion() {
        val position = services.random.pick(Position.entries)

        val spawn = when {
            leftBody!!.on -> leftBody!!.body.getCenter()
            rightBody!!.on -> rightBody!!.body.getCenter()
            leftBodyPieces!!.on -> leftBodyPieces!!.body.getCenter()
            else -> rightBodyPieces!!.body.getCenter()
        }

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
                ConstKeys.POSITION pairTo spawn.add(
                    (position.x - 1) * ConstVals.PPM.toFloat(),
                    (position.y - 1) * ConstVals.PPM.toFloat()
                ),
                ConstKeys.DAMAGER pairTo false
            )
        )
    }

    override fun spawnExplosionOrbs(spawn: Vector2) {
        GameLogger.debug(TAG, "spawnExplosionOrbs()")
        val spawn = when {
            leftBody!!.on -> leftBody!!.body.getCenter()
            rightBody!!.on -> rightBody!!.body.getCenter()
            leftBodyPieces!!.on -> leftBodyPieces!!.body.getCenter()
            else -> rightBodyPieces!!.body.getCenter()
        }
        super.spawnExplosionOrbs(spawn)
    }

    override fun disintegrate(disintegrationProps: Properties?) {
        GameLogger.debug(TAG, "disintegrate()")
        val spawn = when {
            leftBody!!.on -> leftBody!!.body.getCenter()
            rightBody!!.on -> rightBody!!.body.getCenter()
            leftBodyPieces!!.on -> leftBodyPieces!!.body.getCenter()
            else -> rightBodyPieces!!.body.getCenter()
        }
        super.disintegrate(props(ConstKeys.POSITION pairTo spawn))
    }

    override fun getCurrentState() = currentState

    override fun onHealthPhaseStart(phase: Int) {
        GameLogger.debug(TAG, "onHealthPhaseStart(): phase=$phase")
        phaseFlashTimer.reset()
        requestToPlaySound(SoundAsset.MM3_ELECTRIC_PULSE_SOUND, loop = false)
        if (phase == 2) powerDownGenerators()
    }

    private fun powerDownGenerators() =
        MegaGameEntities.getOfTag(WilyGenerator.TAG).forEach { (it as WilyGenerator).powerDown() }

    internal fun isPhaseFlashOn() =
        !phaseFlashTimer.isFinished() && (phaseFlashTimer.time / PHASE_FLASH_BLINK).toInt() % 2 == 0

    private fun nextState(): ElecDevilState {
        cycleIndex = (cycleIndex + 1) % CYCLE.size
        val next = CYCLE[cycleIndex]
        currentState = if (next == ElecDevilState.LAUNCH) pickMoveAttack() else next
        val stateTuning = tuning
        stateTimers[currentState]?.resetDuration(stateTuning.durationOf(currentState))
        when (currentState) {
            ElecDevilState.CHARGE -> pickEyePattern(stateTuning)
            ElecDevilState.HAND -> {
                if (stateTuning.clawDelayDur > 0f) {
                    clawPhase = ClawPhase.DELAY
                    clawTimer.resetDuration(stateTuning.clawDelayDur)
                } else startClawWindup(stateTuning)
            }

            else -> {}
        }
        return currentState
    }

    private fun pickEyePattern(stateTuning: ElecDevilTuning) {
        eyePatternCandidates.clear()
        stateTuning.eyePatterns.forEach { if (it != lastEyePattern) eyePatternCandidates.add(it) }
        eyePattern = eyePatternCandidates[services.random.nextInt(0, eyePatternCandidates.size - 1)]
        lastEyePattern = eyePattern
    }

    internal fun getEyePattern() = eyePattern

    internal fun getClawPhase() = clawPhase

    private fun startClawWindup(clawTuning: ElecDevilTuning) {
        clawPhase = ClawPhase.WINDUP
        clawTimer.resetDuration(clawTuning.clawWindupDur)
        spawnClaw(clawTuning)
    }

    private fun updateTetherClaw(delta: Float): Boolean {
        when (clawPhase) {
            ClawPhase.DELAY -> {
                clawTimer.update(delta)
                if (clawTimer.isFinished()) startClawWindup(tuning)
            }

            ClawPhase.WINDUP -> {
                clawTimer.update(delta)
                if (clawTimer.isFinished()) {
                    claw!!.fire()
                    clawPhase = ClawPhase.OUT
                    requestToPlaySound(SoundAsset.MM3_ELECTRIC_SAW_SOUND, loop = false)
                }
            }

            ClawPhase.OUT -> {}

            ClawPhase.RECOIL -> {
                clawTimer.update(delta)
                return clawTimer.isFinished()
            }
        }
        return false
    }

    private fun spawnClaw(clawTuning: ElecDevilTuning) {
        val facingRight = leftBody!!.on
        val activeBody = if (facingRight) leftBody!! else rightBody!!
        val floorY = activeBody.body.getY()
        val clawY = floorY + ElecDevilClaw.BODY_HEIGHT * ConstVals.PPM / 2f

        val socket = handCenter(GameObjectPools.fetch(Vector2::class))
        val room = body.getBounds()
        val holdInset = ElecDevilClaw.BODY_WIDTH / 2f * ConstVals.PPM
        val holdX = if (facingRight) room.getMaxX() - holdInset else room.getX() + holdInset
        val hold = GameObjectPools.fetch(Vector2::class).set(holdX, clawY)

        val claw = MegaEntityFactory.fetch(ElecDevilClaw::class)!!
        claw.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.FACING pairTo if (facingRight) Facing.RIGHT else Facing.LEFT,
                ConstKeys.START pairTo socket,
                ConstKeys.TARGET pairTo hold,
                ConstKeys.SPEED pairTo clawTuning.clawSpeed,
                ConstKeys.DURATION pairTo clawTuning.clawHoldDur,
                ConstKeys.END pairTo { onClawBack() }
            )
        )
        this.claw = claw
    }

    private fun onClawBack() {
        claw = null
        clawPhase = ClawPhase.RECOIL
        clawTimer.resetDuration(CLAW_RECOIL_DUR)
        requestToPlaySound(SoundAsset.ELECTRIC_2_SOUND, loop = false)
    }

    private fun destroyClaw() {
        claw?.destroy()
        claw = null
    }

    private fun handCenter(out: Vector2): Vector2 = when {
        leftBody!!.on -> out.set(leftBody!!.body.getCenter()).add(
            HAND_OFFSET_X * ConstVals.PPM, HAND_OFFSET_Y * ConstVals.PPM
        )

        else -> out.set(rightBody!!.body.getCenter()).add(
            -HAND_OFFSET_X * ConstVals.PPM, HAND_OFFSET_Y * ConstVals.PPM
        )
    }

    private fun pickMoveAttack(): ElecDevilState {
        if (healthPhase < 2) return ElecDevilState.LAUNCH
        // TODO: phase 2 picks among LAUNCH, SERPENT, CIRCUIT_FLOOR and STORM_RAIN with services.random (item 077, the branch after 077c)
        return ElecDevilState.LAUNCH
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (bossComponent.phase == BossPhase.ENTRANCE) {
                // TODO: players.all also visits a player who is dead or waits to respawn; such a
                //  player never lands, so the boss would never start; skipping players who are not
                //  present is a co-op rule for item 047
                if (players.all { it.body.isSensing(BodySense.FEET_ON_GROUND) }) initTimer.update(delta)
                return@add
            }

            if (defeated) {
                destroyClaw()
                explodeOnDefeat(delta)
                return@add
            }

            phaseFlashTimer.update(delta)
            fireDueEyeSpreadShots(delta)

            val launchedPieceIter = launchedPieces.iterator()
            while (launchedPieceIter.hasNext()) {
                val launchedPiece = launchedPieceIter.next()
                if (!launchedPiece.existenceState.liveAfterUpdate) launchedPieceIter.remove()
            }

            when (getCurrentState()) {
                // During the "launch" state, both bodies should be inactive (hidden) and all body pieces should be
                // active. The "start" body pieces entity should have its pieces hidden one by one as they're launched,
                // while the "target" entity should have its pieces shown as each projectile hits its target.
                ElecDevilState.LAUNCH -> {
                    if (launchQueue.isEmpty && launchedPieces.isEmpty) {
                        fromLeft = !fromLeft

                        val old = getCurrentState()
                        val next = nextState()
                        GameLogger.debug(TAG, "update(): old=$old, next=$next")

                        setOneBodyActive()
                        setBodyPiecesActive(false)

                        requestToPlaySound(SoundAsset.ELECTRIC_2_SOUND, loop = false)

                        return@add
                    }

                    launchDelayTimer.update(delta)
                    if (launchDelayTimer.isFinished() && !launchQueue.isEmpty) {
                        val (start, target, startPosition, targetPosition, linkBand) = launchQueue.removeFirst()

                        val startBodyPieces: ElecDevilBodyPieces
                        val targetBodyPieces: ElecDevilBodyPieces

                        if (fromLeft) {
                            startBodyPieces = leftBodyPieces!!
                            targetBodyPieces = rightBodyPieces!!
                        } else {
                            startBodyPieces = rightBodyPieces!!
                            targetBodyPieces = leftBodyPieces!!
                        }

                        GameLogger.debug(
                            TAG,
                            "update(): startBodyPieces.setStateOfPiece: start=$start, state=false"
                        )

                        val (startRow, startColumn) = start
                        startBodyPieces.setStateOfPiece(startRow, startColumn, ElecDevilBodyPieceState.INACTIVE)

                        val (targetRow, targetColumn) = target
                        targetBodyPieces.setStateOfPiece(targetRow, targetColumn, ElecDevilBodyPieceState.STANDBY)

                        val onEnd: () -> Unit = {
                            GameLogger.debug(
                                TAG,
                                "update(): targetBodyPieces.setStateOfPiece: target=$target, state=true"
                            )

                            targetBodyPieces.setStateOfPiece(targetRow, targetColumn, ElecDevilBodyPieceState.ACTIVE)
                        }

                        val speed = pieceSpeed() * ConstVals.PPM

                        val bodyPiece = MegaEntityFactory.fetch(ElecDevilBodyPiece::class)!!
                        bodyPiece.spawn(
                            props(
                                ConstKeys.END pairTo onEnd,
                                ConstKeys.OWNER pairTo this,
                                ConstKeys.SPEED pairTo speed,
                                ConstKeys.START pairTo startPosition,
                                ConstKeys.TARGET pairTo targetPosition,
                                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
                            )
                        )

                        if (linkBand != null) bodyPiece.linkedPiece = lastLaunchedPiece
                        lastLaunchedPiece = bodyPiece
                        launchedPieces.add(bodyPiece)

                        if (!launchQueue.isEmpty && launchQueue.first().linkBand == LiveWireBand.JUMP_OVER)
                            setJumpOverLaunchDelay(startPosition, targetPosition, launchQueue.first().startPosition)
                        else resetLaunchDelay()

                        requestToPlaySound(SoundAsset.ELECTRIC_1_SOUND, loop = false)
                    }
                }

                ElecDevilState.HAND -> if (updateTetherClaw(delta)) endTimedState()

                // For all other states, update the state's corresponding timer. If the timer is finished and the next
                // state is "launch", then set up the initial "active" values for the "start" and "target" entities.
                else -> {
                    val timer = stateTimers[getCurrentState()]
                    timer.update(delta)
                    if (timer.isFinished()) endTimedState()
                }
            }
        }
    }

    private fun endTimedState() {
        val old = getCurrentState()
        if (old == ElecDevilState.CHARGE) shootEyePattern()
        val next = nextState()
        GameLogger.debug(TAG, "update(): old=$old, next=$next")

        if (next == ElecDevilState.LAUNCH) {
            loadLaunchQueue(false)
            resetLaunchDelay()

            setBothBodiesInactive()
            setBodyPiecesActive(true)

            val startBodyPieces: ElecDevilBodyPieces
            val targetBodyPieces: ElecDevilBodyPieces

            if (fromLeft) {
                startBodyPieces = leftBodyPieces!!
                targetBodyPieces = rightBodyPieces!!
            } else {
                startBodyPieces = rightBodyPieces!!
                targetBodyPieces = leftBodyPieces!!
            }

            GameLogger.debug(
                TAG,
                "update(): set body pieces active states: " +
                    "startBodyPieces=${startBodyPieces.facing}, " +
                    "targetBodyPieces=${targetBodyPieces.facing}"
            )

            startBodyPieces.setStateOfAllPieces(ElecDevilBodyPieceState.ACTIVE)
            targetBodyPieces.setStateOfAllPieces(ElecDevilBodyPieceState.INACTIVE)
        }
    }

    // No need for this entity to have a body. Contacts will be made from the "body" and "pieces" entities.
    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()

        eye = GameCircle().setRadius(0.5f * ConstVals.PPM)
        val eyeFixture = Fixture(body, FixtureType.DAMAGEABLE, eye)
        eyeFixture.attachedToBody = false
        body.addFixture(eyeFixture)
        debugShapes.add { eyeFixture }

        body.preProcess.put(ConstKeys.DAMAGEABLE) {
            val active = getCurrentState().equalsAny(ElecDevilState.STAND, ElecDevilState.CHARGE, ElecDevilState.HAND)
            eyeFixture.setActive(active)

            eyeFixture.drawingColor = if (active) Color.PURPLE else Color.GRAY

            val center = when {
                leftBody!!.on -> leftBody!!.body.getCenter().add(-0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
                else -> rightBody!!.body.getCenter().add(0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
            }
            eye.setCenter(center)
        }

        hand = GameRectangle().setSize(1.5f * ConstVals.PPM)
        debugShapes.add { hand }

        val handShieldFixture = Fixture(body, FixtureType.SHIELD, hand)
        handShieldFixture.attachedToBody = false
        body.addFixture(handShieldFixture)

        val handDamagerFixture = Fixture(body, FixtureType.DAMAGER, hand)
        handDamagerFixture.attachedToBody = false
        body.addFixture(handDamagerFixture)

        body.preProcess.put(ConstKeys.HAND) {
            val active = getCurrentState() == ElecDevilState.HAND && clawPhase != ClawPhase.OUT
            hand.drawingColor = if (active) Color.YELLOW else Color.GRAY
            handShieldFixture.setActive(active)
            handDamagerFixture.setActive(active)

            hand.setCenter(handCenter(GameObjectPools.fetch(Vector2::class)))
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    // No sprites for this entity. Sprites will be shown from the "body" and "pieces" entities.
    override fun defineSpritesComponent() = SpritesComponent()

    private fun resetLaunchDelay() {
        val numerator = max(0f, getCurrentHealth() - MIN_HEALTH_FOR_LAUNCH_OFFSET)
        val ratio = min(1f, numerator / ConstVals.MAX_HEALTH)

        val launchTuning = tuning
        val min: Float
        val max: Float
        if (shouldRandomizeLaunchTargets()) {
            min = launchTuning.randomLaunchDelayMin
            max = launchTuning.randomLaunchDelayMax
        } else {
            min = launchTuning.launchDelayMin
            max = launchTuning.launchDelayMax
        }

        val duration = UtilMethods.interpolate(min, max, ratio)
        launchDelayTimer.resetDuration(duration)

        // game.setDebugText(UtilMethods.roundFloat(duration, 2).toString())
    }

    private fun setJumpOverLaunchDelay(leadStart: Vector2, leadTarget: Vector2, trailStart: Vector2) {
        val speed = pieceSpeed()
        val direction = if (leadTarget.x >= leadStart.x) 1f else -1f
        val trailBehind = (leadStart.x - trailStart.x) * direction / ConstVals.PPM
        launchDelayTimer.resetDuration(max(0f, LiveWireLinks.jumpOverPassGap(speed) - trailBehind / speed))
    }

    private fun pieceSpeed(): Float {
        val speedTuning = tuning
        return UtilMethods.interpolate(speedTuning.pieceMaxSpeed, speedTuning.pieceMinSpeed, getPhaseHealthRatio())
    }

    private fun getPhaseHealthRatio() = getCurrentHealth().toFloat() / getMaxHealth().toFloat()

    private fun shouldRandomizeLaunchTargets() = launches % 3 == 0

    private fun loadLaunchQueue(firstLaunch: Boolean) {
        launches++

        val startBody: ElecDevilBody
        val targetBody: ElecDevilBody

        if (fromLeft) {
            startBody = leftBody!!
            targetBody = rightBody!!
        } else {
            startBody = rightBody!!
            targetBody = leftBody!!
        }

        val randomTargetRows = shouldRandomizeLaunchTargets()

        val columnRowMap = when {
            randomTargetRows -> ElecDevilConstants.fillMapWithColumnsAsKeys(reusableIntMap)
            else -> null
        }

        val pieceQueue = ElecDevilConstants.getPieceQueue(services.random)
        pieceQueue.forEachIndexed { index, (row, _) -> startRows[index] = row }
        val launchTuning = tuning
        LiveWireLinks.choose(
            pieces = pieceQueue.size,
            links = launchTuning.liveWireLinks,
            minGap = launchTuning.liveWireMinGap,
            startRows = startRows,
            random = services.random,
            candidates = liveWireCandidates,
            linkedToPrevious = liveWireLinks
        )
        lastLaunchedPiece = null

        if (randomTargetRows) {
            pieceQueue.forEachIndexed { index, (row, column) ->
                if (isInLinkedPair(index, pieceQueue.size))
                    targetRows[index] = takeTargetRow(columnRowMap!!, column, LiveWireLinks.bandOf(row))
            }
            pieceQueue.forEachIndexed { index, (_, column) ->
                if (!isInLinkedPair(index, pieceQueue.size))
                    targetRows[index] = takeTargetRow(columnRowMap!!, column, band = null)
            }
        } else pieceQueue.forEachIndexed { index, (row, _) -> targetRows[index] = row }

        pieceQueue.forEachIndexed { index, (row, column) ->
            val xOffset = ElecDevilConstants.PIECE_WIDTH * ConstVals.PPM / 2f
            val yOffset = ElecDevilConstants.PIECE_HEIGHT * ConstVals.PPM / 2f

            val start = row pairTo column
            val startPosition = startBody.getPositionOf(row, column, Vector2())
            when {
                firstLaunch -> startPosition.add(0f, yOffset).setX(startX)
                else -> startPosition.add(xOffset, yOffset)
            }

            val target = targetRows[index] pairTo column
            val targetPosition = targetBody.getPositionOf(targetRows[index], column, Vector2()).add(xOffset, yOffset)

            launchQueue.addLast(
                ElecDevilLaunchQueueElement(
                    start,
                    target,
                    startPosition,
                    targetPosition,
                    if (liveWireLinks[index]) LiveWireLinks.bandOf(row) else null
                )
            )
        }

        if (columnRowMap != null && !columnRowMap.isEmpty) {
            GameLogger.error(TAG, "loadLaunchQueue(): column-row map should be empty but is not: $columnRowMap")
            columnRowMap.clear()
        }

        GameLogger.debug(
            TAG,
            "loadLaunchQueue():\n" +
                "\tstartBody=${startBody.facing},\n" +
                "\ttargetBody=${targetBody.facing},\n" +
                "\tlaunchPositionQueue=\n\t\t${launchQueue.toString("\n\t\t")}"
        )
    }

    private fun isInLinkedPair(index: Int, pieces: Int) =
        liveWireLinks[index] || (index + 1 < pieces && liveWireLinks[index + 1])

    private fun takeTargetRow(columnRowMap: OrderedMap<Int, OrderedSet<Int>>, column: Int, band: LiveWireBand?): Int {
        val rows = columnRowMap[column]
        var count = 0
        rows.orderedItems().forEach { row -> if (band == null || LiveWireLinks.bandOf(row) == band) count++ }
        if (count == 0) throw IllegalStateException("column $column has no free target row in band $band: $rows")
        var pick = services.random.nextInt(0, count - 1)
        var targetRow = -1
        for (row in rows.orderedItems()) {
            if (band != null && LiveWireLinks.bandOf(row) != band) continue
            if (pick == 0) {
                targetRow = row
                break
            }
            pick--
        }
        rows.remove(targetRow)
        if (rows.isEmpty) columnRowMap.remove(column)
        return targetRow
    }

    private fun setOneBodyActive() {
        val activeBody: ElecDevilBody
        val inactiveBody: ElecDevilBody

        if (fromLeft) {
            activeBody = leftBody!!
            inactiveBody = rightBody!!
        } else {
            activeBody = rightBody!!
            inactiveBody = leftBody!!
        }

        activeBody.on = true
        inactiveBody.on = false

        GameLogger.debug(
            TAG,
            "setOneBodyActive(): activeBody=${activeBody.facing}, inactiveBody=${inactiveBody.facing}"
        )
    }

    private fun setBothBodiesInactive() {
        GameLogger.debug(TAG, "setBothBodiesInactive()")
        leftBody!!.on = false
        rightBody!!.on = false
    }

    private fun setBodyPiecesActive(active: Boolean) {
        GameLogger.debug(TAG, "setBodyPiecesActive(): active=$active")
        leftBodyPieces!!.on = active
        rightBodyPieces!!.on = active
    }

    private fun shootEyePattern() {
        GameLogger.debug(TAG, "shootEyePattern(): pattern=$eyePattern")
        val patternTuning = tuning
        val target = targetPlayer
        val center = target.body.getCenter()
        val eyeCenter = eye.getCenter()
        when (eyePattern) {
            EyePattern.FAN -> {
                setEyeShots(patternTuning.eyeSpreadAims.size, EYE_SPREAD_SHOT_GAP)
                val velocity = target.body.physics.velocity
                patternTuning.eyeSpreadAims.forEachIndexed { index, aim ->
                    OrbAim.point(
                        aim, center, velocity, patternTuning.eyeSpreadLeadTime, body.getBounds(), randomRange,
                        eyeSpreadTargets[index]
                    )
                }
            }

            EyePattern.RING -> {
                setEyeShots(RING_BALLS, 0f)
                val angle = GameObjectPools.fetch(Vector2::class).set(center).sub(eyeCenter).angleDeg()
                for (index in 0 until RING_BALLS) {
                    val ballAngle = angle + index * 360f / RING_BALLS
                    eyeSpreadTargets[index].set(eyeCenter).add(
                        MathUtils.cosDeg(ballAngle) * ConstVals.PPM, MathUtils.sinDeg(ballAngle) * ConstVals.PPM
                    )
                }
            }

            EyePattern.STALL -> {
                setEyeShots(STALL_ANGLES.size, EYE_SPREAD_SHOT_GAP)
                val side = if (center.x >= eyeCenter.x) 1f else -1f
                STALL_ANGLES.forEachIndexed { index, ballAngle ->
                    eyeSpreadTargets[index].set(eyeCenter).add(
                        side * MathUtils.cosDeg(ballAngle) * ConstVals.PPM, MathUtils.sinDeg(ballAngle) * ConstVals.PPM
                    )
                }
            }

            EyePattern.BOUNCE -> {
                setEyeShots(2, BOUNCE_SHOT_GAP)
                val velocity = target.body.physics.velocity
                OrbAim.point(
                    OrbAim.CURRENT, center, velocity, patternTuning.eyeSpreadLeadTime, body.getBounds(), randomRange,
                    eyeSpreadTargets[0]
                )
                OrbAim.point(
                    OrbAim.LEAD, center, velocity, patternTuning.eyeSpreadLeadTime, body.getBounds(), randomRange,
                    eyeSpreadTargets[1]
                )
                val activeBody = if (leftBody!!.on) leftBody!! else rightBody!!
                val mirrorY = activeBody.body.getY() + ElecBall.BODY_SIZE * ConstVals.PPM / 2f
                for (index in 0 until 2) eyeSpreadTargets[index].y = 2f * mirrorY - eyeSpreadTargets[index].y
            }
        }
        fireDueEyeSpreadShots(0f)
    }

    private fun setEyeShots(shots: Int, gap: Float) {
        while (eyeSpreadTargets.size < shots) eyeSpreadTargets.add(Vector2())
        eyeSpreadShots = shots
        eyeSpreadGap = gap
        eyeSpreadShotsFired = 0
        eyeSpreadTime = 0f
    }

    internal fun isEyeSpreadShooting() = eyeSpreadShots > 0 && eyeSpreadTime < max(eyeSpreadShots * eyeSpreadGap, EYE_SHOOT_MIN_DUR)

    private fun fireDueEyeSpreadShots(delta: Float) {
        if (eyeSpreadShots == 0 || !isEyeSpreadShooting()) return
        eyeSpreadTime += delta
        while (eyeSpreadShotsFired < eyeSpreadShots && eyeSpreadTime >= eyeSpreadShotsFired * eyeSpreadGap) {
            spawnElecBall(GameObjectPools.fetch(Vector2::class).set(eyeSpreadTargets[eyeSpreadShotsFired]))
            if (eyeSpreadGap > 0f || eyeSpreadShotsFired == 0) requestToPlaySound(SoundAsset.VOLT_SOUND, loop = false)
            eyeSpreadShotsFired++
        }
    }

    private fun spawnElecBall(target: Vector2) {
        val ballProps = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.TARGET pairTo target,
            ConstKeys.POSITION pairTo eye.getCenter(),
            ConstKeys.COLOR pairTo eyePattern.color,
            "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
        )
        when (eyePattern) {
            EyePattern.FAN -> {}
            EyePattern.RING -> ballProps.put(ConstKeys.SPEED, RING_SPEED)
            EyePattern.STALL -> {
                ballProps.put(ConstKeys.SPEED, STALL_SPEED)
                ballProps.put(ConstKeys.DELAY, STALL_MOVE_DUR)
                ballProps.put(ConstKeys.PAUSE, STALL_PAUSE_DUR)
                ballProps.put(ElecBall.DART_SPEED, STALL_DART_SPEED)
            }

            EyePattern.BOUNCE -> {
                ballProps.put(ConstKeys.SPEED, BOUNCE_SPEED)
                ballProps.put(ConstKeys.BOUNCE, 1)
            }
        }
        MegaEntityFactory.fetch(ElecBall::class)!!.spawn(ballProps)
    }

    internal fun isDamageBlinking() = damageBlink
}
