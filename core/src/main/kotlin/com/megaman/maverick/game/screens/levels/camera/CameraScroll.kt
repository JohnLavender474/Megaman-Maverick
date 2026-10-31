package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraLimits
import com.maverick.game.engine.camera.ICameraLimitRule
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.shapes.GameRectangle
import kotlin.math.max
import kotlin.math.min

/**
 * A `type=scroll` object in the `camera_sections` layer.
 *
 * Required in Tiled: the rectangle, `type`, `direction` and `speed`.
 * Optional in Tiled:
 * - `stop`: `exit` (default) or `arrive`, for a corner.
 * - `next`: one-way object reference to the section that starts when this one stops. With none, the first section that
 *   overlaps this one starts.
 * - `edge_blocks`: replaces the block list. A listed edge has a block and no fall kill. An edge left out has no block, and
 *   a trailing edge left out also loses its "fully past" kill. A fall edge left out stays a kill.
 * - `crush_surface` objects with a `section` reference. With none, nothing crushes at that solid.
 * - A zone over the section, to lock the other axis. It must reach one view past the far edge.
 *
 * Automatic: the edges in [CameraScroll], the block length (the section rectangle), the kept trailing edge, the wait at a
 * room edge, the continue after a pan, the holds, the wait until the player is ready, and the ease back to the follow.
 */
class CameraScrollSection(
    val bounds: Rectangle,
    val direction: Direction,
    val speed: Float,
    val arrive: Boolean = false,
    val edgeBlocks: ObjectSet<Direction>? = null,
    var next: CameraScrollSection? = null
) {
    val crushSurfaces = Array<GameRectangle>()
}

private class KeptEdge(val value: Float, val section: CameraScrollSection)

/**
 * Runs one [CameraScrollSection] at a time. It pins its axis, and the other axis keeps its follow and its limits. Edges
 * across the scroll axis have no block.
 *
 * The edges depend on the fall direction, which is the opposite of the gravity direction:
 *
 * | Case                              | Leading edge                  | Trailing edge                          | After the stop       |
 * |-----------------------------------|-------------------------------|----------------------------------------|----------------------|
 * | A. Fall toward the leading edge   | No block. Fully past it kills,| Block. Crushes at a `crush_surface`.   | Trailing block stays.|
 * |                                   | except at a room edge.        | Fully past it kills.                   | Leading kill ends.   |
 * | B. Fall toward the trailing edge  | Block. Stops at a room edge.  | No block. Fully past it kills, also at | Trailing kill stays. |
 * |                                   |                               | a room edge.                           |                      |
 * | C. Fall across the scroll         | Block. Stops at a room edge.  | Block. Crushes at a `crush_surface`.   | Trailing block stays.|
 * |                                   |                               | Fully past it kills.                   |                      |
 *
 * | Scroll | Falls down (normal) | Falls up (`down`) | Falls right (`left`) | Falls left (`right`) |
 * |--------|---------------------|-------------------|----------------------|----------------------|
 * | right  | C                   | C                 | A                    | B                    |
 * | left   | C                   | C                 | B                    | A                    |
 * | up     | B                   | A                 | C                    | C                    |
 * | down   | A                   | B                 | C                    | C                    |
 *
 * The case updates during play if gravity changes. The tables are also in the `051` item `NOTES.md`.
 */
