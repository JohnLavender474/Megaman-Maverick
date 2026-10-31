package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.CameraAxisLimit
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraFollowMode
import com.maverick.game.engine.camera.CameraLimits
import com.maverick.game.engine.camera.ICameraLimitRule
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import kotlin.math.abs

class CameraLimitStage(
    private val follow: CameraFollow,
    private val roomBounds: CameraRoomBounds,
    private val settledPosition: Vector3,
    private val defaultEaseSpeed: Float,
    private val hold: () -> Boolean = { false }
) : ICameraPart<RotatableCamera>, Resettable {

    val limits = CameraLimits()

    var active = false
        private set

    private val rules = Array<ICameraLimitRule>()
    private val target = CameraLimits()

    private var limitRoom: RectangleMapObject? = null
    private var snap = true

    fun addRule(rule: ICameraLimitRule) {
        rules.add(rule)
    }

    override fun reset() {
        snap = true
    }

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        val room = when (follow.mode) {
            CameraFollowMode.X_AND_Y -> roomBounds.currentGameRoom
            CameraFollowMode.X_ONLY -> roomBounds.priorGameRoom!!
            CameraFollowMode.OFF -> null
        }

        active = room != null
        if (room == null) return false

        val bounds = room.rectangle

        target.x.set(bounds.x, bounds.x + bounds.width)
        target.y.set(bounds.y, bounds.y + bounds.height)
        target.easeSpeed = defaultEaseSpeed
        target.blockedEdges.clear()

        for (i in 0 until rules.size) rules[i].apply(target)

        target.x.resolve(bounds.x, bounds.x + bounds.width)
        target.y.resolve(bounds.y, bounds.y + bounds.height)

        if (snap || room !== limitRoom) {
            limits.set(target)
            snap = false
            limitRoom = room
            return false
        }

        if (focusOutOfView(camera)) limits.set(target)
        else if (!hold()) {
            val step = target.easeSpeed * delta
            limits.x.easeTo(target.x, settledPosition.x, camera.viewWidth, step)
            limits.y.easeTo(target.y, settledPosition.y, camera.viewHeight, step)
        }

        return false
    }

    fun edgeLimit(direction: Direction): Float? {
        val bounds = limitRoom?.rectangle
        if (!active || bounds == null || !target.blockedEdges.contains(direction)) return null

        return when (direction) {
            Direction.LEFT -> limits.x.min.takeIf { it > bounds.x }
            Direction.RIGHT -> limits.x.max.takeIf { it < bounds.x + bounds.width }
            Direction.DOWN -> limits.y.min.takeIf { it > bounds.y }
            Direction.UP -> limits.y.max.takeIf { it < bounds.y + bounds.height }
        }
    }

    private fun focusOutOfView(camera: RotatableCamera): Boolean {
        val focus = follow.focus?.getFocusPosition() ?: return false
        return outOfView(limits.x, focus.x, camera.viewWidth) || outOfView(limits.y, focus.y, camera.viewHeight)
    }

    private fun outOfView(limit: CameraAxisLimit, position: Float, viewSize: Float) =
        abs(position - limit.clamp(position, viewSize)) > viewSize / 2f
}
