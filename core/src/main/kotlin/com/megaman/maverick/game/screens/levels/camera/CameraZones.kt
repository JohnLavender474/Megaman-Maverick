package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.camera.CameraAxisLimit
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraLimits
import com.maverick.game.engine.camera.ICameraLimitRule
import com.maverick.game.engine.common.enums.Direction

class CameraZone(
    val bounds: Rectangle,
    val minX: Float? = null,
    val maxX: Float? = null,
    val minY: Float? = null,
    val maxY: Float? = null,
    val easeSpeed: Float? = null,
    val edgeBlocks: ObjectSet<Direction> = ObjectSet()
)

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
        }
    }

    private fun applyEdges(limit: CameraAxisLimit, min: Float?, max: Float?) {
        min?.let { limit.setMin(it) }
        max?.let { limit.setMax(it) }
    }
}
