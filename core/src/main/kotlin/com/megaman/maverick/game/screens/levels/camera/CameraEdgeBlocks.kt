package com.megaman.maverick.game.screens.levels.camera

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
    centerX: Float,
    centerY: Float,
    viewWidth: Float,
    viewHeight: Float,
    thickness: Float
): GameRectangle {
    val length = (if (direction.isVertical()) viewWidth else viewHeight) + 2f * thickness

    if (direction.isVertical()) out.setSize(length, thickness) else out.setSize(thickness, length)

    return when (direction) {
        Direction.LEFT -> out.setCenter(edge - thickness / 2f, centerY)
        Direction.RIGHT -> out.setCenter(edge + thickness / 2f, centerY)
        Direction.DOWN -> out.setCenter(centerX, edge - thickness / 2f)
        Direction.UP -> out.setCenter(centerX, edge + thickness / 2f)
    }
}

class CameraEdgeBlocks(
    private val stage: CameraLimitStage,
    private val enabled: () -> Boolean
) : ICameraPart<RotatableCamera>, Resettable {

    private val blocks = ObjectMap<Direction, CameraEdgeBlock>()
    private val bounds = GameRectangle()

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        val enabled = enabled()

        for (direction in Direction.entries) {
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
                continue
            }

            cameraEdgeBlockBounds(
                bounds, direction, edge, camera.position.x, camera.position.y, camera.viewWidth, camera.viewHeight,
                ConstVals.PPM.toFloat()
            )

            if (block != null) block.body.set(bounds)
            else {
                val spawned = MegaEntityFactory.fetch(CameraEdgeBlock::class)!!
                spawned.spawn(props(ConstKeys.BOUNDS pairTo bounds.copy(), ConstKeys.DIRECTION pairTo direction))
                blocks.put(direction, spawned)
            }
        }

        return false
    }

    override fun reset() {
        blocks.values().forEach { if (it.existenceState.liveAfterUpdate) it.destroy() }
        blocks.clear()
    }
}
