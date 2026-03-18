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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.special.TestTentacle.Companion.CIRCLE_UPDATE_INTERVAL
import com.megaman.maverick.game.entities.special.TestTentacle.Companion.IDLE_DURATION
import com.megaman.maverick.game.entities.special.TestTentacle.Companion.LUNGE_PAUSE_DURATION
import com.megaman.maverick.game.entities.special.TestTentacle.Companion.LUNGE_SPEED
import com.megaman.maverick.game.entities.special.TestTentacle.Companion.RETURN_SPEED
import com.megaman.maverick.game.utils.extensions.getCenter
import kotlin.math.sqrt

/**
 * A single tentacle entity intended to be used as a visual/hazard appendage of a larger boss or
 * environmental object. The tentacle is composed of [segmentCount] line segments connected by
 * spherical joint markers, producing a chain of [segmentCount + 1] joints total (joint 0 is the
 * fixed [anchor], joint N is the moving [target]).
 *
 * ---
 *
 * ## Structure
 *
 * Each frame the tentacle rebuilds all joint positions from scratch using the current [anchor] and
 * [target] vectors. The joint positions are never accumulated over time — only the running [time]
 * accumulator and the state-machine variables are stateful.
 *
 * The segment length is NOT fixed. Instead, it is recomputed every frame as:
 *
 *     segmentLength = distance(anchor, target) / segmentCount
 *
 * This means the tentacle automatically expands and contracts as the anchor and target move apart
 * or closer together.
 *
 * ---
 *
 * ## Joint position math (idle / returning)
 *
 * For each joint i in [0, segmentCount], a normalized parameter t ∈ [0, 1] is computed:
 *
 *     t = i / segmentCount
 *
 * The **base position** is a simple linear interpolation along the anchor→target direction:
 *
 *     dir  = normalize(target - anchor)
 *     base = anchor + dir * t * distance(anchor, target)
 *
 * A **perpendicular offset** (the wave) is then added at right-angles to dir:
 *
 *     perp = (-dir.y, dir.x)          // 90-degree CCW rotation of dir
 *
 *     envelope = sin(t * π)           // bell curve: 0 at both ends, 1 at the midpoint
 *     amplitude = waveAmplitude * segmentLength * waveBlend
 *     wave      = sin(time * waveSpeed + i * wavePhaseOffset) * amplitude * envelope
 *
 *     joint[i] = base + perp * wave
 *
 * Key properties of this formula:
 * - The **sine term** `sin(time * waveSpeed + i * wavePhaseOffset)` advances the phase of each
 *   joint independently over time, creating a travelling-wave / ripple effect along the tentacle.
 * - The **envelope** `sin(t * π)` ensures both endpoints are always pinned (wave = 0 at i=0 and
 *   i=segmentCount) while intermediate joints receive the most lateral displacement near the middle.
 * - The **amplitude** scales with `segmentLength` so the wave looks proportionally the same
 *   regardless of how far apart the anchor and target are.
 * - After the loop, joints[0] and joints[segmentCount] are force-set to [anchor] and [target]
 *   respectively, eliminating any floating-point drift at the endpoints.
 *
 * ---
 *
 * ## State machine
 *
 * The tentacle cycles through four states:
 *
 * ### IDLE
 * Normal wavy behavior. [waveBlend] lerps toward 1 (full wave). After [IDLE_DURATION] seconds,
 * Megaman's current world-space center is sampled once as [lungeTarget] and the state advances
 * to LUNGING.
 *
 * ### LUNGING
 * [waveBlend] lerps toward 0, smoothly straightening the tentacle into a rigid spike aimed at
 * [lungeTarget]. The [target] point moves toward [lungeTarget] at [LUNGE_SPEED] units/sec. Once
 * [target] arrives, [waveBlend] is clamped to 0 and the state advances to PAUSING.
 *
 * ### PAUSING
 * The tentacle holds fully extended and straight at [lungeTarget] for [LUNGE_PAUSE_DURATION]
 * seconds before advancing to RETURNING.
 *
 * ### RETURNING
 * [waveBlend] lerps back toward 1 as [target] moves back toward [idleTarget] at [RETURN_SPEED]
 * units/sec (deliberately slower than the lunge for dramatic effect). Once [target] reaches
 * [idleTarget] the state resets to IDLE.
 *
 * ---
 *
 * ## Wave blend transitions
 *
 * The [waveBlend] scalar in [0, 1] drives the effective wave amplitude:
 *
 *     waveBlend' = lerp(waveBlend, targetBlend, WAVE_BLEND_SPEED * delta)
 *
 * where targetBlend is 1 during IDLE/RETURNING and 0 during LUNGING/PAUSING. This exponential
 * lerp means the joint spheres smoothly drift toward the straight anchor→target line before the
 * lunge and ease back into their wavy idle positions after the return, avoiding the visual snap
 * that would result from a hard amplitude switch.
 *
 * ---
 *
 * ## Anchor and target drift
 *
 * Both the [anchor] and the idle tip position drift continuously around their spawn-time origins
 * ([anchorOrigin] and [targetOrigin]) using independent two-axis sine oscillators:
 *
 *     anchor.x = anchorOrigin.x + sin(time * ANCHOR_DRIFT_SPEED_X + phaseX) * ANCHOR_DRIFT_RADIUS
 *     anchor.y = anchorOrigin.y + sin(time * ANCHOR_DRIFT_SPEED_Y + phaseY) * ANCHOR_DRIFT_RADIUS
 *
 * The X and Y speeds are intentionally incommensurable (e.g. 0.8 and 0.57) so the path traces an
 * aperiodic Lissajous-like curve rather than a simple back-and-forth line. The anchor and target
 * each have their own set of speeds, and all four phase offsets ([anchorDriftPhaseX],
 * [anchorDriftPhaseY], [targetDriftPhaseX], [targetDriftPhaseY]) are randomised on every spawn,
 * ensuring the two endpoints never drift in parallel with each other or with themselves across
 * multiple spawns.
 *
 * During IDLE, [target] is set directly to [idleTarget] each frame so the tip follows the drift.
 * During RETURNING the tip moves toward the drifting [idleTarget], so the return destination
 * shifts naturally rather than snapping back to a fixed point.
 *
 * ---
 *
 * ## Rendering
 *
 * Line segments update every frame for smooth movement. Joint sphere positions update on a fixed
 * [CIRCLE_UPDATE_INTERVAL] timer to produce a deliberate pixel-game stutter that gives the joints
 * a retro, chunky feel distinct from the continuous line motion.
 *
 * ---
 *
 * ## Spawn props
 *
 * | Key              | Type               | Default                  | Description                        |
 * |------------------|--------------------|--------------------------|------------------------------------|
 * | `bounds`         | GameRectangle      | required                 | Center becomes the anchor point    |
 * | `target`         | RectangleMapObject | required                 | Center becomes the idle target     |
 * | `count`          | Int                | 6                        | Number of segments                 |
 * | `radius`         | Float              | 0.5 * PPM                | Joint sphere radius                |
 * | `line_thickness` | Float              | 0.1 * PPM                | Rendered line thickness            |
 * | `speed`          | Float              | 3.0                      | Wave animation speed (rad/sec)     |
 * | `wave_amplitude` | Float              | 0.5                      | Wave amplitude scalar              |
 * | `wave_phase_offset` | Float           | 1.25                     | Per-joint phase offset (radians)   |
 */
