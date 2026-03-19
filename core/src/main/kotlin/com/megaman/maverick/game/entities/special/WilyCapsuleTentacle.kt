package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.special.WavyTentacleOfJoints.TentacleState
import com.megaman.maverick.game.utils.extensions.getCenter
import kotlin.math.sqrt

/**
 * Controller entity that owns a [WavyTentacleOfJoints] child and drives it through
 * an idle/lunge/pause/return cycle. The anchor position is set externally each frame
 * by the boss via [setAnchor]. The idle tip position is computed as anchor + [idleOffset]
 * plus a small sine drift for organic feel.
 */
class WilyCapsuleTentacle(game: MegamanMaverickGame) :
    MegaGameEntity(game), IDrawableShapesEntity, Updatable {

    companion object {
        const val TAG = "WilyCapsuleTentacle"

        private const val SEGMENT_COUNT = 6
        private const val JOINT_RADIUS = 0.5f * ConstVals.PPM
        private const val LINE_THICKNESS = 0.1f * ConstVals.PPM

        // Idle tip drift: sine base + random wander layered on top
        private const val TIP_DRIFT_RADIUS = 0.5f * ConstVals.PPM
        private const val TIP_DRIFT_SPEED_X = 0.65f
        private const val TIP_DRIFT_SPEED_Y = 0.43f

        // Random wander: the idle target smoothly drifts toward a random offset that is
        // re-rolled periodically, giving an organic, unpredictable sway
        private const val WANDER_RADIUS = 1.25f * ConstVals.PPM
        private const val WANDER_RETARGET_MIN = 1f
        private const val WANDER_RETARGET_MAX = 3f
        private const val WANDER_LERP_SPEED = 3f

        // How fast the drawn circles lerp toward their joint positions
        private const val CIRCLE_LERP_SPEED = 12f

        // Lunge movement constants (boss decides *when* to lunge; this class handles the motion)
        private const val LUNGE_SPEED = 18f * ConstVals.PPM
        private const val LUNGE_PAUSE_DURATION = 0.3f
        private const val RETURN_SPEED = 6f * ConstVals.PPM
    }

    // --- Child tentacle ---

    private var tentacle: WavyTentacleOfJoints? = null
    private var tentacleSpawned = false

    // --- Drawable shapes ---

    private val lines = Array<GameLine>()
    private val circles = Array<GameCircle>()
    private val circlePositions = Array<Vector2>()

    // --- Anchor and idle offset ---

    private val anchor = Vector2()
    private val idleOffset = Vector2()

    // --- Time and drift ---

    private var time = 0f
    private var tipDriftPhaseX = 0f
    private var tipDriftPhaseY = 0f

    // Random wander state
    private val wanderOffset = Vector2()
    private val wanderGoal = Vector2()
    private var wanderTimer = 0f
    private var wanderRetargetTime = 0f

    // Computed each frame
    private val currentIdleTarget = Vector2()
    private val currentTipTarget = Vector2()

    // --- Lunge state machine (motion only; boss triggers via lunge()) ---

    private var pauseTimer = 0f
    private val lungeTarget = Vector2()

    // Reusable scratch vector
    private val scratch = Vector2()

    // --- Public API ---

    fun setAnchor(v: Vector2) {
        anchor.set(v)
    }

    fun isIdle(): Boolean = tentacle?.getState() == TentacleState.IDLE

    fun lunge(target: Vector2) {
        if (tentacle == null || tentacle!!.getState() != TentacleState.IDLE) return
        lungeTarget.set(target)
        tentacle!!.setState(TentacleState.LUNGING)
    }

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
        pauseTimer = 0f

        val anchorPos = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        anchor.set(anchorPos)

        val offset = spawnProps.get(ConstKeys.OFFSET, Vector2::class)
        if (offset != null) idleOffset.set(offset) else idleOffset.set(0f, -3f * ConstVals.PPM)

        currentIdleTarget.set(anchor).add(idleOffset)
        currentTipTarget.set(currentIdleTarget)

        // Randomize drift phases
        tipDriftPhaseX = MathUtils.random(0f, MathUtils.PI2)
        tipDriftPhaseY = MathUtils.random(0f, MathUtils.PI2)

        // Initialize wander
        wanderOffset.setZero()
        rollWanderGoal()
        wanderTimer = 0f
        wanderRetargetTime = MathUtils.random(WANDER_RETARGET_MIN, WANDER_RETARGET_MAX)

        // Spawn the child tentacle
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
        circles.clear()
        circlePositions.clear()
    }

    // --- Per-frame update ---

    override fun update(delta: Float) {
        if (!tentacleSpawned && tentacle!!.spawned) {
            tentacleSpawned = true

            // Build drawables sized to match the child's joint count
            val jointCount = tentacle!!.jointCount
            lines.clear()
            circles.clear()
            circlePositions.clear()
            repeat(SEGMENT_COUNT) {
                lines.add(GameLine().also { line ->
                    line.drawingColor = Color.GREEN
                    line.drawingShapeType = ShapeType.Filled
                    line.drawingRenderType = GameLine.GameLineRenderingType.RECT_LINE
                    line.drawingThickness = LINE_THICKNESS
                })
            }
            repeat(jointCount) { i ->
                val pos = Vector2()
                tentacle!!.getJoint(i, pos)
                circlePositions.add(pos)
                circles.add(GameCircle().also { circle ->
                    circle.drawingColor = Color.YELLOW
                    circle.drawingShapeType = ShapeType.Filled
                    circle.setRadius(JOINT_RADIUS)
                    circle.setCenter(pos)
                })
            }

            clearProdShapeSuppliers()
            for (line in lines) addProdShapeSupplier { line }
            for (circle in circles) addProdShapeSupplier { circle }
        }

        if (!tentacleSpawned) return

        time += delta

        // Update random wander: smoothly lerp toward a random goal, re-roll periodically
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
                val dx = lungeTarget.x - currentTipTarget.x
                val dy = lungeTarget.y - currentTipTarget.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val step = LUNGE_SPEED * delta

                if (dist <= step) {
                    currentTipTarget.set(lungeTarget)
                    pauseTimer = 0f
                    tentacle!!.setState(TentacleState.PAUSING)
                } else {
                    currentTipTarget.x += dx / dist * step
                    currentTipTarget.y += dy / dist * step
                }
                tentacle!!.setTarget(currentTipTarget)
            }

            TentacleState.PAUSING -> {
                pauseTimer += delta
                if (pauseTimer >= LUNGE_PAUSE_DURATION)
                    tentacle!!.setState(TentacleState.RETURNING)
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

        // Update line segments every frame for smooth motion
        for (i in 0 until SEGMENT_COUNT) {
            lines[i].set(tentacle!!.getJoint(i, scratch), tentacle!!.getJoint(i + 1, Vector2()))
        }

        // Smoothly lerp circle positions toward their joint positions each frame
        val jointCount = tentacle!!.jointCount
        for (i in 0 until jointCount) {
            val jointPos = tentacle!!.getJoint(i, scratch)
            val circlePos = circlePositions[i]
            circlePos.x = MathUtils.lerp(circlePos.x, jointPos.x, CIRCLE_LERP_SPEED * delta)
            circlePos.y = MathUtils.lerp(circlePos.y, jointPos.y, CIRCLE_LERP_SPEED * delta)
            circles[i].setCenter(circlePos)
        }
    }

    private fun rollWanderGoal() {
        val angle = MathUtils.random(0f, MathUtils.PI2)
        val radius = MathUtils.random(0f, WANDER_RADIUS)
        wanderGoal.set(MathUtils.cos(angle) * radius, MathUtils.sin(angle) * radius)
    }

    override fun getType() = EntityType.SPECIAL
}
