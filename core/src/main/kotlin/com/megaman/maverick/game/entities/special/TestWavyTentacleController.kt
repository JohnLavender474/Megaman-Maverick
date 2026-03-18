package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.maps.objects.RectangleMapObject
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
import com.megaman.maverick.game.utils.extensions.getCenter
import kotlin.math.sqrt

/**
 * Test harness that owns a [WavyTentacleOfJoints] child and replicates the full visual and
 * behavioural result of [TestTentacle] — wavy idle drift, lunge/pause/return state machine,
 * and pixel-stutter joint circles — but using [WavyTentacleOfJoints]'s external-control API
 * instead of the self-contained logic in [TestTentacle].
 *
 * This entity is responsible for:
 * 1. Spawning and destroying the child [WavyTentacleOfJoints].
 * 2. Computing and applying anchor/target drift each frame.
 * 3. Running the lunge state machine and calling [WavyTentacleOfJoints.setState].
 * 4. Reading joint positions from the child and drawing line segments + joint circles.
 */
class TestWavyTentacleController(game: MegamanMaverickGame) :
    MegaGameEntity(game), IDrawableShapesEntity, Updatable {

    companion object {
        const val TAG = "TestWavyTentacleController"

        private const val SEGMENT_COUNT = 6
        private const val JOINT_RADIUS = 0.5f * ConstVals.PPM
        private const val LINE_THICKNESS = 0.1f * ConstVals.PPM

        // Drift — mirrors TestTentacle
        private const val ANCHOR_DRIFT_RADIUS = 0.75f * ConstVals.PPM
        private const val ANCHOR_DRIFT_SPEED_X = 0.8f
        private const val ANCHOR_DRIFT_SPEED_Y = 0.57f
        private const val TARGET_DRIFT_RADIUS = 1.25f * ConstVals.PPM
        private const val TARGET_DRIFT_SPEED_X = 0.65f
        private const val TARGET_DRIFT_SPEED_Y = 0.43f

        // Lunge — mirrors TestTentacle
        private const val IDLE_DURATION = 10f
        private const val LUNGE_SPEED = 20f * ConstVals.PPM
        private const val LUNGE_PAUSE_DURATION = 0.4f
        private const val RETURN_SPEED = 5f * ConstVals.PPM

        // Pixel-stutter interval for joint circles
        private const val CIRCLE_UPDATE_INTERVAL = 0.05f
    }

    // --- Child tentacle ---

    private var tentacle: WavyTentacleOfJoints? = null
    private var tentacleSpawned = false

    // --- Drawable shapes ---

    private val lines = Array<GameLine>()
    private val circles = Array<GameCircle>()
    private var circleUpdateTimer = 0f

    // --- Time and drift ---

    private var time = 0f
    private val anchorOrigin = Vector2()
    private val targetOrigin = Vector2()
    private var anchorDriftPhaseX = 0f
    private var anchorDriftPhaseY = 0f
    private var targetDriftPhaseX = 0f
    private var targetDriftPhaseY = 0f

    // Computed each frame; passed to the child each frame
    private val currentAnchor = Vector2()
    private val currentIdleTarget = Vector2()

    // Tracks the tentacle tip position we are authoring (mirrors what is passed to setTarget)
    private val currentTipTarget = Vector2()

    // --- Lunge state machine ---

    private var idleTimer = 0f
    private var pauseTimer = 0f
    private val lungeTarget = Vector2()

    // Reusable scratch vector — never used to store state
    private val scratch = Vector2()

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
        idleTimer = 0f
        pauseTimer = 0f
        circleUpdateTimer = 0f

        val anchorPos = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        anchorOrigin.set(anchorPos)
        currentAnchor.set(anchorPos)

        val targetPos = spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!.rectangle.getCenter()
        targetOrigin.set(targetPos)
        currentIdleTarget.set(targetPos)
        currentTipTarget.set(targetPos)

        // Randomize drift phases so anchor and target never move in parallel
        anchorDriftPhaseX = MathUtils.random(0f, MathUtils.PI2)
        anchorDriftPhaseY = MathUtils.random(0f, MathUtils.PI2)
        targetDriftPhaseX = MathUtils.random(0f, MathUtils.PI2)
        targetDriftPhaseY = MathUtils.random(0f, MathUtils.PI2)

        // Spawn the child tentacle, anchored at the same bounds
        tentacle = MegaEntityFactory.fetch(WavyTentacleOfJoints::class)!!
        tentacle!!.spawn(
            props(
                ConstKeys.BOUNDS pairTo spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!,
                ConstKeys.COUNT pairTo SEGMENT_COUNT
            )
        )
        tentacle!!.setAnchor(currentAnchor)
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
    }

    // --- Per-frame update ---

    override fun update(delta: Float) {
        if (!tentacleSpawned && tentacle!!.spawned) {
            tentacleSpawned = true

            // Build drawables sized to match the child's joint count
            val jointCount = tentacle!!.jointCount
            lines.clear()
            circles.clear()
            repeat(SEGMENT_COUNT) {
                lines.add(GameLine().also { line ->
                    line.drawingColor = Color.GREEN
                    line.drawingShapeType = ShapeType.Filled
                    line.drawingRenderType = GameLine.GameLineRenderingType.RECT_LINE
                    line.drawingThickness = LINE_THICKNESS
                })
            }
            repeat(jointCount) {
                circles.add(GameCircle().also { circle ->
                    circle.drawingColor = Color.YELLOW
                    circle.drawingShapeType = ShapeType.Filled
                    circle.setRadius(JOINT_RADIUS)
                })
            }

            clearProdShapeSuppliers()
            for (line in lines) addProdShapeSupplier { line }
            for (circle in circles) addProdShapeSupplier { circle }
        }

        if (!tentacleSpawned) return

        time += delta

        // Update drifting anchor and idle-tip positions
        currentAnchor.x =
            anchorOrigin.x + MathUtils.sin(time * ANCHOR_DRIFT_SPEED_X + anchorDriftPhaseX) * ANCHOR_DRIFT_RADIUS
        currentAnchor.y =
            anchorOrigin.y + MathUtils.sin(time * ANCHOR_DRIFT_SPEED_Y + anchorDriftPhaseY) * ANCHOR_DRIFT_RADIUS
        currentIdleTarget.x =
            targetOrigin.x + MathUtils.sin(time * TARGET_DRIFT_SPEED_X + targetDriftPhaseX) * TARGET_DRIFT_RADIUS
        currentIdleTarget.y =
            targetOrigin.y + MathUtils.sin(time * TARGET_DRIFT_SPEED_Y + targetDriftPhaseY) * TARGET_DRIFT_RADIUS

        tentacle!!.setAnchor(currentAnchor)

        // Run the lunge state machine, pushing target updates to the child
        when (tentacle!!.getState()) {
            WavyTentacleOfJoints.TentacleState.IDLE -> {
                currentTipTarget.set(currentIdleTarget)
                tentacle!!.setTarget(currentTipTarget)

                idleTimer += delta
                if (idleTimer >= IDLE_DURATION) {
                    idleTimer = 0f
                    lungeTarget.set(game.megaman.body.getCenter())
                    tentacle!!.setState(WavyTentacleOfJoints.TentacleState.LUNGING)
                }
            }

            WavyTentacleOfJoints.TentacleState.LUNGING -> {
                val dx = lungeTarget.x - currentTipTarget.x
                val dy = lungeTarget.y - currentTipTarget.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val step = LUNGE_SPEED * delta

                if (dist <= step) {
                    currentTipTarget.set(lungeTarget)
                    pauseTimer = 0f
                    tentacle!!.setState(WavyTentacleOfJoints.TentacleState.PAUSING)
                } else {
                    currentTipTarget.x += dx / dist * step
                    currentTipTarget.y += dy / dist * step
                }
                tentacle!!.setTarget(currentTipTarget)
            }

            WavyTentacleOfJoints.TentacleState.PAUSING -> {
                pauseTimer += delta
                if (pauseTimer >= LUNGE_PAUSE_DURATION)
                    tentacle!!.setState(WavyTentacleOfJoints.TentacleState.RETURNING)
            }

            WavyTentacleOfJoints.TentacleState.RETURNING -> {
                val dx = currentIdleTarget.x - currentTipTarget.x
                val dy = currentIdleTarget.y - currentTipTarget.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val step = RETURN_SPEED * delta

                if (dist <= step) {
                    currentTipTarget.set(currentIdleTarget)
                    tentacle!!.setState(WavyTentacleOfJoints.TentacleState.IDLE)
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

        // Update circle positions at a fixed stutter interval for the retro pixel feel
        circleUpdateTimer += delta
        if (circleUpdateTimer >= CIRCLE_UPDATE_INTERVAL) {
            circleUpdateTimer -= CIRCLE_UPDATE_INTERVAL
            val jointCount = tentacle!!.jointCount
            for (i in 0 until jointCount) circles[i].setCenter(tentacle!!.getJoint(i, scratch))
        }
    }

    override fun getType() = EntityType.SPECIAL
}
