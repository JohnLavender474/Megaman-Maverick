package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.special.WavyTentacleOfJoints
import com.megaman.maverick.game.entities.special.WavyTentacleOfJoints.TentacleState
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.sqrt

class WilyCapsuleTentacle(game: MegamanMaverickGame) :
    MegaGameEntity(game), IDrawableShapesEntity, Updatable {

    companion object {
        const val TAG = "WilyCapsuleTentacle"

        private const val SEGMENT_COUNT = 3
        private const val LINE_THICKNESS = 0.1f * ConstVals.PPM

        private const val DEFAULT_IDLE_OFFSET = 2f

        // Idle tip drift: sine base + random wander layered on top
        private const val TIP_DRIFT_RADIUS = 0.5f * ConstVals.PPM
        private const val TIP_DRIFT_SPEED_X = 0.65f
        private const val TIP_DRIFT_SPEED_Y = 0.43f

        // Random wander
        private const val WANDER_RADIUS = 1f * ConstVals.PPM
        private const val WANDER_RETARGET_MIN = 1f
        private const val WANDER_RETARGET_MAX = 3f
        private const val WANDER_LERP_SPEED = 3f

        // Joint lerp
        private const val JOINT_LERP_SPEED = 12f
        private const val JOINT_UPDATE_INTERVAL = 0.05f

        // Logging
        private const val LOG_INTERVAL = 2f

        // Lunge movement
        private const val LUNGE_SPEED = 10f * ConstVals.PPM
        private const val LUNGE_PAUSE_DURATION = 0.25f
        private const val RETURN_SPEED = 12f * ConstVals.PPM

        // Pin hold
        private const val PIN_DURATION = 0.5f

        // Multi-step: pull 2nd lunge slightly toward anchor so it's not directly at Mega Man
        private const val MULTI_STEP_PULL_TOWARD_ANCHOR = 0.2f

        // Lunge-past-and-swipe
        private const val LUNGE_PAST_OVERSHOOT = 3f
        private const val SWIPE_DISTANCE = 6f
        private const val SWIPE_HORIZONTAL_EXTEND = 3f
        private const val COIL_BACK_DISTANCE = 3f
        private const val COIL_BACK_SPEED = 12f * ConstVals.PPM
    }

    enum class LungeType { SIMPLE, MULTI_STEP, LUNGE_PAST_AND_SWIPE }

    // --- Child tentacle ---

    private var tentacle: WavyTentacleOfJoints? = null
    private var tentacleSpawned = false

    // --- Scissor tip entity ---

    private var scissor: WilyCapsuleTentacleScissor? = null

    // --- Drawable shapes ---

    private val lines = Array<GameLine>()

    // --- Joint entities ---

    private val jointPositions = Array<Vector2>()
    private val jointEntities = Array<WilyCapsuleTentacleJoint>()

    // --- Anchor and idle offset ---

    private val anchor = Vector2()
    private val idleOffset = Vector2()

    // --- Time and drift ---

    private var time = 0f
    private var tipDriftPhaseX = 0f
    private var tipDriftPhaseY = 0f

    private val wanderOffset = Vector2()
    private val wanderGoal = Vector2()
    private var wanderTimer = 0f
    private var wanderRetargetTime = 0f

    // Computed each frame
    private val currentIdleTarget = Vector2()
    private val currentTipTarget = Vector2()

    // --- Lunge state ---

    private var pauseTimer = 0f

    private val lungeTarget = Vector2()

    private var coilingBack = false
    private val coilBackTarget = Vector2()

    private var pinned = false
    private var pinTimer = 0f

    // Cycles SIMPLE → MULTI_STEP → LUNGE_PAST_AND_SWIPE → SIMPLE → …
    // setIndex(-1) before first use so that the first next() returns SIMPLE.
    private var currentLungeType = LungeType.SIMPLE

    // Phase 0 = first step, phase 1 = second step (for MULTI_STEP and LUNGE_PAST_AND_SWIPE)
    private var lungePhase = 0
    private var lungeCount = 0

    // Reusable scratch vector
    private val scratch = Vector2()

    // Logging timer
    private var logTimer = 0f

    // --- Lifecycle ---

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(DrawableShapesComponent())
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        time = 0f
        logTimer = 0f
        pauseTimer = 0f
        pinned = false
        pinTimer = 0f
        coilingBack = false

        lungePhase = 0
        lungeCount = 0
        currentLungeType = LungeType.SIMPLE

        val anchorPos = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        anchor.set(anchorPos)

        val offset = spawnProps.get(ConstKeys.OFFSET, Vector2::class)
        if (offset != null) idleOffset.set(offset) else idleOffset.set(0f, -DEFAULT_IDLE_OFFSET * ConstVals.PPM)

        currentIdleTarget.set(anchor).add(idleOffset)
        currentTipTarget.set(currentIdleTarget)

        tipDriftPhaseX = MathUtils.random(0f, MathUtils.PI2)
        tipDriftPhaseY = MathUtils.random(0f, MathUtils.PI2)

        wanderOffset.setZero()
        rollWanderGoal()
        wanderTimer = 0f
        wanderRetargetTime = MathUtils.random(WANDER_RETARGET_MIN, WANDER_RETARGET_MAX)

        tentacle = MegaEntityFactory.fetch(WavyTentacleOfJoints::class)!!
        tentacle!!.spawn(
            props(
                ConstKeys.BOUNDS pairTo spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!,
                ConstKeys.COUNT pairTo SEGMENT_COUNT
            )
        )
        tentacle!!.setAnchor(anchor)
        tentacle!!.setTarget(currentTipTarget)

        tentacleSpawned = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        tentacle?.destroy()
        tentacle = null

        lines.clear()

        jointPositions.forEach { GameObjectPools.free(it) }
        jointPositions.clear()

        for (joint in jointEntities) joint.destroy()
        jointEntities.clear()

        scissor?.destroy()
        scissor = null
    }

    // --- Public API ---

    fun setAnchor(v: Vector2): Vector2 = anchor.set(v)

    fun getAnchor(out: Vector2) = tentacle?.getAnchor(out)

    fun getTarget(out: Vector2) = tentacle?.getTarget(out)

    fun isIdle(): Boolean = tentacle?.getState() == TentacleState.IDLE

    fun getTentacleState(): TentacleState? = tentacle?.getState()

    fun getPenultimateJointPosition(out: Vector2): Vector2? {
        val jointCount = tentacle?.jointCount ?: return null
        if (jointCount < 2) return null
        return out.set(jointPositions[jointCount - 2])
    }

    fun lunge(
        target: Vector2 = megaman.body.getCenter(),
        lungeType: LungeType = when {
            lungeCount == 0 || lungeCount % 4 == 0 -> LungeType.entries.random()
            else -> LungeType.SIMPLE
        }
    ) {
        if (tentacle == null || tentacle!!.getState() != TentacleState.IDLE) return

        lungePhase = 0
        coilingBack = false
        currentLungeType = lungeType
        GameLogger.debug(TAG, "lunge(): type=$currentLungeType, target=$target")

        when (currentLungeType) {
            LungeType.SIMPLE -> lungeTarget.set(target)
            LungeType.MULTI_STEP -> lungeTarget.set(target)
            LungeType.LUNGE_PAST_AND_SWIPE -> {
                // Overshoot past the target in the anchor→target direction
                val dx = target.x - anchor.x
                val dy = target.y - anchor.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist > 0.0001f) {
                    val dirX = dx / dist
                    val dirY = dy / dist
                    lungeTarget.set(
                        target.x + dirX * LUNGE_PAST_OVERSHOOT * ConstVals.PPM,
                        target.y + dirY * LUNGE_PAST_OVERSHOOT * ConstVals.PPM
                    )
                } else lungeTarget.set(target)
                // Swipe direction is decided when the first pause ends
            }
        }

        tentacle!!.setState(TentacleState.LUNGING)
    }

    fun pin(target: Vector2 = megaman.body.getCenter()) {
        if (tentacle == null || tentacle!!.getState() != TentacleState.IDLE) {
            GameLogger.error(TAG, "pin(): failed to pin")
            return
        }

        pinned = true
        pinTimer = 0f

        lunge(target = target, lungeType = LungeType.SIMPLE)
    }

    fun explodeAndDestroy() {
        for (joint in jointEntities) {
            val explosion = MegaEntityFactory.fetch(Explosion::class)!!
            explosion.spawn(props(
                ConstKeys.POSITION pairTo joint.body.getCenter(),
                ConstKeys.DAMAGER pairTo false,
            ))
        }
        scissor?.let {
            val explosion = MegaEntityFactory.fetch(Explosion::class)!!
            explosion.spawn(props(
                ConstKeys.POSITION pairTo it.body.getCenter(),
                ConstKeys.DAMAGER pairTo false,
            ))
        }
        destroy()
    }

    // --- Per-frame update ---

    override fun update(delta: Float) {
        if (!tentacleSpawned && tentacle!!.spawned) {
            tentacleSpawned = true

            for (joint in jointEntities) joint.destroy()
            jointEntities.clear()

            repeat(SEGMENT_COUNT) {
                lines.add(GameLine().also { line ->
                    line.drawingColor = Color.GREEN
                    line.drawingShapeType = ShapeRenderer.ShapeType.Filled
                    line.drawingRenderType = GameLine.GameLineRenderingType.RECT_LINE
                    line.drawingThickness = LINE_THICKNESS
                })
            }

            val jointCount = tentacle!!.jointCount
            val lastJointIndex = jointCount - 1
            repeat(jointCount) { i ->
                val pos = GameObjectPools.fetch(Vector2::class, false)
                    .set(-100f, -100f).scl(ConstVals.PPM.toFloat())
                tentacle!!.getJoint(i, pos)
                jointPositions.add(pos)

                if (i == lastJointIndex) {
                    // Spawn scissor at the tip instead of a regular joint
                    val scissorEntity = MegaEntityFactory.fetch(WilyCapsuleTentacleScissor::class)!!
                    scissorEntity.owner = this
                    scissorEntity.spawn(props(ConstKeys.CENTER pairTo pos))
                    scissor = scissorEntity
                } else {
                    val jointEntity = MegaEntityFactory.fetch(WilyCapsuleTentacleJoint::class)!!
                    jointEntity.owner = this
                    jointEntity.spawn(props(ConstKeys.CENTER pairTo pos))
                    jointEntities.add(jointEntity)
                }
            }

            clearDebugShapeSuppliers()
            for (line in lines) addDebugShapeSupplier { line }

            for (i in 0 until SEGMENT_COUNT) lines[i].let {
                it.setFirstLocalPoint(tentacle!!.getJoint(i, scratch))
                it.setSecondLocalPoint(tentacle!!.getJoint(i + 1, scratch))
            }

            return
        }

        if (!tentacleSpawned) return

        if (jointEntities.any { !it.spawned })
            throw IllegalStateException("Joint entities should be spawned by this point")

        time += delta

        // Update random wander
        wanderTimer += delta
        if (wanderTimer >= wanderRetargetTime) {
            wanderTimer = 0f
            wanderRetargetTime = MathUtils.random(WANDER_RETARGET_MIN, WANDER_RETARGET_MAX)
            rollWanderGoal()
        }
        wanderOffset.x = MathUtils.lerp(wanderOffset.x, wanderGoal.x, WANDER_LERP_SPEED * delta)
        wanderOffset.y = MathUtils.lerp(wanderOffset.y, wanderGoal.y, WANDER_LERP_SPEED * delta)

        // Compute idle target = anchor + idleOffset + sine drift + random wander
        currentIdleTarget.set(anchor).add(idleOffset)
        currentIdleTarget.x += MathUtils.sin(time * TIP_DRIFT_SPEED_X + tipDriftPhaseX) * TIP_DRIFT_RADIUS
        currentIdleTarget.y += MathUtils.sin(time * TIP_DRIFT_SPEED_Y + tipDriftPhaseY) * TIP_DRIFT_RADIUS
        currentIdleTarget.add(wanderOffset)

        tentacle!!.setAnchor(anchor)

        // Run the lunge state machine
        when (tentacle!!.getState()) {
            TentacleState.IDLE -> {
                currentTipTarget.set(currentIdleTarget)
                tentacle!!.setTarget(currentTipTarget)
            }

            TentacleState.LUNGING -> {
                val target = if (coilingBack) coilBackTarget else lungeTarget
                val speed = if (coilingBack) COIL_BACK_SPEED else LUNGE_SPEED
                val dx = target.x - currentTipTarget.x
                val dy = target.y - currentTipTarget.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val step = speed * delta

                if (dist <= step) {
                    currentTipTarget.set(target)
                    if (coilingBack) {
                        coilingBack = false
                        // Stay in LUNGING — next frame will chase lungeTarget
                    } else {
                        pauseTimer = 0f
                        tentacle!!.setState(TentacleState.PAUSING)
                    }
                } else {
                    currentTipTarget.x += dx / dist * step
                    currentTipTarget.y += dy / dist * step
                }
                tentacle!!.setTarget(currentTipTarget)
            }

            TentacleState.PAUSING -> {
                if (pinned) {
                    pinTimer += delta
                    if (pinTimer >= PIN_DURATION) {
                        pinned = false
                        tentacle!!.setState(TentacleState.RETURNING)
                    }
                } else {
                    pauseTimer += delta
                    if (pauseTimer >= LUNGE_PAUSE_DURATION) {
                        pauseTimer = 0f
                        when (currentLungeType) {
                            LungeType.SIMPLE ->
                                tentacle!!.setState(TentacleState.RETURNING)

                            LungeType.MULTI_STEP -> {
                                if (lungePhase == 0) {
                                    // Fire the second lunge, pulled slightly toward the anchor
                                    // so it's not aimed perfectly at Mega Man
                                    lungePhase = 1
                                    val megaCenter = megaman.body.getCenter()
                                    lungeTarget.set(
                                        megaCenter.x + (anchor.x - megaCenter.x) * MULTI_STEP_PULL_TOWARD_ANCHOR,
                                        megaCenter.y + (anchor.y - megaCenter.y) * MULTI_STEP_PULL_TOWARD_ANCHOR
                                    )
                                    tentacle!!.setState(TentacleState.LUNGING)
                                } else tentacle!!.setState(TentacleState.RETURNING)
                            }

                            LungeType.LUNGE_PAST_AND_SWIPE -> {
                                if (lungePhase == 0) {
                                    // Coil back before swiping
                                    val swipeUp = megaman.body.getY() > currentTipTarget.y
                                    val swipeDir = if (swipeUp) 1f else -1f

                                    // Extend further horizontally away from the anchor during swipe
                                    val horizDir = if (currentTipTarget.x > anchor.x) 1f else -1f
                                    val horizExtend = horizDir * SWIPE_HORIZONTAL_EXTEND * ConstVals.PPM

                                    // Swipe target
                                    lungeTarget.set(
                                        currentTipTarget.x + horizExtend,
                                        currentTipTarget.y + swipeDir * SWIPE_DISTANCE * ConstVals.PPM
                                    )

                                    // Coil back target (opposite direction vertically, same horizontal extend)
                                    coilBackTarget.set(
                                        currentTipTarget.x + horizExtend * 0.5f,
                                        currentTipTarget.y - swipeDir * COIL_BACK_DISTANCE * ConstVals.PPM
                                    )
                                    coilingBack = true

                                    lungePhase = 1
                                    tentacle!!.setState(TentacleState.LUNGING)
                                } else tentacle!!.setState(TentacleState.RETURNING)
                            }
                        }
                    }
                }
            }

            TentacleState.RETURNING -> {
                val dx = currentIdleTarget.x - currentTipTarget.x
                val dy = currentIdleTarget.y - currentTipTarget.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val step = RETURN_SPEED * delta

                if (dist <= step) {
                    currentTipTarget.set(currentIdleTarget)
                    tentacle!!.setState(TentacleState.IDLE)
                } else {
                    currentTipTarget.x += dx / dist * step
                    currentTipTarget.y += dy / dist * step
                }
                tentacle!!.setTarget(currentTipTarget)
            }
        }

        logTimer += delta
        if (logTimer >= LOG_INTERVAL) {
            logTimer = 0f
            val sb = StringBuilder("update(): jointEntities.size=${jointEntities.size}")
            for (i in 0 until jointEntities.size) {
                val center = jointEntities[i].body.getCenter()
                sb.append(", joint[$i]=(${center.x}, ${center.y})")
            }
            GameLogger.debug(TAG, sb.toString())
        }

        for (i in 0 until SEGMENT_COUNT) lines[i].let {
            it.setFirstLocalPoint(tentacle!!.getJoint(i, scratch))
            it.setSecondLocalPoint(tentacle!!.getJoint(i + 1, scratch))
        }

        val jointCount = tentacle!!.jointCount
        val lastJointIndex = jointCount - 1
        for (i in 0 until jointCount) {
            val jointPos = tentacle!!.getJoint(i, scratch)

            val lerpedPos = jointPositions[i]
            lerpedPos.x = MathUtils.lerp(lerpedPos.x, jointPos.x, JOINT_LERP_SPEED * JOINT_UPDATE_INTERVAL)
            lerpedPos.y = MathUtils.lerp(lerpedPos.y, jointPos.y, JOINT_LERP_SPEED * JOINT_UPDATE_INTERVAL)

            if (i == lastJointIndex) scissor?.body?.setCenter(lerpedPos)
            else jointEntities[i].body.setCenter(lerpedPos)
        }
    }

    private fun rollWanderGoal() {
        val angle = MathUtils.random(0f, MathUtils.PI2)
        val radius = MathUtils.random(0f, WANDER_RADIUS)
        wanderGoal.set(MathUtils.cos(angle) * radius, MathUtils.sin(angle) * radius)
    }

    override fun getType() = EntityType.SPECIAL
}
