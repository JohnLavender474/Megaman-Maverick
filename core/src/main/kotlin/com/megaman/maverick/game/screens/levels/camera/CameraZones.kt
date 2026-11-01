package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.camera.CameraAxisLimit
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraLimits
import com.maverick.game.engine.camera.ICameraLimitRule
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle

/**
 * A `type=zone` object in the `camera_sections` layer. It acts while the focus point is inside it.
 *
 * Required in Tiled: the rectangle and `type`.
 * Optional in Tiled:
 * - `min_x`, `max_x`, `min_y`, `max_y`: object references. A `min_*` property reads the low edge of its object, and a `max_*`
 *   property reads the high edge. One rectangle of the view height in `min_y` and `max_y` locks the view.
 * - `speed`: the ease in tiles per second, while this zone is active.
 * - `edge_blocks`: edges with a `CameraEdgeBlock`. Default none. A block covers the zone rectangle, and it stops at a room edge.
 * - `crush_surface` objects with a `section` reference. With none, nothing crushes at that solid.
 *
 * Automatic: the ease, the snap when the focus is out of view, and the fold in Tiled order, where a later zone wins per edge.
 * Draw a zone over every position that the player can reach in that part of the room. A player above a locked view gets an
 * ease of half a view, then a jump. The level design avoids that, for example with a ceiling.
 */
class CameraZone(
    val bounds: Rectangle,
    val minX: Float? = null,
    val maxX: Float? = null,
    val minY: Float? = null,
    val maxY: Float? = null,
    val easeSpeed: Float? = null,
    val edgeBlocks: ObjectSet<Direction> = ObjectSet()
) {
    val crushSurfaces = Array<GameRectangle>()
}

class CameraZones(private val follow: CameraFollow) : ICameraLimitRule {

    var zones = Array<CameraZone>()

    override fun apply(target: CameraLimits) {
        val focus = follow.focus?.getFocusPosition() ?: return

        for (i in 0 until zones.size) {
            val zone = zones[i]
            if (!zone.bounds.contains(focus.x, focus.y)) continue

            applyEdges(target.x, zone.minX, zone.maxX)
            applyEdges(target.y, zone.minY, zone.maxY)
            zone.easeSpeed?.let { target.easeSpeed = it }
            target.blockedEdges.addAll(zone.edgeBlocks)
            for (edge in zone.edgeBlocks) target.edgeExtents.put(edge, zone.bounds)
            target.crushSurfaces.addAll(zone.crushSurfaces)
        }
    }

    private fun applyEdges(limit: CameraAxisLimit, min: Float?, max: Float?) {
        min?.let { limit.setMin(it) }
        max?.let { limit.setMax(it) }
    }
}