class TestTentacle(game: MegamanMaverickGame) : MegaGameEntity(game), IDrawableShapesEntity, Updatable {

    companion object {
        const val TAG = "TestTentacle"

        private const val DEFAULT_SEGMENT_COUNT = 6
        private const val DEFAULT_JOINT_RADIUS = 0.5f * ConstVals.PPM
        private const val DEFAULT_LINE_THICKNESS = 0.1f * ConstVals.PPM

        private const val DEFAULT_WAVE_SPEED = 3f
        private const val DEFAULT_WAVE_AMPLITUDE = 0.5f
        private const val DEFAULT_WAVE_PHASE_OFFSET = 1.25f

        private const val CIRCLE_UPDATE_INTERVAL = 0.05f

        private const val IDLE_DURATION = 10f
        private const val LUNGE_SPEED = 20f * ConstVals.PPM
        private const val LUNGE_PAUSE_DURATION = 0.4f
        private const val RETURN_SPEED = 5f * ConstVals.PPM

        private const val WAVE_BLEND_SPEED = 5f

        // Anchor drifts on independent X/Y frequencies to trace a Lissajous-like path
        private const val ANCHOR_DRIFT_RADIUS = 0.75f * ConstVals.PPM
        private const val ANCHOR_DRIFT_SPEED_X = 0.8f
        private const val ANCHOR_DRIFT_SPEED_Y = 0.57f   // irrational ratio → never repeats simply

        // Target drifts with different speeds so it is never in sync with the anchor
        private const val TARGET_DRIFT_RADIUS = 1.25f * ConstVals.PPM
        private const val TARGET_DRIFT_SPEED_X = 0.65f
        private const val TARGET_DRIFT_SPEED_Y = 0.43f
    }

