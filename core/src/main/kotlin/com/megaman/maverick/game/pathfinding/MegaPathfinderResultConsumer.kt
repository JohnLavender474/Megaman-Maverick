package com.megaman.maverick.game.pathfinding

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.pathfinding.PathfinderResult
import com.maverick.game.engine.world.body.Body
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toWorldCoordinate
import com.megaman.maverick.game.world.body.getBounds

object MegaPathfinderResultConsumer {

    const val TAG = "StandardPathfinderResultConsumer"

    private val pathCell = GameRectangle().setSize(ConstVals.PPM.toFloat())
    private val trajectory = Vector2()

    fun consume(
        result: PathfinderResult,
        body: Body,
        start: Vector2,
        speed: () -> Float,
        targetPursuer: GameRectangle = body.getBounds(),
        onTargetReached: () -> Unit = {},
        stopOnTargetReached: Boolean = true,
        onTargetNull: () -> Unit = {},
        stopOnTargetNull: Boolean = true,
        preProcess: (() -> Unit)? = null,
        trajectoryConsumer: (Vector2) -> Unit = { trajectory -> body.physics.velocity.set(trajectory) },
        postProcess: (() -> Unit)? = null
    ): Boolean {
        preProcess?.invoke()

        val path = result.path
        if (path == null || path.isEmpty) {
            if (stopOnTargetReached) body.physics.velocity.setZero()
            onTargetReached.invoke()
            return false
        }

        var target: Vector2? = null
        for (i in 0 until path.size) {
            pathCell.setPosition(path[i].toWorldCoordinate())
            if (!targetPursuer.overlaps(pathCell)) {
                target = pathCell.getCenter()
                break
            }
        }
        if (target == null) {
            if (stopOnTargetNull) body.physics.velocity.setZero()
            onTargetNull.invoke()
            return false
        }

        val angle = MathUtils.atan2(target.y - start.y, target.x - start.x)
        trajectory.set(MathUtils.cos(angle), MathUtils.sin(angle)).scl(speed())
        trajectoryConsumer(trajectory)

        postProcess?.invoke()

        return true
    }
}