class CameraScroll(
    private val follow: CameraFollow,
    private val roomBounds: CameraRoomBounds,
    private val settledPosition: Vector3,
    private val fallDirection: () -> Direction,
    private val hold: () -> Boolean = { false }
) : ICameraPart<RotatableCamera>, ICameraLimitRule, Resettable {

    var sections = Array<CameraScrollSection>()

    private val done = ObjectSet<CameraScrollSection>()
    private val keptEdges = ObjectMap<Direction, KeptEdge>()

    private var section: CameraScrollSection? = null
    private var position = 0f
    private var room: RectangleMapObject? = null
    private var deriveDone = true

    override fun reset() {
        done.clear()
        keptEdges.clear()
        section = null
        room = null
        deriveDone = true
    }

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        if (deriveDone) {
            deriveDone = false
            deriveDoneSections(camera)
        }

        val focus = follow.focus?.getFocusPosition() ?: return false
        val currentRoom = roomBounds.currentGameRoom ?: roomBounds.priorGameRoom

        if (section == null) startAt(focus, currentRoom)

        var active = section ?: return false

        if (room !== currentRoom) {
            room = currentRoom
            if (!active.bounds.contains(focus.x, focus.y)) {
                section = null
                return false
            }
            position = axisPosition(active)
        }

        if (hold()) return false

        val viewSize = viewSize(camera, active)
        val stop = stopPosition(active, viewSize)

        position += sign(active) * active.speed * delta
        if (sign(active) * (position - stop) >= 0f) position = stop
        currentRoom?.let { position = insideRoom(active, position, it.rectangle, viewSize) }

        if (sign(active) * (position - stop) >= 0f) finish(active, viewSize)

        return false
    }

    override fun apply(target: CameraLimits) {
        keptEdges.forEach { applyKeptEdge(target, it.key, it.value) }

        val active = section ?: return

        (if (active.direction.isHorizontal()) target.x else target.y).setPin(position)

        target.crushSurfaces.addAll(active.crushSurfaces)

        markEdge(target, active, active.direction, false)

        val trailing = active.direction.getOpposite()
        markEdge(target, active, trailing, true)
        if (hasBlock(active, trailing)) target.killEdges.add(trailing)
    }

    private fun startAt(focus: Vector2, currentRoom: RectangleMapObject?) {
        for (i in 0 until sections.size) {
            val candidate = sections[i]
            if (done.contains(candidate) || !candidate.bounds.contains(focus.x, focus.y)) continue

            section = candidate
            position = axisPosition(candidate)
            room = currentRoom
            return
        }
    }

    private fun finish(finished: CameraScrollSection, viewSize: Float) {
        done.add(finished)
        section = null

        if (keepsTrailingEdge(finished)) keep(finished, trailingEdge(finished, position, viewSize))

        val following = following(finished) ?: return
        section = following
        position = axisPosition(following)
    }

    private fun following(finished: CameraScrollSection): CameraScrollSection? {
        finished.next?.let { return if (done.contains(it)) null else it }

        for (i in 0 until sections.size) {
            val candidate = sections[i]
            if (!done.contains(candidate) && candidate.bounds.overlaps(finished.bounds)) return candidate
        }
        return null
    }

    private fun deriveDoneSections(camera: RotatableCamera) {
        for (i in 0 until sections.size) {
            val candidate = sections[i]
            val viewSize = viewSize(camera, candidate)
            val stop = stopPosition(candidate, viewSize)
            val center = if (candidate.direction.isHorizontal()) camera.position.x else camera.position.y

            if (sign(candidate) * (center - stop) < 0f) continue

            done.add(candidate)
            if (keepsTrailingEdge(candidate)) keep(candidate, trailingEdge(candidate, stop, viewSize))
        }
    }

    private fun keep(finished: CameraScrollSection, edge: Float) {
        val trailing = finished.direction.getOpposite()
        val kept = keptEdges.get(trailing)?.value
        val value = if (kept == null) edge else if (sign(finished) > 0f) max(kept, edge) else min(kept, edge)
        keptEdges.put(trailing, KeptEdge(value, finished))
    }

    private fun applyKeptEdge(target: CameraLimits, edge: Direction, kept: KeptEdge) {
        when (edge) {
            Direction.LEFT -> target.x.setMin(max(target.x.min, kept.value))
            Direction.RIGHT -> target.x.setMax(min(target.x.max, kept.value))
            Direction.DOWN -> target.y.setMin(max(target.y.min, kept.value))
            Direction.UP -> target.y.setMax(min(target.y.max, kept.value))
        }
        markEdge(target, kept.section, edge, true)
    }

    private fun markEdge(target: CameraLimits, section: CameraScrollSection, edge: Direction, trailing: Boolean) {
        val blocked = hasBlock(section, edge)
        if (!blocked && edge != fallDirection()) return

        if (trailing) target.keptEdges.add(edge)

        if (blocked) {
            target.blockedEdges.add(edge)
            target.edgeExtents.put(edge, section.bounds)
        } else target.killEdges.add(edge)
    }

    private fun hasBlock(section: CameraScrollSection, edge: Direction) =
        section.edgeBlocks?.contains(edge) ?: (edge != fallDirection())

    private fun keepsTrailingEdge(section: CameraScrollSection): Boolean {
        val trailing = section.direction.getOpposite()
        return hasBlock(section, trailing) || trailing == fallDirection()
    }

    private fun axisPosition(section: CameraScrollSection) =
        if (section.direction.isHorizontal()) settledPosition.x else settledPosition.y

    private fun viewSize(camera: RotatableCamera, section: CameraScrollSection) =
        if (section.direction.isHorizontal()) camera.viewWidth else camera.viewHeight

    private fun sign(section: CameraScrollSection) =
        if (section.direction == Direction.RIGHT || section.direction == Direction.UP) 1f else -1f

    private fun farEdge(section: CameraScrollSection) = when (section.direction) {
        Direction.RIGHT -> section.bounds.x + section.bounds.width
        Direction.LEFT -> section.bounds.x
        Direction.UP -> section.bounds.y + section.bounds.height
        Direction.DOWN -> section.bounds.y
    }

    private fun stopPosition(section: CameraScrollSection, viewSize: Float) =
        farEdge(section) + sign(section) * viewSize / 2f * (if (section.arrive) -1f else 1f)

    private fun trailingEdge(section: CameraScrollSection, center: Float, viewSize: Float) =
        center - sign(section) * viewSize / 2f

    private fun insideRoom(section: CameraScrollSection, center: Float, room: Rectangle, viewSize: Float): Float {
        val low = if (section.direction.isHorizontal()) room.x else room.y
        val size = if (section.direction.isHorizontal()) room.width else room.height
        return if (sign(section) > 0f) min(center, low + size - viewSize / 2f) else max(center, low + viewSize / 2f)
    }
}