    private enum class TentacleState { IDLE, LUNGING, PAUSING, RETURNING }

    var anchor = Vector2()
        private set
    var target = Vector2()
        private set

    var segmentCount = DEFAULT_SEGMENT_COUNT
        private set
    var jointRadius = DEFAULT_JOINT_RADIUS
        private set
    var lineThickness = DEFAULT_LINE_THICKNESS
        private set

    var waveSpeed = DEFAULT_WAVE_SPEED
    var waveAmplitude = DEFAULT_WAVE_AMPLITUDE
    var wavePhaseOffset = DEFAULT_WAVE_PHASE_OFFSET

    private var joints = Array<Vector2>()
    private var lines = Array<GameLine>()
    private var circles = Array<GameCircle>()

    private var state = TentacleState.IDLE

    private var time = 0f
    private var circleUpdateTimer = 0f

    private var idleTimer = 0f
    private var pauseTimer = 0f
    private val anchorOrigin = Vector2()
    private val targetOrigin = Vector2()
    private val idleTarget = Vector2()
    private val lungeTarget = Vector2()
    private var waveBlend = 1f

    // Per-axis phase offsets randomized on each spawn so anchor and target
    // drift independently and are never visually parallel to each other
    private var anchorDriftPhaseX = 0f
    private var anchorDriftPhaseY = 0f
    private var targetDriftPhaseX = 0f
    private var targetDriftPhaseY = 0f

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
        circleUpdateTimer = 0f

        idleTimer = 0f
        pauseTimer = 0f

        state = TentacleState.IDLE

        waveBlend = 1f

        segmentCount = spawnProps.getOrDefault(ConstKeys.COUNT, DEFAULT_SEGMENT_COUNT, Int::class)
        jointRadius = spawnProps.getOrDefault(ConstKeys.RADIUS, DEFAULT_JOINT_RADIUS, Float::class)
        lineThickness = spawnProps.getOrDefault("line_thickness", DEFAULT_LINE_THICKNESS, Float::class)

        waveSpeed = spawnProps.getOrDefault(ConstKeys.SPEED, DEFAULT_WAVE_SPEED, Float::class)
        waveAmplitude = spawnProps.getOrDefault("wave_amplitude", DEFAULT_WAVE_AMPLITUDE, Float::class)
        wavePhaseOffset = spawnProps.getOrDefault("wave_phase_offset", DEFAULT_WAVE_PHASE_OFFSET, Float::class)

        val anchorPos = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        anchor.set(anchorPos)
        anchorOrigin.set(anchorPos)

        val targetPos = spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!.rectangle.getCenter()
        target.set(targetPos)
        idleTarget.set(targetPos)
        targetOrigin.set(targetPos)

        anchorDriftPhaseX = MathUtils.random(0f, MathUtils.PI2)
        anchorDriftPhaseY = MathUtils.random(0f, MathUtils.PI2)
        targetDriftPhaseX = MathUtils.random(0f, MathUtils.PI2)
        targetDriftPhaseY = MathUtils.random(0f, MathUtils.PI2)

        val jointCount = segmentCount + 1
        (0 until jointCount).forEach { _ ->
            joints.add(Vector2())
            circles.add(GameCircle())
        }
        (0 until segmentCount).forEach { _ ->
            lines.add(GameLine())
        }

        for (line in lines) {
            line.drawingColor = Color.GREEN
            line.drawingShapeType = ShapeType.Filled
            line.drawingRenderType = GameLine.GameLineRenderingType.RECT_LINE
            line.drawingThickness = lineThickness
        }

        for (circle in circles) {
            circle.drawingColor = Color.YELLOW
            circle.drawingShapeType = ShapeType.Filled
            circle.setRadius(jointRadius)
        }

        updateJoints(0f)

