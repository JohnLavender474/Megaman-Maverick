package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.maverick.game.engine.world.IDrawOffsets
import kotlin.math.abs

class CameraDrawOffset(
    private val view: CameraView,
    private val follow: CameraFollow,
    private val stage: CameraLimitStage,
    private val settledPosition: Vector3,
    private val offsets: IDrawOffsets
) : ICameraPart {

    companion object {
        const val SETTLED_EPSILON = 0.01f
    }

    val offset = Vector2()

    var source: IDrawOffsetSource? = null

    private val focusOffset = Vector2()

    override fun update(camera: Camera, delta: Float): Boolean {
        offset.setZero()

        val source = source
        val followedX = follow.followedX
        val followedY = follow.followedY
        follow.clearFollowed()

        if (source == null || (followedX.isNaN() && followedY.isNaN())) return false

        source.getDrawOffset(offsets, focusOffset)

        if (!followedX.isNaN()) offset.x = axisOffset(followedX, focusOffset.x, settledPosition.x, true)
        if (!followedY.isNaN()) offset.y = axisOffset(followedY, focusOffset.y, settledPosition.y, false)

        return false
    }

    private fun axisOffset(followed: Float, focusOffset: Float, settled: Float, horizontal: Boolean): Float {
        if (focusOffset == 0f) return 0f
        if (abs(clamp(followed, horizontal) - settled) > SETTLED_EPSILON) return 0f
        return clamp(followed + focusOffset, horizontal) - settled
    }

    private fun clamp(center: Float, horizontal: Boolean): Float {
        var result = if (horizontal) center.coerceIn(follow.minX, follow.maxX) else center
        if (stage.active) result =
            if (horizontal) stage.limits.x.clamp(result, view.blendedViewWidth)
            else stage.limits.y.clamp(result, view.blendedViewHeight)
        return result
    }
}
