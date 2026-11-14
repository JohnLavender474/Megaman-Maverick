package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.ArenaPerimeter
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.BigAssMaverickRobotMotion
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.BigAssMaverickRobotTuning
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandChoreography
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandSide
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.HandState
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.MotionMode
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.ShockWaveRow
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.SlamAim
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.SlamPlan
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.BigAssMaverickRobotOrbExplosion
import com.megaman.maverick.game.entities.feetFixture
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb.OrbColor
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class BigAssMaverickRobotHand(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity,
    IDamagerEntity, IHazard, IOwnable<BigAssMaverickRobot>, IAudioEntity {

    companion object {
        const val TAG = "BigAssMaverickRobot/Hand"

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f

        private const val BLOCK_WIDTH = 1.5f
        private const val BLOCK_HEIGHT = 0.25f
        private val BLOCK_POSITION = Position.TOP_CENTER

        private const val SHIELD_WIDTH = 2f
        private const val SHIELD_HEIGHT = 1f

        private const val SPRITE_SIZE = 3f

        private const val GRAVITY = -22.5f

        private const val MOVE_TO_ORIGIN_SPEED = 8f
        private const val ORB_REVEAL_DELAY = 0.15f
        private const val ARM_ORBS = 4

        const val ARRIVED = 0.1f
        const val WIND_UP_BACK = 1f
        const val ROCKET_TELL = 0.2f
        const val RECOIL_OVERSHOOT = 0.5f
        const val RECOIL_OVERSHOOT_DUR = 0.15f
        const val RECOIL_DUR = 0.3f
        const val RECOIL_HOLD = 0.4f
        const val RETURN_MAX_SPEED = 7f
        const val FALL_SPEED = 13.5f
        const val FEET_OVER_BOTTOM = 0.5f
        const val SLAM_SHAKE_DUR = 0.3f
        const val SECOND_WAVE_DELAY = 0.5f
        const val IMPACT_DUR = 0.3f
        const val DEFEAT_BOUNCE = 6f
        const val CHAIN_POP_GAP = 0.15f

        private const val BLINK_DELAY = 0.05f

        private val animDefs = orderedMapOf(
            "still" pairTo AnimationDef(),
            "rocket" pairTo AnimationDef(2, 2, 0.05f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MotionKind { STILL, EXACT, SPRING, TOWARD }

    override var owner: BigAssMaverickRobot? = null

    lateinit var side: HandSide
        private set
    var state = HandState.FOLLOW
        private set
    var stateTime = 0f
        private set
    val stateDuration: Float
        get() = when (state) {
            HandState.WIND_UP -> attackTuning.windUpDur
            HandState.RECOIL -> RECOIL_DUR
            HandState.HANG -> attackTuning.lockDur
            HandState.REST -> attackTuning.restDur
            HandState.CLAP_LOCK -> attackTuning.lockDur
            HandState.CLAP_IMPACT -> IMPACT_DUR
            else -> 0f
        }
    val clapStepDone: Boolean
        get() = when (state) {
            HandState.CLAP_ALIGN -> arrived(attackTarget, BigAssMaverickRobotMotion.ARRIVED_DISTANCE)
            HandState.CLAP_LOCK, HandState.CLAP_IMPACT -> stateTime >= stateDuration
            HandState.CLAP_CLOSE -> arrived(clapStop, ARRIVED)
            else -> false
        }
    val attackDirection = Vector2()
    val partnerOffset = Vector2()

    private val origin = Vector2()
    private val armOrigin = Vector2()
    private var attackTuning = BigAssMaverickRobotTuning.NORMAL
    private val attackTarget = Vector2()
    private val windUpTarget = Vector2()
    private val slamPlan = SlamPlan()
    private val clapStop = Vector2()
    private var secondWaveSpawned = false
    private var bounced = false

    private var motionKind = MotionKind.STILL
    private val motionTarget = Vector2()
    private var motionSpeed = 0f

    private var block: Block? = null
    private val blockTarget: Vector2
        get() = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.5f * ConstVals.PPM)

    private val orbRevealTimer = Timer(ORB_REVEAL_DELAY)
    private var orbsRevealedCount = 0
    private val armOrbs = Array<BigAssMaverickRobotOrb>()
    private var chainPopped = 0

    private val shockWaves = ShockWaveRow()

    private val blinkDelay = Timer(BLINK_DELAY)
    private var blink = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val owner = spawnProps.get(ConstKeys.OWNER, BigAssMaverickRobot::class)!!
        this.owner = owner
        side = spawnProps.get(ConstKeys.SIDE, HandSide::class)!!
        origin.set(spawnProps.get(ConstKeys.ORIGIN, Vector2::class)!!)
        armOrigin.set(spawnProps.get("${ConstKeys.ARM}_${ConstKeys.ORIGIN}", Vector2::class)!!)

        val ownerCenter = owner.body.getCenter()
        body.setCenter(ownerCenter.x, ownerCenter.y)
        body.physics.velocity.setZero()

        state = HandState.FOLLOW
        stateTime = 0f
        attackTuning = BigAssMaverickRobotTuning.NORMAL
        attackDirection.setZero()
        partnerOffset.setZero()
        secondWaveSpawned = false
        bounced = false
        motionKind = MotionKind.STILL

        orbsRevealedCount = 0
        orbRevealTimer.reset()
        chainPopped = 0

        blinkDelay.reset()
        blink = false

        val block = MegaEntityFactory.fetch(Block::class)!!
        block.spawn(
            props(
                ConstKeys.BLOCK_FILTERS pairTo TAG,
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false,
                ConstKeys.BOUNDS pairTo GameRectangle()
                    .setSize(BLOCK_WIDTH * ConstVals.PPM, BLOCK_HEIGHT * ConstVals.PPM)
                    .setTopCenterToPoint(blockTarget),
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY)
            )
        )
        this.block = block

        (0 until ARM_ORBS).forEach { _ ->
            val armOrb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
            armOrb.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.TRAJECTORY pairTo Vector2.Zero,
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    ConstKeys.CAN_BE_HIT pairTo false,
                    ConstKeys.ACTIVE pairTo false,
                    ConstKeys.HIDDEN pairTo true,
                    BigAssMaverickRobotOrb.POP_ON_BOSS_DEFEAT pairTo false,
                    ConstKeys.PRIORITY pairTo -1,
                    ConstKeys.COLOR pairTo OrbColor.PURPLE,
                    ConstKeys.SECTION pairTo DrawingSection.PLAYGROUND
                )
            )
            armOrbs.add(armOrb)
        }
    }

    override fun onBossDefeated(boss: AbstractBoss) {
        GameLogger.debug(TAG, "onBossDefeated(): boss=$boss")
        goTo(HandState.DEFEATED)
        shockWaves.clear()
        block?.destroy()
        block = null
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        armOrbs.forEach { if (it.existenceState.liveAfterUpdate) it.destroy() }
        armOrbs.clear()
        shockWaves.clear()

        block?.destroy()
        block = null
    }

    internal fun moveToOrigin() = goTo(HandState.MOVE_TO_ORIGIN)

    internal fun punch(target: Vector2, tuning: BigAssMaverickRobotTuning) {
        attackTuning = tuning
        attackTarget.set(target)
        val center = body.getCenter()
        attackDirection.set(target).sub(center).nor()
        windUpTarget.set(attackDirection).scl(-WIND_UP_BACK * ConstVals.PPM).add(center)
        goTo(HandState.WIND_UP)
    }

    internal fun slam(target: Vector2, tuning: BigAssMaverickRobotTuning) {
        attackTuning = tuning
        SlamAim.plan(
            body.getCenter(),
            target,
            owner!!.arena,
            body.getWidth() / 2f,
            body.getHeight() / 2f - FEET_OVER_BOTTOM * ConstVals.PPM,
            slamPlan
        )
        attackDirection.set(slamPlan.direction)
        attackTarget.set(slamPlan.start)
        goTo(HandState.RISE)
    }

    internal fun clap(start: Vector2, stop: Vector2, tuning: BigAssMaverickRobotTuning) {
        attackTuning = tuning
        attackTarget.set(start)
        clapStop.set(stop)
        goTo(HandState.CLAP_ALIGN)
    }

    internal fun advanceClap() = goTo(
        when (state) {
            HandState.CLAP_ALIGN -> HandState.CLAP_LOCK
            HandState.CLAP_LOCK -> HandState.CLAP_CLOSE
            HandState.CLAP_CLOSE -> HandState.CLAP_IMPACT
            else -> throw IllegalStateException("advanceClap(): the state $state is not a clap step with a next step")
        }
    )

    internal fun returnToSlot() = goTo(HandState.RETURN)

    internal fun setArmOrbColor(color: OrbColor) = armOrbs.forEach { it.color = color }

    internal fun isBeingStoodUpon(): Boolean {
        val block = block ?: return false
        return players.anyLive { it.feetFixture.getShape().overlaps(block.body.getBounds()) }
    }

    private fun goTo(next: HandState) {
        state = next
        stateTime = 0f
        when (next) {
            HandState.REVEAL_ORBS -> {
                orbsRevealedCount = 0
                orbRevealTimer.reset()
            }

            HandState.HANG, HandState.CLAP_LOCK ->
                owner!!.flashAt(GameObjectPools.fetch(Vector2::class).set(body.getCenter()))

            HandState.REST -> {
                secondWaveSpawned = false
                startShockWaves()
            }

            HandState.DEFEATED -> {
                bounced = false
                chainPopped = 0
                body.physics.velocity.setZero()
            }

            else -> {}
        }
    }

    internal fun slotTarget(out: Vector2): Vector2 = owner!!.slotOf(origin, side, out)

    private fun arrived(target: Vector2, distance: Float) =
        body.getCenter().dst2(target) <= distance * distance * ConstVals.PPM * ConstVals.PPM

    private fun moveTo(kind: MotionKind, target: Vector2, speed: Float = 0f) {
        motionKind = kind
        motionTarget.set(target)
        motionSpeed = speed
    }

    private fun startShockWaves() {
        val offset = ArenaPerimeter.offsetOf(owner!!.arena, slamPlan.hit, slamPlan.surface)
        shockWaves.start(offset, -1f, attackTuning.waveLife)
        shockWaves.start(offset, 1f, attackTuning.waveLife)
        requestToPlaySound(SoundAsset.POUND_SOUND, loop = false)
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo SLAM_SHAKE_DUR,
                    ConstKeys.INTERVAL pairTo 0.1f,
                    ConstKeys.X pairTo 0f,
                    ConstKeys.Y pairTo 0.005f * ConstVals.PPM
                )
            )
        )
        GameLogger.debug(TAG, "startShockWaves(): hit=${slamPlan.hit}, surface=${slamPlan.surface}")
    }

    private fun spawnPillar(offset: Float) {
        val point = GameObjectPools.fetch(Vector2::class)
        val side = ArenaPerimeter.pointAt(owner!!.arena, offset, point)
        requestToPlaySound(SoundAsset.SHOCK_WAVE_HIT_SOUND, loop = false)
        val pillar = MegaEntityFactory.fetch(BigAssMaverickRobotOrbExplosion::class)!!
        pillar.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo point,
                ConstKeys.DIRECTION pairTo side.outward
            )
        )
    }

    private fun updateState(delta: Float) {
        val target = GameObjectPools.fetch(Vector2::class)
        when (state) {
            HandState.FOLLOW -> {
                val ownerCenter = owner!!.body.getCenter()
                body.setCenter(ownerCenter.x, ownerCenter.y)
                moveTo(MotionKind.STILL, ownerCenter)
            }

            HandState.MOVE_TO_ORIGIN -> {
                slotTarget(target)
                moveTo(MotionKind.TOWARD, target, MOVE_TO_ORIGIN_SPEED)
                if (arrived(target, ARRIVED)) {
                    body.setCenter(target.x, target.y)
                    moveTo(MotionKind.STILL, target)
                    goTo(HandState.REVEAL_ORBS)
                }
            }

            HandState.REVEAL_ORBS -> {
                moveTo(MotionKind.STILL, body.getCenter())
                orbRevealTimer.update(delta)
                if (orbRevealTimer.isJustFinished()) {
                    armOrbs[orbsRevealedCount].hidden = false
                    orbsRevealedCount++
                    requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
                    if (orbsRevealedCount >= armOrbs.size) goTo(HandState.IDLE) else orbRevealTimer.reset()
                }
            }

            HandState.IDLE -> {
                slotTarget(target).add(partnerOffset)
                val onSlot = owner!!.motionMode == MotionMode.ORBIT && partnerOffset.isZero &&
                    arrived(target, ARRIVED)
                if (onSlot) moveTo(MotionKind.EXACT, target)
                else moveTo(MotionKind.SPRING, target, BigAssMaverickRobotMotion.SPRING_MAX_SPEED)
            }

            HandState.WIND_UP -> {
                moveTo(MotionKind.SPRING, windUpTarget, BigAssMaverickRobotMotion.SPRING_MAX_SPEED)
                if (stateTime >= attackTuning.windUpDur) goTo(HandState.LAUNCH)
            }

            HandState.LAUNCH -> {
                val speed = UtilMethods.interpolate(
                    attackTuning.launchMinSpeed, attackTuning.launchMaxSpeed, 1f - owner!!.getHealthRatio()
                )
                moveTo(MotionKind.TOWARD, attackTarget, speed)
                if (arrived(attackTarget, ARRIVED)) goTo(HandState.RECOIL)
            }

            HandState.RECOIL -> {
                target.set(attackTarget)
                if (stateTime < RECOIL_OVERSHOOT_DUR)
                    target.add(attackDirection.x * RECOIL_OVERSHOOT * ConstVals.PPM, attackDirection.y * RECOIL_OVERSHOOT * ConstVals.PPM)
                if (stateTime < RECOIL_DUR) moveTo(MotionKind.SPRING, target, BigAssMaverickRobotMotion.SPRING_MAX_SPEED)
                else moveTo(MotionKind.STILL, body.getCenter())
                if (stateTime >= RECOIL_DUR + RECOIL_HOLD) goTo(HandState.RETURN)
            }

            HandState.RISE -> {
                moveTo(MotionKind.SPRING, attackTarget, BigAssMaverickRobotMotion.SPRING_MAX_SPEED)
                if (arrived(attackTarget, BigAssMaverickRobotMotion.ARRIVED_DISTANCE)) goTo(HandState.HANG)
            }

            HandState.HANG -> {
                moveTo(MotionKind.STILL, body.getCenter())
                if (stateTime >= attackTuning.lockDur) goTo(HandState.FALL)
            }

            HandState.FALL -> {
                moveTo(MotionKind.TOWARD, slamPlan.stop, FALL_SPEED)
                if (arrived(slamPlan.stop, ARRIVED)) {
                    body.setCenter(slamPlan.stop.x, slamPlan.stop.y)
                    moveTo(MotionKind.STILL, slamPlan.stop)
                    goTo(HandState.REST)
                }
            }

            HandState.REST -> {
                moveTo(MotionKind.STILL, body.getCenter())
                if (!secondWaveSpawned && attackTuning.shockWavePairs >= 2 && stateTime >= SECOND_WAVE_DELAY) {
                    secondWaveSpawned = true
                    startShockWaves()
                }
                if (stateTime >= attackTuning.restDur) goTo(HandState.RETURN)
            }

            HandState.CLAP_ALIGN ->
                moveTo(MotionKind.SPRING, attackTarget, BigAssMaverickRobotMotion.SPRING_MAX_SPEED)

            HandState.CLAP_LOCK, HandState.CLAP_IMPACT -> moveTo(MotionKind.STILL, body.getCenter())

            HandState.CLAP_CLOSE -> {
                moveTo(MotionKind.TOWARD, clapStop, attackTuning.clapSpeed)
                if (arrived(clapStop, ARRIVED)) {
                    body.setCenter(clapStop.x, clapStop.y)
                    moveTo(MotionKind.STILL, clapStop)
                }
            }

            HandState.RETURN -> {
                slotTarget(target)
                moveTo(MotionKind.SPRING, target, RETURN_MAX_SPEED)
                if (arrived(target, BigAssMaverickRobotMotion.ARRIVED_DISTANCE)) goTo(HandState.IDLE)
            }

            HandState.DEFEATED -> {
                val popsDue = (stateTime / CHAIN_POP_GAP).toInt() + 1
                while (chainPopped < armOrbs.size && chainPopped < popsDue) {
                    val orb = armOrbs[armOrbs.size - 1 - chainPopped]
                    if (orb.existenceState.liveAfterUpdate) orb.pop()
                    chainPopped++
                }
                blinkDelay.update(delta)
                if (blinkDelay.isFinished()) {
                    blink = !blink
                    blinkDelay.reset()
                }
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val fighting = owner?.bossComponent?.phase == BossPhase.FIGHT
        if (state != HandState.IDLE || fighting) stateTime += delta

        if (state != HandState.DEFEATED) for (i in 0 until armOrbs.size) {
            val scalar = i.toFloat() / armOrbs.size.toFloat()
            val center =
                UtilMethods.interpolate(armOrigin, body.getCenter(), scalar, GameObjectPools.fetch(Vector2::class))
            armOrbs[i].body.setCenter(center)
        }

        updateState(delta)

        val arena = owner?.arena
        if (arena != null) shockWaves.update(delta, 0f, ArenaPerimeter.length(arena)) { spawnPillar(it) }
    })

    private fun applyMotion(step: Float) {
        val velocity = body.physics.velocity
        val center = body.getCenter()
        when (motionKind) {
            MotionKind.STILL -> velocity.setZero()
            MotionKind.EXACT -> velocity.set(motionTarget).sub(center).scl(1f / step)
            MotionKind.SPRING -> BigAssMaverickRobotMotion.spring(
                center, velocity, motionTarget, motionSpeed, step, velocity
            )

            MotionKind.TOWARD -> {
                val distance = center.dst(motionTarget)
                val speed = motionSpeed * ConstVals.PPM
                if (distance <= speed * step) velocity.set(motionTarget).sub(center).scl(1f / step)
                else velocity.set(motionTarget).sub(center).nor().scl(speed)
            }
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(SHIELD_WIDTH * ConstVals.PPM, SHIELD_HEIGHT * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = (-body.getHeight() / 2f) + FEET_OVER_BOTTOM * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val step = services.settings.performance.fixedStep
            val defeated = state == HandState.DEFEATED
            if (defeated) {
                val onGround = body.isSensing(BodySense.FEET_ON_GROUND)
                body.physics.gravityOn = !onGround
                if (onGround && body.physics.velocity.y <= 0f) {
                    if (!bounced) {
                        bounced = true
                        body.physics.velocity.set(0f, DEFEAT_BOUNCE * ConstVals.PPM)
                        requestToPlaySound(SoundAsset.SMB3_THWOMP_SOUND, loop = false)
                    } else body.physics.velocity.setZero()
                }
            } else {
                body.physics.gravityOn = false
                applyMotion(step)
            }

            block?.let { block ->
                block.body.physics.velocity
                    .set(blockTarget)
                    .sub(block.body.getPositionPoint(BLOCK_POSITION))
                    .scl(1f / step)
            }

            val blockActive = state.platform
            block?.body?.let { blockBody ->
                blockBody.physics.collisionOn = blockActive
                blockBody.forEachFixture { it.setActive(blockActive) }
            }

            damagerFixture.setActive(state.damager)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            val position = Position.TOP_CENTER
            val point = when (block) {
                null -> body.getPositionPoint(position)
                else -> block!!.body.getPositionPoint(position)
            }
            sprite.setPosition(point, position)
            sprite.translateY(0.25f * ConstVals.PPM)
            if (state == HandState.WIND_UP || state == HandState.HANG || state == HandState.CLAP_LOCK)
                sprite.translateX(HandChoreography.shake(stateTime))
            sprite.hidden = state == HandState.DEFEATED && blink
        }
        .build()
        .also { component ->
            component.drawOffsetBody = {
                if (state == HandState.FOLLOW) owner!!.body else null
            }
        }

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    val tell = state == HandState.WIND_UP && stateTime >= attackTuning.windUpDur - ROCKET_TELL
                    if (state.rocket || tell) "rocket" else "still"
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.OTHER

    override fun defineTagsComponent() = TagsComponent(TAG)
}