        clearProdShapeSuppliers()
        for (line in lines) addProdShapeSupplier { line }
        for (circle in circles) addProdShapeSupplier { circle }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        lines.clear()
        joints.clear()
        circles.clear()
    }

    override fun update(delta: Float) {
        time += delta

        // Drift anchor and idle target around their spawn origins with independent Lissajous paths.
        // Different X/Y frequencies per point mean neither axis nor either endpoint stays in sync.
        anchor.x = anchorOrigin.x + MathUtils.sin(time * ANCHOR_DRIFT_SPEED_X + anchorDriftPhaseX) * ANCHOR_DRIFT_RADIUS
        anchor.y = anchorOrigin.y + MathUtils.sin(time * ANCHOR_DRIFT_SPEED_Y + anchorDriftPhaseY) * ANCHOR_DRIFT_RADIUS
        idleTarget.x = targetOrigin.x + MathUtils.sin(time * TARGET_DRIFT_SPEED_X + targetDriftPhaseX) * TARGET_DRIFT_RADIUS
        idleTarget.y = targetOrigin.y + MathUtils.sin(time * TARGET_DRIFT_SPEED_Y + targetDriftPhaseY) * TARGET_DRIFT_RADIUS

        when (state) {
            TentacleState.IDLE -> {
                waveBlend = MathUtils.lerp(waveBlend, 1f, WAVE_BLEND_SPEED * delta)
                target.set(idleTarget)
                idleTimer += delta
                if (idleTimer >= IDLE_DURATION) {
                    idleTimer = 0f
                    lungeTarget.set(game.megaman.body.getCenter())
                    state = TentacleState.LUNGING
                }
            }

            TentacleState.LUNGING -> {
                waveBlend = MathUtils.lerp(waveBlend, 0f, WAVE_BLEND_SPEED * delta)
                val dx = lungeTarget.x - target.x
                val dy = lungeTarget.y - target.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val step = LUNGE_SPEED * delta
                if (dist <= step) {
                    target.set(lungeTarget)
                    waveBlend = 0f
                    pauseTimer = 0f
                    state = TentacleState.PAUSING
                } else {
                    target.x += dx / dist * step
                    target.y += dy / dist * step
                }
            }

            TentacleState.PAUSING -> {
                pauseTimer += delta
                if (pauseTimer >= LUNGE_PAUSE_DURATION) state = TentacleState.RETURNING
            }

            TentacleState.RETURNING -> {
                waveBlend = MathUtils.lerp(waveBlend, 1f, WAVE_BLEND_SPEED * delta)
                val dx = idleTarget.x - target.x
                val dy = idleTarget.y - target.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val step = RETURN_SPEED * delta
                if (dist <= step) {
                    target.set(idleTarget)
                    state = TentacleState.IDLE
                } else {
                    target.x += dx / dist * step
                    target.y += dy / dist * step
                }
            }
        }

        updateJoints(delta)
    }

    private fun updateJoints(delta: Float) {
        val jointCount = joints.size
        if (jointCount < 2) return

        val dx = target.x - anchor.x
        val dy = target.y - anchor.y
        val distToTarget = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // Direction from anchor to target
        val dirX: Float
        val dirY: Float
        if (distToTarget > 0.0001f) {
            dirX = dx / distToTarget
            dirY = dy / distToTarget
        } else {
            dirX = 0f
            dirY = 1f
        }

        // Perpendicular direction (for sine wave offsets)
        val perpX = -dirY
        val perpY = dirX

        // Segment length is derived from the distance, so the tentacle expands/contracts
        val segmentLength = distToTarget / segmentCount

        // Place joints along the line from anchor toward target, then add wave offsets
        for (i in 0 until jointCount) {
            val t = i.toFloat() / (jointCount - 1).toFloat()

            // Base position: evenly spaced from anchor to target
            val baseX = anchor.x + dirX * t * distToTarget
            val baseY = anchor.y + dirY * t * distToTarget

            // Wave offset: bell-shaped envelope so both ends are pinned, middle waves most.
            // waveBlend smoothly fades the wave out when lunging and back in when returning.
            val envelope = MathUtils.sin(t * MathUtils.PI)
            val effectiveAmplitude = waveAmplitude * segmentLength * waveBlend
            val wave = MathUtils.sin(time * waveSpeed + i * wavePhaseOffset) * effectiveAmplitude * envelope

            joints[i].set(
                baseX + perpX * wave,
                baseY + perpY * wave
            )
        }

        // Force endpoints exactly
        joints[0].set(anchor)
        joints[jointCount - 1].set(target)

        // Update line segments every frame
        for (i in 0 until segmentCount) {
            lines[i].set(joints[i], joints[i + 1])
        }

        // Update circle positions at a fixed interval for a pixel-game stutter look
        circleUpdateTimer += delta
        if (circleUpdateTimer >= CIRCLE_UPDATE_INTERVAL) {
            circleUpdateTimer -= CIRCLE_UPDATE_INTERVAL
            for (i in 0 until jointCount) {
                circles[i].setCenter(joints[i])
            }
        }
    }

    override fun getType() = EntityType.SPECIAL
}
