package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.toGdxArray

data class TrajectoryDefinition(val xVelocity: Float, val yVelocity: Float, val time: Float)

class Trajectory(
    private val trajectoryDefinitions: Array<TrajectoryDefinition>,
    private val ppm: Int = 1
) : IMotion {

    companion object {

        fun parseTrajectoryDefinitions(trajectoryDefinitionString: String) =
            trajectoryDefinitionString
                .split(";".toRegex())
                .map {
                    val values = it.split(",".toRegex())
                    TrajectoryDefinition(values[0].toFloat(), values[1].toFloat(), values[2].toFloat())
                }
                .toGdxArray()
    }

    private var currentDefinition =
        if (!trajectoryDefinitions.isEmpty) trajectoryDefinitions[0] else null
    private var duration = 0f
    private var index = 0

    constructor(
        trajectoryDefinitions: String,
        ppm: Int = 1
    ) : this(parseTrajectoryDefinitions(trajectoryDefinitions), ppm)

    override fun getMotionValue(out: Vector2) =
        currentDefinition?.let { out.set(it.xVelocity, it.yVelocity).scl(ppm.toFloat()) }

    override fun update(delta: Float) {
        duration += delta
        currentDefinition = trajectoryDefinitions[index]

        currentDefinition?.let {
            if (duration >= it.time) {
                duration = 0f
                index++

                if (index >= trajectoryDefinitions.size) index = 0
                currentDefinition = trajectoryDefinitions[index]
            }
        }
    }

    override fun reset() {
        duration = 0f
        index = 0
    }
}
