package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getCenter
import kotlin.math.sqrt

class WavyTentacleOfJoints(game: MegamanMaverickGame) : MegaGameEntity(game), Updatable {

    companion object {
        const val TAG = "WavyTentacleOfJoints"

        private const val DEFAULT_SEGMENT_COUNT = 6
        private const val DEFAULT_WAVE_SPEED = 3f
        private const val DEFAULT_WAVE_AMPLITUDE = 0.5f
        private const val DEFAULT_WAVE_PHASE_OFFSET = 1.25f

        private const val WAVE_BLEND_SPEED = 5f
    }

    enum class TentacleState { IDLE, LUNGING, PAUSING, RETURNING }

    private val _anchor = Vector2()
    private val _target = Vector2()

    var segmentCount = DEFAULT_SEGMENT_COUNT
        private set
    var waveSpeed = DEFAULT_WAVE_SPEED
    var waveAmplitude = DEFAULT_WAVE_AMPLITUDE
    var wavePhaseOffset = DEFAULT_WAVE_PHASE_OFFSET

    private val joints = Array<Vector2>()

    val jointCount: Int
        get() = joints.size

    private var state = TentacleState.IDLE
    private var waveBlend = 1f
    private var accumulator = 0f

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        state = TentacleState.IDLE

        accumulator = 0f
        waveBlend = 1f

        segmentCount = spawnProps.getOrDefault(ConstKeys.COUNT, DEFAULT_SEGMENT_COUNT, Int::class)
        waveSpeed = spawnProps.getOrDefault(ConstKeys.SPEED, DEFAULT_WAVE_SPEED, Float::class)
        waveAmplitude = spawnProps.getOrDefault("wave_amplitude", DEFAULT_WAVE_AMPLITUDE, Float::class)
        wavePhaseOffset = spawnProps.getOrDefault("wave_phase_offset", DEFAULT_WAVE_PHASE_OFFSET, Float::class)

        val anchorPos = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        _anchor.set(anchorPos)
        _target.set(anchorPos)

        joints.clear()
        val jointCount = segmentCount + 1
        repeat(jointCount) { joints.add(Vector2()) }

        updateJoints()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        joints.clear()
    }

    override fun update(delta: Float) {
        accumulator += delta

        waveBlend = when (state) {
            TentacleState.IDLE, TentacleState.RETURNING ->
                MathUtils.lerp(waveBlend, 1f, WAVE_BLEND_SPEED * delta)
            TentacleState.LUNGING ->
                MathUtils.lerp(waveBlend, 0f, WAVE_BLEND_SPEED * delta)
            TentacleState.PAUSING -> 0f
        }

        updateJoints()
    }

    fun getState(): TentacleState = state

    fun setState(newState: TentacleState) {
        if (state == newState) return
        if (newState == TentacleState.PAUSING) waveBlend = 0f
        state = newState
    }

    fun setAnchor(x: Float, y: Float) = _anchor.set(x, y)

    fun setTarget(x: Float, y: Float) = _target.set(x, y)

    fun setAnchor(v: Vector2) = _anchor.set(v)

    fun setTarget(v: Vector2) = _target.set(v)

    fun getJoint(index: Int, out: Vector2): Vector2 = out.set(joints[index])

    private fun updateJoints() {
        val jointCount = joints.size
        if (jointCount < 2) return

        val dx = _target.x - _anchor.x
        val dy = _target.y - _anchor.y
        val distToTarget = sqrt(dx * dx + dy * dy)

        val dirX: Float
        val dirY: Float
        if (distToTarget > 0.0001f) {
            dirX = dx / distToTarget
            dirY = dy / distToTarget
        } else {
            dirX = 0f
            dirY = 1f
        }

        // Perpendicular direction for the lateral wave offset (90° CCW rotation of dir)
        val perpX = -dirY

        // Segment length scales with distance so the wave looks proportional at any extension
        val segmentLength = distToTarget / segmentCount

        for (i in 0 until jointCount) {
            val t = i.toFloat() / (jointCount - 1).toFloat()

            val baseX = _anchor.x + dirX * t * distToTarget
            val baseY = _anchor.y + dirY * t * distToTarget

            // Bell envelope pins both endpoints; waveBlend fades wave in/out during transitions
            val envelope = MathUtils.sin(t * MathUtils.PI)
            val effectiveAmplitude = waveAmplitude * segmentLength * waveBlend
            val wave = MathUtils.sin(accumulator * waveSpeed + i * wavePhaseOffset) * effectiveAmplitude * envelope

            joints[i].set(baseX + perpX * wave, baseY + dirX * wave)
        }

        // Force endpoints to match exactly, eliminating any floating-point drift
        joints[0].set(_anchor)
        joints[jointCount - 1].set(_target)
    }

    override fun getType() = EntityType.SPECIAL
}
