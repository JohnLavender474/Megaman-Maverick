package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.*
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.SmoothOscillationTimer
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.state.EnumStateMachineBuilder
import com.maverick.game.engine.state.StateMachine
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import com.megaman.maverick.game.entities.bosses.mechadragon.ArtBox
import com.megaman.maverick.game.entities.bosses.mechadragon.MechaDragonArt
import com.megaman.maverick.game.entities.bosses.mechadragon.MechaDragonDirector
import com.megaman.maverick.game.entities.bosses.mechadragon.MechaDragonMove
import com.megaman.maverick.game.entities.bosses.mechadragon.MechaDragonTuning
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.entities.hazards.LavaDrop
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.MechaDragonFlame
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.entities.projectiles.SpitFireball
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint

class MechaDragon(services: MegaServices) : AbstractBoss(services), IFreezableEntity, IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MechaDragon"

        private const val LEFT_HOVER_TARGET = "${ConstKeys.LEFT}_${ConstKeys.HOVER}_${ConstKeys.TARGET}"
        private const val RIGHT_HOVER_TARGET = "${ConstKeys.RIGHT}_${ConstKeys.HOVER}_${ConstKeys.TARGET}"

        private const val LEFT_CHARGE_TARGET = "${ConstKeys.LEFT}_${ConstKeys.CHARGE}_${ConstKeys.TARGET}"
        private const val RIGHT_CHARGE_TARGET = "${ConstKeys.RIGHT}_${ConstKeys.CHARGE}_${ConstKeys.TARGET}"

        private const val LEFT_LAND_TARGET = "${ConstKeys.LEFT}_${ConstKeys.LAND}_${ConstKeys.TARGET}"
        private const val RIGHT_LAND_TARGET = "${ConstKeys.RIGHT}_${ConstKeys.LAND}_${ConstKeys.TARGET}"

        private const val BLOCK_ON_SPAWN = "${ConstKeys.BLOCK}_${ConstKeys.ON}_${ConstKeys.SPAWN}"

        private const val INIT_DUR = 0.5f

        private const val HOVER_IN_PLACE_DUR = 2f
        private const val HOVER_SWAY_DUR = 2f
        private const val HOVER_SWAY_X = 1f

        private const val FLY_TO_TARGET_SPEED = 6f

        private const val TURNING_AROUND_DUR = 0.5f

        private const val CHARGE_SPEED = 8f
        private const val CHARGE_END_DELAY = 0.1f
        private const val CHARGE_TELL_DRIFT_SPEED = 0.5f

        private const val FIRE_DUR = 2f
        private const val FIRE_DUR_HARD = 1.5f
        private const val FIRE_TIME = 1f
        private const val FIRE_TIME_HARD = 0.75f
        private const val FIRE_MIN_DELAY = 0.5f
        private const val FIRE_MIN_DELAY_HARD = 0.25f
        private const val FIRE_MAX_DELAY = 1f
        private const val FIRE_MAX_DELAY_HARD = 0.5f
        private const val FIRE_SPEED = 10f
        private const val FIRE_ANGLE_DELTA = 75f

        private const val LAND_HANG_HEIGHT = 1.5f
        private const val LAND_DROP_SPEED = 12f
        private const val STAND_SETTLE_DUR = 0.3f
        private const val SHAKE_DUR = 0.5f
        private const val SHAKE_INTERVAL = 0.1f
        private const val SHAKE_Y = 0.005f

        private const val DRIP_TELL_DUR = 0.5f
        private const val DRIP_WALL_MARGIN = 0.5f

        private const val SWEEP_START_ANGLE = 30f
        private const val SWEEP_END_ANGLE = -45f
        private const val SWEEP_HOLD_DUR = 0.25f
        private const val BREATH_END_FRAME_DUR = 0.15f
        private const val BREATH_IDLE_DUR = 0.4f
        private const val FLAME_GAP = 0.05f
        private const val FLAME_SPEED = 11f

        private const val CHOMP_BURST_FORWARD = 0.75f
        private const val CHOMP_BURST_UP = 0.75f
        private const val POCKET_MIN_X = 1f
        private const val POCKET_MAX_X = 5f

        private const val TAKEOFF_RISE_SPEED = 8f

        private val LAND_CELL_DURS = gdxArrayOf(0.08f, 0.12f, 0.12f)
        private val TAKEOFF_CELL_DURS = gdxArrayOf(0.12f, 0.12f, 0.08f, 0.1f)
        private val CHOMP_STRIKE_DURS = gdxArrayOf(0.06f, 0.3f, 0.15f)
        private val END_CELL_DURS = gdxArrayOf(BREATH_END_FRAME_DUR, BREATH_END_FRAME_DUR)
        private val GROUND_STRIPS = gdxArrayOf("land", "stand", "breath_windup", "breath_sweep", "breath_end", "chomp")

        private val animDefs = orderedMapOf(
            "init" pairTo AnimationDef(),
            "fly" pairTo AnimationDef(3, 2, 0.1f, true),
            "fire1" pairTo AnimationDef(2, 5, 0.1f, false),
            "fire2" pairTo AnimationDef(3, 2, 0.1f, true),
            "turning" pairTo AnimationDef(5, 1, 0.1f, false),
            "charging" pairTo AnimationDef(3, 2, 0.05f, true),
            "defeated" pairTo AnimationDef(2, 2, 0.1f, true),
            "frozen" pairTo AnimationDef(),
        )
        private val regions = ObjectMap<String, TextureRegion>()

        private fun extractVector2(value: Any?) = (value as RectangleMapObject).rectangle.getCenter(Vector2())

        private fun sum(values: Array<Float>): Float {
            var total = 0f
            values.forEach { total += it }
            return total
        }
    }

    private enum class MechaDragonState {
        INIT, HOVER_IN_PLACE, FLY_TO_TARGET, CHARGE_TO_OTHER_SIDE, TURNING_AROUND, LAND, BREATH, CHOMP, TAKEOFF
    }

    private enum class RoomSide { LEFT, RIGHT }

    private enum class LandPhase { APPROACH, HANG, DROP, IMPACT }

    private enum class BreathPhase { WINDUP, SWEEP, HOLD, RETURN, END, IDLE }

    private enum class TakeoffPhase { CROUCH, RISE }

    private enum class SpriteLayout { AIR, GROUND, STANDING }

    private class DragonPart(
        val damageableShape: GameRectangle,
        val bodyShape: GameRectangle,
        val shieldShape: GameRectangle,
        val damagerShape: GameRectangle?,
        val fixtures: Array<Fixture>,
        val shield: Fixture
    ) {

        var enabled = true

        fun place(offsetX: Float, offsetY: Float, width: Float, height: Float) {
            damageableShape.setSize(width, height)
            shieldShape.setSize(width, height)
            damagerShape?.setSize(width, height)
            bodyShape.setSize(0.9f * width, 0.9f * height)
            fixtures.forEach { it.offsetFromBodyAttachment.set(offsetX, offsetY) }
            shield.offsetFromBodyAttachment.set(offsetX, offsetY)
        }
    }

    override lateinit var facing: Facing
    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            GameLogger.debug(TAG, "frozen: set=$value")
            if (value) {
                frozenTimer.reset()
                fireDelay.reset()
                fireTimer.setToEnd()
            } else frozenTimer.setToEnd()
        }

    private lateinit var stateMachine: StateMachine<MechaDragonState>
    private val currentState: MechaDragonState
        get() = stateMachine.getCurrentElement()
    private var plannedState = MechaDragonState.INIT
    private val stateTimers = OrderedMap<MechaDragonState, Timer>()

    private val frozenTimer = Timer(1f)

    private val hoverScalar = SmoothOscillationTimer(duration = HOVER_SWAY_DUR, start = -1f, end = 1f)

    private val leftSideHoverTargets = Array<Vector2>()
    private val rightSideHoverTargets = Array<Vector2>()
    private val reusableHoverTargetsSet = OrderedSet<Vector2>()

    private val leftSideChargeTargets = Array<Vector2>()
    private val rightSideChargeTargets = Array<Vector2>()

    private val leftLandTarget = Vector2()
    private val rightLandTarget = Vector2()

    private val min = Vector2()
    private val max = Vector2()
    private var ceilingY = 0f
    private val arena = GameRectangle()

    private val roomCenter = Vector2()

    private val currentTarget = Vector2()
    private val groundCenter = Vector2()

    private lateinit var chargeState: ProcessState
    private var chargeChance = 0
    private val beginChargeDelay = Timer()
    private val endChargeDelay = Timer(CHARGE_END_DELAY)

    private val fireDelay = Timer()
    private val fireTimer = Timer(FIRE_DUR)
    private var fireTime = FIRE_TIME
    private val firing: Boolean
        get() = !fireTimer.isFinished()

    private val blockIds = OrderedSet<Int>()

    private val tuning: MechaDragonTuning
        get() = MechaDragonTuning.of(hard = services.gameState.getDifficultyMode() == DifficultyMode.HARD, panic = panic)
    private var panic = false
    private var hoversThisAirCycle = 0
    private var breathsThisGroundCycle = 0

    private val phaseTimer = Timer()
    private var landPhase = LandPhase.APPROACH
    private var breathPhase = BreathPhase.WINDUP
    private var takeoffPhase = TakeoffPhase.CROUCH
    private var sweepAngle = SWEEP_START_ANGLE
    private val flameTimer = Timer(FLAME_GAP)
    private var chompBurstDone = false

    private val hangingDrips = Array<LavaDrop>()
    private val fallingDrips = Array<LavaDrop>()
    private val dripTimer = Timer(DRIP_TELL_DUR)

    private val windupDurs = Array<Float>()
    private val chompDurs = Array<Float>()
    private lateinit var windupAnimation: Animation
    private lateinit var chompAnimation: Animation

    private val parts = Array<DragonPart>()
    private lateinit var head: DragonPart
    private lateinit var neck: DragonPart
    private lateinit var torso: DragonPart
    private lateinit var tail1: DragonPart
    private lateinit var tail2: DragonPart
    private lateinit var legs: DragonPart

    private val randomRange: (Float, Float) -> Float = { from, to -> services.random.nextFloat(from, to) }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.MECHA_DRAGON.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
            GROUND_STRIPS.forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        if (stateTimers.isEmpty) stateTimers.putAll(
            MechaDragonState.INIT pairTo Timer(INIT_DUR),
            MechaDragonState.HOVER_IN_PLACE pairTo Timer(HOVER_IN_PLACE_DUR),
            MechaDragonState.TURNING_AROUND pairTo Timer(TURNING_AROUND_DUR)
        )
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        damageOverrides.put(ChargedShotExplosion::class, dmgNeg(1))
        damageOverrides.put(SmallIceCube::class, dmgNeg(4))
        damageOverrides.put(SlashWave::class, dmgNeg(3))
        damageOverrides.put(Axe::class, dmgNeg(3))
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ORB, false)
        super.onSpawn(spawnProps)

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setBottomCenterToPoint(spawn)

        val firstTarget = extractVector2(spawnProps.get("${ConstKeys.FIRST}_${ConstKeys.TARGET}"))
        currentTarget.set(firstTarget)

        facing = if (firstTarget.x < spawn.x) Facing.LEFT else Facing.RIGHT

        val roomCenter = extractVector2(spawnProps.get("${ConstKeys.ROOM}_${ConstKeys.CENTER}"))
        this.roomCenter.set(roomCenter)

        val min = extractVector2(spawnProps.get(ConstKeys.MIN))
        this.min.set(min)

        val maxObject = spawnProps.get(ConstKeys.MAX, RectangleMapObject::class)!!
        maxObject.rectangle.getCenter(max)
        ceilingY = maxObject.rectangle.y
        arena.set(this.min.x, this.min.y, max.x - this.min.x, max.y - this.min.y)

        readLandTarget(spawnProps.get(LEFT_LAND_TARGET, RectangleMapObject::class)!!, leftLandTarget)
        readLandTarget(spawnProps.get(RIGHT_LAND_TARGET, RectangleMapObject::class)!!, rightLandTarget)

        spawnProps.forEach { key, value ->
            key.toString().let {
                when {
                    it.contains(LEFT_HOVER_TARGET) -> leftSideHoverTargets.add(extractVector2(value))
                    it.contains(RIGHT_HOVER_TARGET) -> rightSideHoverTargets.add(extractVector2(value))
                    it.contains(LEFT_CHARGE_TARGET) -> leftSideChargeTargets.add(extractVector2(value))
                    it.contains(RIGHT_CHARGE_TARGET) -> rightSideChargeTargets.add(extractVector2((value)))
                    it.contains(ConstKeys.BLOCK) -> {
                        val id = (value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java)
                        blockIds.add(id)
                    }
                }
            }
        }

        stateMachine.reset()
        plannedState = MechaDragonState.INIT
        stateTimers.values().forEach { it.reset() }

        chargeState = ProcessState.BEGIN
        chargeChance = 0
        endChargeDelay.reset()

        val fireDur: Float
        val fireDelayDur: Float

        if (services.gameState.getDifficultyMode() == DifficultyMode.HARD) {
            fireDur = FIRE_DUR_HARD
            fireTime = FIRE_TIME_HARD
            fireDelayDur = FIRE_MAX_DELAY_HARD
        } else {
            fireDur = FIRE_DUR
            fireTime = FIRE_TIME
            fireDelayDur = FIRE_MAX_DELAY
        }

        fireDelay.resetDuration(fireDelayDur)
        fireTimer.resetDuration(fireDur)
        fireTimer.setToEnd()

        hoverScalar.reset()

        panic = false
        hoversThisAirCycle = 0
        breathsThisGroundCycle = 0
        landPhase = LandPhase.APPROACH
        breathPhase = BreathPhase.WINDUP
        takeoffPhase = TakeoffPhase.CROUCH
        sweepAngle = SWEEP_START_ANGLE
        chompBurstDone = false
        dripTimer.reset()

        val blockOnSpawn = spawnProps.get(BLOCK_ON_SPAWN, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        val block = MegaEntityFactory.fetch(Block::class)!!
        block.spawn(props(ConstKeys.BOUNDS pairTo blockOnSpawn))

        frozen = false

        GameLogger.debug(
            TAG, "onSpawn():\n" +
                "spawnProps=$spawnProps,\n" +
                "leftSideHoverTargets=$leftSideHoverTargets,\n" +
                "rightSideHoverTargets=$rightSideHoverTargets,\n" +
                "leftSideChargeSpots=$leftSideChargeTargets,\n" +
                "rightSideChargeSpots=$rightSideChargeTargets,\n" +
                "leftLandTarget=$leftLandTarget,\n" +
                "rightLandTarget=$rightLandTarget,\n" +
                "blockIds=$blockIds"
        )
    }

    override fun isReady(delta: Float) = stateTimers[MechaDragonState.INIT].isFinished()

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val oldHealth = getCurrentHealth()
        val damageTaken = super.takeDamageFrom(damager)

        GameLogger.debug(TAG, "takeDamageFrom(): health=${getCurrentHealth()}, oldHealth=$oldHealth, damager=$damager")

        if (damageTaken && damager is IFreezerEntity && !frozen) {
            GameLogger.debug(TAG, "takeDamageFrom(): hit by freezer entity")
            frozen = true
        }

        return damageTaken
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        leftSideHoverTargets.clear()
        rightSideHoverTargets.clear()

        leftSideChargeTargets.clear()
        rightSideChargeTargets.clear()

        hangingDrips.forEach { if (it.existenceState.liveAfterUpdate) it.destroy() }
        fallingDrips.forEach { if (it.existenceState.liveAfterUpdate) it.destroy() }
        hangingDrips.clear()
        fallingDrips.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            updateDrips(delta)

            if (defeated) {
                body.physics.velocity.setZero()
                explodeOnDefeat(delta)
                return@add
            }

            if (frozen) {
                body.physics.velocity.setZero()

                frozenTimer.update(delta)
                if (frozenTimer.isJustFinished()) {
                    damageTimer.reset()

                    IceShard.spawn5(body.getCenter())
                    IceShard.spawn5(body.getPositionPoint(Position.TOP_CENTER))
                }

                return@add
            }

            if (!panic && currentState != MechaDragonState.INIT &&
                getHealthRatio() <= MechaDragonTuning.PANIC_HEALTH_RATIO
            ) {
                GameLogger.debug(TAG, "update(): panic")
                panic = true
                requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
            }

            when (currentState) {
                MechaDragonState.INIT -> updateInit(delta)
                MechaDragonState.HOVER_IN_PLACE -> updateHover(delta)
                MechaDragonState.FLY_TO_TARGET -> {
                    val roomSide = getRoomSideOf(body.getCenter())
                    updateFacingByRoomSide(roomSide)

                    if (!isTargetReached(currentTarget)) {
                        flyToTarget(currentTarget, FLY_TO_TARGET_SPEED * ConstVals.PPM)
                        return@add
                    }

                    goTo(MechaDragonState.HOVER_IN_PLACE)
                }
                MechaDragonState.CHARGE_TO_OTHER_SIDE -> updateCharge(delta)
                MechaDragonState.TURNING_AROUND -> {
                    body.physics.velocity.setZero()

                    val stateTimer = stateTimers[MechaDragonState.TURNING_AROUND]
                    stateTimer.update(delta)
                    if (stateTimer.isFinished()) {
                        goTo(MechaDragonState.HOVER_IN_PLACE)
                        stateTimer.reset()
                    }
                }
                MechaDragonState.LAND -> updateLand(delta)
                MechaDragonState.BREATH -> updateBreath(delta)
                MechaDragonState.CHOMP -> updateChomp(delta)
                MechaDragonState.TAKEOFF -> updateTakeoff(delta)
            }
        }
    }

    private fun updateInit(delta: Float) {
        if (!isTargetReached(currentTarget)) {
            flyToTarget(currentTarget, FLY_TO_TARGET_SPEED * ConstVals.PPM)
            return
        }

        body.physics.velocity.setZero()

        val stateTimer = stateTimers[MechaDragonState.INIT]
        if (stateTimer.isAtBeginning()) requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
        stateTimer.update(delta)
        if (stateTimer.isFinished())
            goTo(if (shouldTurnAround()) MechaDragonState.TURNING_AROUND else MechaDragonState.HOVER_IN_PLACE)
    }

    private fun updateHover(delta: Float) {
        hoverScalar.update(delta)

        val velX = HOVER_SWAY_X * ConstVals.PPM * hoverScalar.getValue()
        body.physics.velocity.set(velX, 0f)

        val stateTimer = stateTimers[MechaDragonState.HOVER_IN_PLACE]
        stateTimer.update(delta)

        val fireTimeBefore = fireTimer.time
        fireTimer.update(delta)
        if (fireTimeBefore < fireTime && fireTimer.time >= fireTime) spitFireballs()
        if (fireTimer.isFinished()) fireDelay.update(delta)
        if (!stateTimer.isFinished() && fireDelay.isFinished()) {
            fireTimer.reset()

            val fireMinDelay: Float
            val fireMaxDelay: Float
            if (services.gameState.getDifficultyMode() == DifficultyMode.HARD) {
                fireMinDelay = FIRE_MIN_DELAY_HARD
                fireMaxDelay = FIRE_MAX_DELAY_HARD
            } else {
                fireMinDelay = FIRE_MIN_DELAY
                fireMaxDelay = FIRE_MAX_DELAY
            }

            val fireDelayDur = UtilMethods.interpolate(fireMinDelay, fireMaxDelay, getHealthRatio())
            fireDelay.resetDuration(fireDelayDur)
        }

        if (stateTimer.isFinished() && fireTimer.isFinished()) {
            stateTimer.reset()
            endHover()
        }
    }

    private fun endHover() {
        hoversThisAirCycle++
        val move = MechaDragonDirector.afterHover(hoversThisAirCycle, chargeChance, services.random)
        val oldChance = chargeChance
        chargeChance = MechaDragonDirector.chargeChanceAfter(move, chargeChance)
        GameLogger.debug(
            TAG, "endHover(): move=$move, hovers=$hoversThisAirCycle, chargeChance=$chargeChance, old=$oldChance"
        )
        goTo(
            when (move) {
                MechaDragonMove.CHARGE -> MechaDragonState.CHARGE_TO_OTHER_SIDE
                MechaDragonMove.LAND -> MechaDragonState.LAND
                else -> MechaDragonState.FLY_TO_TARGET
            }
        )
    }

    private fun updateCharge(delta: Float) {
        when (chargeState) {
            ProcessState.BEGIN -> {
                val target = body.getCenter().setY(currentTarget.y)
                flyToTarget(target, FLY_TO_TARGET_SPEED * ConstVals.PPM)

                if (isTargetReached(target)) {
                    chargeState = ProcessState.CONTINUE

                    body.physics.velocity.setZero()

                    val delay = UtilMethods.interpolate(
                        tuning.chargeTellMinDur,
                        tuning.chargeTellMaxDur,
                        1f - getHealthRatio()
                    )
                    beginChargeDelay.resetDuration(delay)
                    requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
                }
            }

            ProcessState.CONTINUE -> {
                beginChargeDelay.update(delta)
                if (!beginChargeDelay.isFinished())
                    body.physics.velocity.set(-CHARGE_TELL_DRIFT_SPEED * ConstVals.PPM * facing.value, 0f)
                else {
                    flyToTarget(currentTarget, CHARGE_SPEED * ConstVals.PPM)

                    if (isTargetReached(currentTarget)) {
                        chargeState = ProcessState.END
                        body.physics.velocity.setZero()
                    }
                }
            }

            ProcessState.END -> {
                endChargeDelay.update(delta)
                if (endChargeDelay.isFinished()) {
                    goTo(MechaDragonState.TURNING_AROUND)
                    endChargeDelay.reset()
                }
            }
        }
    }

    private fun updateLand(delta: Float) {
        when (landPhase) {
            LandPhase.APPROACH -> {
                val hang = GameObjectPools.fetch(Vector2::class)
                    .set(groundCenter.x, groundCenter.y + LAND_HANG_HEIGHT * ConstVals.PPM)
                if (!isTargetReached(hang)) {
                    flyToTarget(hang, FLY_TO_TARGET_SPEED * ConstVals.PPM)
                    return
                }
                body.setCenter(hang)
                body.physics.velocity.setZero()
                landPhase = LandPhase.HANG
                phaseTimer.resetDuration(tuning.landHangDur)
            }
            LandPhase.HANG -> {
                body.physics.velocity.setZero()
                phaseTimer.update(delta)
                if (phaseTimer.isFinished()) landPhase = LandPhase.DROP
            }
            LandPhase.DROP -> {
                body.physics.velocity.set(0f, -LAND_DROP_SPEED * ConstVals.PPM)
                if (body.getCenter().y <= groundCenter.y + LAND_DROP_SPEED * ConstVals.PPM * delta) {
                    body.setCenter(groundCenter)
                    body.physics.velocity.setZero()
                    stomp()
                    landPhase = LandPhase.IMPACT
                    phaseTimer.resetDuration(sum(LAND_CELL_DURS) + STAND_SETTLE_DUR)
                }
            }
            LandPhase.IMPACT -> {
                body.physics.velocity.setZero()
                phaseTimer.update(delta)
                if (phaseTimer.isFinished()) goTo(MechaDragonState.BREATH)
            }
        }
    }

    private fun stomp() {
        GameLogger.debug(TAG, "stomp()")
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
        services.events.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                    ConstKeys.DURATION pairTo SHAKE_DUR,
                    ConstKeys.X pairTo 0f,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )

        hangingDrips.forEach { if (it.existenceState.liveAfterUpdate) it.setToFall() }
        fallingDrips.addAll(hangingDrips)
        hangingDrips.clear()

        val minX = leftLandTarget.x - DRIP_WALL_MARGIN * ConstVals.PPM
        val maxX = rightLandTarget.x + DRIP_WALL_MARGIN * ConstVals.PPM
        MechaDragonDirector.AIMS.forEach { aim ->
            val point = OrbAim.point(
                aim,
                targetPlayer.body.getCenter(),
                targetPlayer.body.physics.velocity,
                MechaDragonDirector.AIM_LEAD_TIME,
                arena,
                randomRange,
                GameObjectPools.fetch(Vector2::class)
            )
            val spawn = GameObjectPools.fetch(Vector2::class).set(MathUtils.clamp(point.x, minX, maxX), ceilingY)

            val drop = MegaEntityFactory.fetch(LavaDrop::class)!!
            drop.spawn(props(ConstKeys.POSITION pairTo spawn))
            hangingDrips.add(drop)

            val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
            puff.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.DIRECTION pairTo Direction.DOWN
                )
            )
        }
        dripTimer.reset()
    }

    private fun updateDrips(delta: Float) {
        if (!hangingDrips.isEmpty) {
            dripTimer.update(delta)
            if (dripTimer.isFinished()) {
                hangingDrips.forEach { if (it.existenceState.liveAfterUpdate) it.setToFall() }
                fallingDrips.addAll(hangingDrips)
                hangingDrips.clear()
            }
        }

        val iter = fallingDrips.iterator()
        while (iter.hasNext()) {
            val drop = iter.next()
            if (!drop.existenceState.liveAfterUpdate) {
                iter.remove()
                continue
            }
            if (drop.body.getMaxY() < min.y) {
                drop.destroy()
                iter.remove()
            }
        }
    }

    private fun startBreath() {
        breathsThisGroundCycle++
        breathPhase = BreathPhase.WINDUP
        sweepAngle = SWEEP_START_ANGLE
        val windupDur = tuning.breathWindupDur
        windupAnimation.setFrameDuration(windupDur / windupAnimation.size())
        windupDurs.clear()
        for (i in 0 until windupAnimation.size()) windupDurs.add(windupDur / windupAnimation.size())
        phaseTimer.resetDuration(windupDur)
        requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
    }

    private fun updateBreath(delta: Float) {
        body.physics.velocity.setZero()
        phaseTimer.update(delta)

        when (breathPhase) {
            BreathPhase.WINDUP -> if (phaseTimer.isFinished()) {
                breathPhase = BreathPhase.SWEEP
                phaseTimer.resetDuration(tuning.sweepDur)
                flameTimer.setToEnd()
                requestToPlaySound(SoundAsset.FLAMETHROWER_SOUND, false)
            }
            BreathPhase.SWEEP -> {
                sweepAngle = MathUtils.lerp(SWEEP_START_ANGLE, SWEEP_END_ANGLE, phaseTimer.getRatio())
                emitFlames(delta)
                if (phaseTimer.isFinished()) {
                    breathPhase = BreathPhase.HOLD
                    phaseTimer.resetDuration(SWEEP_HOLD_DUR)
                }
            }
            BreathPhase.HOLD -> {
                sweepAngle = SWEEP_END_ANGLE
                emitFlames(delta)
                if (phaseTimer.isFinished()) {
                    if (tuning.returnSweep) {
                        breathPhase = BreathPhase.RETURN
                        phaseTimer.resetDuration(tuning.sweepDur)
                    } else endSweep()
                }
            }
            BreathPhase.RETURN -> {
                sweepAngle = MathUtils.lerp(SWEEP_END_ANGLE, SWEEP_START_ANGLE, phaseTimer.getRatio())
                emitFlames(delta)
                if (phaseTimer.isFinished()) endSweep()
            }
            BreathPhase.END -> if (phaseTimer.isFinished()) {
                breathPhase = BreathPhase.IDLE
                phaseTimer.resetDuration(BREATH_IDLE_DUR)
            }
            BreathPhase.IDLE -> if (phaseTimer.isFinished()) endBreath()
        }
    }

    private fun endSweep() {
        breathPhase = BreathPhase.END
        phaseTimer.resetDuration(sum(END_CELL_DURS))
    }

    private fun endBreath() {
        val pocket = players.anyLive { isInPocket(it.body.getCenter()) }
        val move = MechaDragonDirector.afterBreath(breathsThisGroundCycle, pocket, panic, services.random)
        GameLogger.debug(TAG, "endBreath(): move=$move, pocket=$pocket, breaths=$breathsThisGroundCycle")
        when (move) {
            MechaDragonMove.CHOMP -> goTo(MechaDragonState.CHOMP)
            MechaDragonMove.BREATH -> startBreath()
            else -> goTo(MechaDragonState.TAKEOFF)
        }
    }

    private fun isInPocket(point: Vector2): Boolean {
        val dx = (point.x - currentLandTarget().x) * facing.value
        return dx > POCKET_MIN_X * ConstVals.PPM && dx <= POCKET_MAX_X * ConstVals.PPM
    }

    private fun emitFlames(delta: Float) {
        flameTimer.update(delta)
        if (!flameTimer.isFinished()) return
        flameTimer.reset()

        val cell = MechaDragonArt.sweepCell(sweepAngle)
        val spawn = MechaDragonArt.pixelOffset(
            MechaDragonArt.SWEEP_MOUTHS[cell],
            MechaDragonArt.GROUND_CENTER_ROW,
            facing,
            GameObjectPools.fetch(Vector2::class)
        ).add(body.getCenter())
        val trajectory = GameObjectPools.fetch(Vector2::class).set(
            MathUtils.cosDeg(sweepAngle) * facing.value,
            MathUtils.sinDeg(sweepAngle)
        ).scl(FLAME_SPEED * ConstVals.PPM)

        val flame = MegaEntityFactory.fetch(MechaDragonFlame::class)!!
        flame.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.BLOCKS pairTo blockIds,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
    }

    private fun startChomp() {
        val tell = tuning.chompTellDur
        chompAnimation.setFrameDuration(0, tell)
        chompDurs.clear()
        chompDurs.add(tell)
        chompDurs.addAll(CHOMP_STRIKE_DURS)
        phaseTimer.resetDuration(sum(chompDurs))
        chompBurstDone = false
    }

    private fun updateChomp(delta: Float) {
        body.physics.velocity.setZero()
        phaseTimer.update(delta)

        if (!chompBurstDone && phaseTimer.time >= chompDurs[0]) {
            chompBurstDone = true
            requestToPlaySound(SoundAsset.CHOMP_SOUND, false)

            val burst = MechaDragonArt.pixelOffset(
                MechaDragonArt.CHOMP_MOUTH,
                MechaDragonArt.GROUND_CENTER_ROW,
                facing,
                GameObjectPools.fetch(Vector2::class)
            ).add(body.getCenter()).add(
                CHOMP_BURST_FORWARD * ConstVals.PPM * facing.value,
                CHOMP_BURST_UP * ConstVals.PPM
            )
            val explosion = MegaEntityFactory.fetch(MagmaExplosion::class)!!
            explosion.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo burst))
        }

        if (phaseTimer.isFinished()) goTo(MechaDragonState.TAKEOFF)
    }

    private fun updateTakeoff(delta: Float) {
        when (takeoffPhase) {
            TakeoffPhase.CROUCH -> {
                body.physics.velocity.setZero()
                phaseTimer.update(delta)
                if (phaseTimer.isFinished()) {
                    takeoffPhase = TakeoffPhase.RISE
                    requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)
                }
            }
            TakeoffPhase.RISE -> {
                if (!isTargetReached(currentTarget)) {
                    flyToTarget(currentTarget, TAKEOFF_RISE_SPEED * ConstVals.PPM)
                    return
                }
                body.physics.velocity.setZero()
                hoversThisAirCycle = 0
                goTo(MechaDragonState.HOVER_IN_PLACE)
            }
        }
    }

    private fun currentLandTarget() = if (facing == Facing.RIGHT) leftLandTarget else rightLandTarget

    private fun isGroundLayout() = when (currentState) {
        MechaDragonState.LAND -> landPhase != LandPhase.APPROACH
        MechaDragonState.BREATH, MechaDragonState.CHOMP -> true
        MechaDragonState.TAKEOFF -> takeoffPhase == TakeoffPhase.CROUCH
        else -> false
    }

    private fun groundHeadBox(): ArtBox = when (currentState) {
        MechaDragonState.BREATH -> when (breathPhase) {
            BreathPhase.WINDUP -> MechaDragonArt.WINDUP_HEAD_BOXES[MechaDragonArt.cellAt(windupDurs, phaseTimer.time)]
            BreathPhase.SWEEP, BreathPhase.HOLD, BreathPhase.RETURN ->
                MechaDragonArt.SWEEP_HEAD_BOXES[MechaDragonArt.sweepCell(sweepAngle)]
            BreathPhase.END -> MechaDragonArt.END_HEAD_BOXES[MechaDragonArt.cellAt(END_CELL_DURS, phaseTimer.time)]
            BreathPhase.IDLE -> MechaDragonArt.STAND_HEAD_BOX
        }
        MechaDragonState.CHOMP -> MechaDragonArt.CHOMP_HEAD_BOXES[MechaDragonArt.cellAt(chompDurs, phaseTimer.time)]
        else -> MechaDragonArt.STAND_HEAD_BOX
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 5f * ConstVals.PPM)
        body.drawingColor = Color.DARK_GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        head = definePart(body, debugShapes, circleDamager = false)
        neck = definePart(body, debugShapes, circleDamager = true)
        torso = definePart(body, debugShapes, circleDamager = false)
        tail1 = definePart(body, debugShapes, circleDamager = false)
        tail2 = definePart(body, debugShapes, circleDamager = false)
        legs = definePart(body, debugShapes, circleDamager = false)

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (isGroundLayout()) placeGroundParts() else placeAirParts()
            parts.forEach { part ->
                part.fixtures.forEach { it.setActive(part.enabled && !defeated) }
                part.shield.setActive(part.enabled && frozen && !defeated)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun definePart(body: Body, debugShapes: Array<() -> IDrawableShape?>, circleDamager: Boolean): DragonPart {
        val damageableShape = GameRectangle()
        val bodyShape = GameRectangle()
        val shieldShape = GameRectangle()
        val damagerRect = if (circleDamager) null else GameRectangle()

        val damager = Fixture(
            body,
            FixtureType.DAMAGER,
            damagerRect ?: GameCircle().setRadius(ConstVals.PPM.toFloat())
        )
        val damageable = Fixture(body, FixtureType.DAMAGEABLE, damageableShape)
        val bodyFixture = Fixture(body, FixtureType.BODY, bodyShape)
        val shield = Fixture(body, FixtureType.SHIELD, shieldShape)
        gdxArrayOf(damager, damageable, bodyFixture, shield).forEach { body.addFixture(it) }
        debugShapes.add { damager }

        val part = DragonPart(
            damageableShape, bodyShape, shieldShape, damagerRect, gdxArrayOf(damager, damageable, bodyFixture), shield
        )
        parts.add(part)
        return part
    }

    private fun placeAirParts() {
        val ppm = ConstVals.PPM.toFloat()
        val f = facing.value
        head.place(2.5f * ppm * f, 1.25f * ppm, 1.5f * ppm, 2.5f * ppm)
        neck.place(0.25f * ppm * f, 1.25f * ppm, ppm, ppm)
        torso.place(0f, 0f, 2.5f * ppm, 6f * ppm)
        tail1.place(-2f * ppm * f, -1.25f * ppm, 0.75f * ppm, 2f * ppm)
        tail2.place(-3f * ppm * f, -0.25f * ppm, ppm, 0.5f * ppm)
        head.enabled = true
        neck.enabled = true
        torso.enabled = true
        tail1.enabled = true
        tail2.enabled = true
        legs.enabled = false
    }

    private fun placeGroundParts() {
        placeGroundPart(head, groundHeadBox())
        placeGroundPart(torso, MechaDragonArt.GROUND_TORSO_BOX)
        placeGroundPart(tail1, MechaDragonArt.GROUND_TAIL_BOXES[0])
        placeGroundPart(tail2, MechaDragonArt.GROUND_TAIL_BOXES[1])
        placeGroundPart(legs, MechaDragonArt.GROUND_LEGS_BOX)
        val neckOffset = MechaDragonArt.pixelOffset(
            MechaDragonArt.GROUND_NECK_CENTER,
            MechaDragonArt.GROUND_CENTER_ROW,
            facing,
            GameObjectPools.fetch(Vector2::class)
        )
        neck.place(neckOffset.x, neckOffset.y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
        parts.forEach { it.enabled = true }
    }

    private fun placeGroundPart(part: DragonPart, box: ArtBox) {
        val offset = MechaDragonArt.boxOffset(
            box,
            MechaDragonArt.GROUND_CENTER_ROW,
            facing,
            GameObjectPools.fetch(Vector2::class)
        )
        val scale = MechaDragonArt.GAME_PX_PER_ART_PX
        part.place(offset.x, offset.y, box.width * scale, box.height * scale)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1)))
        .preProcess { _, sprite ->
            val center = body.getCenter()
            val layout = when {
                !isGroundLayout() -> SpriteLayout.AIR
                defeated || frozen -> SpriteLayout.STANDING
                else -> SpriteLayout.GROUND
            }
            val scale = MechaDragonArt.GAME_PX_PER_ART_PX
            val width = MechaDragonArt.CELL_WIDTH * scale
            val below = when (layout) {
                SpriteLayout.AIR -> MechaDragonArt.spriteBottomBelowCenter(
                    MechaDragonArt.AIR_CENTER_ROW, MechaDragonArt.AIR_CELL_HEIGHT
                )
                SpriteLayout.GROUND -> MechaDragonArt.spriteBottomBelowCenter(
                    MechaDragonArt.GROUND_CENTER_ROW, MechaDragonArt.GROUND_CELL_HEIGHT
                )
                SpriteLayout.STANDING -> MechaDragonArt.standingSpriteBottomBelowCenter(
                    if (defeated) MechaDragonArt.DEFEATED_FOOT_ROW else MechaDragonArt.FROZEN_FOOT_ROW
                )
            }
            val height = scale * if (layout == SpriteLayout.GROUND) MechaDragonArt.GROUND_CELL_HEIGHT
            else MechaDragonArt.AIR_CELL_HEIGHT
            sprite.setSize(width, height)
            sprite.setPosition(center.x - width / 2f, center.y - below)
            sprite.hidden = damageBlink && !frozen
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
        }
        .build()

    private fun animationKey(): String {
        if (defeated) return "defeated"
        if (frozen) return "frozen"
        return when (currentState) {
            MechaDragonState.INIT -> if (isTargetReached(currentTarget)) "init" else "fly"
            MechaDragonState.CHARGE_TO_OTHER_SIDE -> "charging"
            MechaDragonState.TURNING_AROUND -> "turning"
            MechaDragonState.LAND -> when (landPhase) {
                LandPhase.APPROACH -> "fly"
                LandPhase.HANG, LandPhase.DROP -> "land_hang"
                LandPhase.IMPACT -> if (phaseTimer.time < sum(LAND_CELL_DURS)) "land" else "stand"
            }
            MechaDragonState.BREATH -> when (breathPhase) {
                BreathPhase.WINDUP -> "breath_windup"
                BreathPhase.SWEEP, BreathPhase.HOLD, BreathPhase.RETURN ->
                    "breath_sweep_${MechaDragonArt.sweepCell(sweepAngle)}"
                BreathPhase.END -> "breath_end"
                BreathPhase.IDLE -> "stand"
            }
            MechaDragonState.CHOMP -> "chomp"
            MechaDragonState.TAKEOFF -> if (takeoffPhase == TakeoffPhase.CROUCH) "takeoff" else "fly"
            else -> when {
                firing -> if (fireTimer.time <= fireTime) "fire1" else "fire2"
                else -> "fly"
            }
        }
    }

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { animationKey() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, cols, durations, loop) = entry.value
                        try {
                            animations.put(key, Animation(regions[key], rows, cols, durations, loop))
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for key=$key", e)
                        }
                    }

                    val landDurs = gdxArrayOf(1f)
                    landDurs.addAll(LAND_CELL_DURS)
                    val land = Animation(regions["land"], 1, 4, landDurs, false)
                    animations.put("land_hang", land.slice(0, 1))
                    animations.put("land", land.slice(1, 4))
                    val takeoffDurs = Array(TAKEOFF_CELL_DURS)
                    takeoffDurs.reverse()
                    animations.put("takeoff", Animation(regions["land"], 1, 4, takeoffDurs, loop = false, reverse = true))
                    animations.put("stand", Animation(regions["stand"], 1, 2, 0.3f, true))

                    windupAnimation = Animation(regions["breath_windup"], 1, 3, 0.25f, false)
                    animations.put("breath_windup", windupAnimation)

                    val sweep = Animation(regions["breath_sweep"], 1, MechaDragonArt.SWEEP_ANGLES.size, 1f, false)
                    for (i in 0 until MechaDragonArt.SWEEP_ANGLES.size)
                        animations.put("breath_sweep_$i", sweep.slice(i, i + 1))

                    animations.put(
                        "breath_end", Animation(regions["breath_end"], 1, 2, BREATH_END_FRAME_DUR, false)
                    )

                    chompAnimation = Animation(regions["chomp"], 1, 4, 0.1f, false)
                    for (i in 0 until CHOMP_STRIKE_DURS.size) chompAnimation.setFrameDuration(i + 1, CHOMP_STRIKE_DURS[i])
                    animations.put("chomp", chompAnimation)
                }
                .build()
        )
        .build()

    private fun buildStateMachine(): StateMachine<MechaDragonState> {
        val builder = EnumStateMachineBuilder
            .create<MechaDragonState>()
            .initialState(MechaDragonState.INIT)
            .onChangeState(this::onChangeState)
        gdxArrayOf(
            MechaDragonState.INIT pairTo MechaDragonState.TURNING_AROUND,
            MechaDragonState.INIT pairTo MechaDragonState.HOVER_IN_PLACE,
            MechaDragonState.HOVER_IN_PLACE pairTo MechaDragonState.CHARGE_TO_OTHER_SIDE,
            MechaDragonState.HOVER_IN_PLACE pairTo MechaDragonState.FLY_TO_TARGET,
            MechaDragonState.HOVER_IN_PLACE pairTo MechaDragonState.LAND,
            MechaDragonState.CHARGE_TO_OTHER_SIDE pairTo MechaDragonState.TURNING_AROUND,
            MechaDragonState.TURNING_AROUND pairTo MechaDragonState.HOVER_IN_PLACE,
            MechaDragonState.FLY_TO_TARGET pairTo MechaDragonState.HOVER_IN_PLACE,
            MechaDragonState.LAND pairTo MechaDragonState.BREATH,
            MechaDragonState.BREATH pairTo MechaDragonState.CHOMP,
            MechaDragonState.BREATH pairTo MechaDragonState.TAKEOFF,
            MechaDragonState.CHOMP pairTo MechaDragonState.TAKEOFF,
            MechaDragonState.TAKEOFF pairTo MechaDragonState.HOVER_IN_PLACE
        ).forEach { (from, to) -> builder.transition(from, to) { plannedState == to } }
        return builder.build()
    }

    private fun goTo(state: MechaDragonState) {
        val previous = currentState
        plannedState = state
        stateMachine.next()
        if (currentState != state) throw IllegalStateException("$TAG: no transition from $previous to $state")
    }

    private fun onChangeState(current: MechaDragonState, previous: MechaDragonState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (current) {
            MechaDragonState.CHARGE_TO_OTHER_SIDE -> {
                val roomSide = getRoomSideOf(body.getCenter())
                val targets = when (roomSide) {
                    RoomSide.LEFT -> rightSideChargeTargets
                    RoomSide.RIGHT -> leftSideChargeTargets
                }
                var highest = targets[0]
                targets.forEach { if (it.y > highest.y) highest = it }
                currentTarget.set(highest)

                chargeState = ProcessState.BEGIN

                GameLogger.debug(TAG, "onChangeState(): target=$highest, roomSide=$roomSide")
            }
            MechaDragonState.FLY_TO_TARGET -> {
                val roomSide = getRoomSideOf(body.getCenter())
                pickHoverTarget(roomSide, excludeCurrent = true)
                GameLogger.debug(TAG, "onChangeState(): target=$currentTarget, roomSide=$roomSide")
            }
            MechaDragonState.HOVER_IN_PLACE -> {
                val roomSide = getRoomSideOf(body.getCenter())
                updateFacingByRoomSide(roomSide)

                fireDelay.reset()
                fireTimer.setToEnd(false)
            }
            MechaDragonState.LAND -> {
                updateFacingByRoomSide(getRoomSideOf(body.getCenter()))
                MechaDragonArt.groundCenter(currentLandTarget(), facing, groundCenter)
                landPhase = LandPhase.APPROACH
                breathsThisGroundCycle = 0
            }
            MechaDragonState.BREATH -> startBreath()
            MechaDragonState.CHOMP -> startChomp()
            MechaDragonState.TAKEOFF -> {
                takeoffPhase = TakeoffPhase.CROUCH
                phaseTimer.resetDuration(sum(TAKEOFF_CELL_DURS))
                pickHoverTarget(getRoomSideOf(body.getCenter()), excludeCurrent = false)
            }
            else -> {}
        }
    }

    private fun pickHoverTarget(roomSide: RoomSide, excludeCurrent: Boolean) {
        val candidateTargets = when (roomSide) {
            RoomSide.LEFT -> leftSideHoverTargets
            RoomSide.RIGHT -> rightSideHoverTargets
        }
        reusableHoverTargetsSet.addAll(candidateTargets)
        if (excludeCurrent) reusableHoverTargetsSet.remove(currentTarget)
        currentTarget.set(services.random.pick(reusableHoverTargetsSet.orderedItems()))
        reusableHoverTargetsSet.clear()
    }

    private fun shouldTurnAround() = getRoomSideOf(body.getCenter()).name == facing.name

    private fun flyToTarget(target: Vector2, speed: Float) {
        body.physics.velocity.set(target).sub(body.getCenter()).nor().scl(speed)
    }

    private fun isTargetReached(target: Vector2) = body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)

    private fun getRoomSideOf(target: Vector2) = if (target.x < roomCenter.x) RoomSide.LEFT else RoomSide.RIGHT

    private fun readLandTarget(mapObject: RectangleMapObject, out: Vector2) {
        mapObject.rectangle.getCenter(out)
        out.y = mapObject.rectangle.y
    }

    private fun spitFireballs() {
        val fireballsToLaunch = when {
            getHealthRatio() > 0.5f -> 1
            getHealthRatio() > 0.25f -> 2
            else -> 3
        }

        for (i in 0 until fireballsToLaunch) {
            val spawn = body.getCenter().add(3f * ConstVals.PPM * facing.value, 0f)

            val maxAngle: Float
            val minAngle: Float
            when (facing) {
                Facing.LEFT -> {
                    maxAngle = 90f + FIRE_ANGLE_DELTA
                    minAngle = 90f - FIRE_ANGLE_DELTA
                }

                Facing.RIGHT -> {
                    maxAngle = 270f + FIRE_ANGLE_DELTA
                    minAngle = 270f - FIRE_ANGLE_DELTA
                }
            }

            val aim = MechaDragonDirector.AIMS[i]
            val target = OrbAim.point(
                aim,
                targetPlayer.body.getCenter(),
                targetPlayer.body.physics.velocity,
                MechaDragonDirector.AIM_LEAD_TIME,
                arena,
                randomRange,
                GameObjectPools.fetch(Vector2::class)
            )

            var targetToSpawnAngle = target.sub(spawn).nor().angleDeg() - 90f
            if (targetToSpawnAngle < 0f) targetToSpawnAngle += 360f

            val angle = MathUtils.clamp(targetToSpawnAngle, minAngle, maxAngle)

            val trajectory = GameObjectPools.fetch(Vector2::class)
                .set(0f, FIRE_SPEED * ConstVals.PPM)
                .rotateDeg(angle)

            GameLogger.debug(TAG, "spitFireball(): spawn=$spawn, aim=$aim, angle=$angle, trajectory=$trajectory")

            val fireball = MegaEntityFactory.fetch(SpitFireball::class)!!
            fireball.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.BLOCKS pairTo blockIds,
                    ConstKeys.TRAJECTORY pairTo trajectory
                )
            )

            requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
        }
    }

    private fun updateFacingByRoomSide(roomSide: RoomSide) {
        val oldFacing = facing
        facing = if (roomSide == RoomSide.LEFT) Facing.RIGHT else Facing.LEFT

        if (oldFacing != facing) GameLogger.debug(
            TAG,
            "updateFacingByRoomSide(): roomSide=$roomSide, facing=$facing, oldFacing=$oldFacing"
        )
    }
}
