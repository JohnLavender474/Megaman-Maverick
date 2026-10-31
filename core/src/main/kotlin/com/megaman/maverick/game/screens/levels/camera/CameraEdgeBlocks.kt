package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.CameraEdgeBlock

fun cameraEdgeBlockBounds(
    out: GameRectangle,
    direction: Direction,
    edge: Float,
    extent: Rectangle,
    thickness: Float
): GameRectangle {
    val length = (if (direction.isVertical()) extent.width else extent.height) + 2f * thickness
    val centerX = extent.x + extent.width / 2f
    val centerY = extent.y + extent.height / 2f

    if (direction.isVertical()) out.setSize(length, thickness) else out.setSize(thickness, length)

    return when (direction) {
        Direction.LEFT -> out.setCenter(edge - thickness / 2f, centerY)
        Direction.RIGHT -> out.setCenter(edge + thickness / 2f, centerY)
        Direction.DOWN -> out.setCenter(centerX, edge - thickness / 2f)
        Direction.UP -> out.setCenter(centerX, edge + thickness / 2f)
    }
}

fun movedIntoView(direction: Direction, old: Float, new: Float) = when (direction) {
    Direction.LEFT, Direction.DOWN -> new > old
    Direction.RIGHT, Direction.UP -> new < old
}

fun isPastCameraEdge(bounds: GameRectangle, direction: Direction, edge: Float) = when (direction) {
    Direction.LEFT -> bounds.getMaxX() < edge
    Direction.RIGHT -> bounds.getX() > edge
    Direction.DOWN -> bounds.getMaxY() < edge
    Direction.UP -> bounds.getY() > edge
}

class CameraEdgeBlocks(
    private val stage: CameraLimitStage,
    private val enabled: () -> Boolean,
    private val killAtEdge: (Direction, Float) -> Unit = { _, _ -> }
) : ICameraPart<RotatableCamera>, Resettable {

    private val blocks = ObjectMap<Direction, CameraEdgeBlock>()
    private val edges = ObjectMap<Direction, Float>()
    private val bounds = GameRectangle()

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        val enabled = enabled()

        for (direction in Direction.entries) {
            if (enabled) stage.killEdge(direction)?.let { killAtEdge(direction, it) }

            var block = blocks[direction]
            if (block != null && !block.existenceState.liveAfterUpdate) {
                blocks.remove(direction)
                block = null
            }

            val edge = if (enabled) stage.edgeLimit(direction) else null
            if (edge == null) {
                if (block != null) {
                    block.destroy()
                    blocks.remove(direction)
                }
                edges.remove(direction)
                continue
            }

            cameraEdgeBlockBounds(bounds, direction, edge, stage.edgeExtent(direction)!!, ConstVals.PPM.toFloat())

            if (block != null) {
                block.body.set(bounds)
                block.crushing = movedIntoView(direction, edges.get(direction, edge), edge)
                block.crushSurfaces = stage.crushSurfaces
            } else {
                val spawned = MegaEntityFactory.fetch(CameraEdgeBlock::class)!!
                spawned.spawn(props(ConstKeys.BOUNDS pairTo bounds.copy(), ConstKeys.DIRECTION pairTo direction))
                blocks.put(direction, spawned)
            }

            edges.put(direction, edge)
        }

        return false
    }

    override fun reset() {
        blocks.values().forEach { if (it.existenceState.liveAfterUpdate) it.destroy() }
        blocks.clear()
        edges.clear()
    }
}
