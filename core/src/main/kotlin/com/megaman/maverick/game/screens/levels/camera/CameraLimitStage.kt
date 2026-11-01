package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
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
import com.maverick.game.engine.common.shapes.GameRectangle
import kotlin.math.abs

class CameraLimitStage(
    private val follow: CameraFollow,
    private val roomBounds: CameraRoomBounds,
    private val settledPosition: Vector3,
    private val defaultEaseSpeed: Float,
    private val hold: () -> Boolean = { false }
) : ICameraPart<RotatableCamera>, Resettable {

    val limits = CameraLimits()

    val crushSurfaces: Array<GameRectangle>
        get() = target.crushSurfaces

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
        target.clearEdges()

        for (i in 0 until rules.size) rules[i].apply(target)

        target.x.resolve(camera.blendedViewWidth, bounds.x, bounds.x + bounds.width)
        target.y.resolve(camera.blendedViewHeight, bounds.y, bounds.y + bounds.height)

        if (snap || room !== limitRoom) {
            limits.set(target)
            snap = false
            limitRoom = room
            return false
        }

        if (focusOutOfView(camera)) limits.set(target)
        else {
            val held = hold()
            val step = target.easeSpeed * delta
            if (!held || target.x.pinned) limits.x.easeTo(target.x, settledPosition.x, camera.viewWidth, step)
            if (!held || target.y.pinned) limits.y.easeTo(target.y, settledPosition.y, camera.viewHeight, step)
        }

        return false
    }

    fun edgeLimit(direction: Direction): Float? {
        return if (target.blockedEdges.contains(direction)) activeEdge(direction) else null
    }

    fun killEdge(direction: Direction): Float? =
        if (target.killEdges.contains(direction)) activeEdge(direction) else null

    fun edgeExtent(direction: Direction): Rectangle? = target.edgeExtents[direction] ?: limitRoom?.rectangle

    private fun activeEdge(direction: Direction): Float? {
        val bounds = limitRoom?.rectangle
        if (!active || bounds == null) return null
        if (limitRoom !== (roomBounds.currentGameRoom ?: roomBounds.priorGameRoom)) return null

        if (target.keptEdges.contains(direction)) return edge(direction)

        return when (direction) {
            Direction.LEFT -> limits.x.min.takeIf { it > bounds.x }
            Direction.RIGHT -> limits.x.max.takeIf { it < bounds.x + bounds.width }
            Direction.DOWN -> limits.y.min.takeIf { it > bounds.y }
            Direction.UP -> limits.y.max.takeIf { it < bounds.y + bounds.height }
        }
    }

    private fun edge(direction: Direction) = when (direction) {
        Direction.LEFT -> limits.x.min
        Direction.RIGHT -> limits.x.max
        Direction.DOWN -> limits.y.min
        Direction.UP -> limits.y.max
    }

    private fun focusOutOfView(camera: RotatableCamera): Boolean {
        val focus = follow.focus?.getFocusPosition() ?: return false
        return outOfView(limits.x, focus.x, camera.blendedViewWidth) || outOfView(limits.y, focus.y, camera.blendedViewHeight)
    }

    private fun outOfView(limit: CameraAxisLimit, position: Float, viewSize: Float) =
        abs(position - limit.clamp(position, viewSize)) > viewSize / 2f
}
