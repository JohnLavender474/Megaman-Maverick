package com.megaman.maverick.game.pathfinding

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.shapes.GameLine
import com.engine.drawables.shapes.IDrawableShape
import com.engine.pathfinding.PathfinderResult
import com.engine.world.Body
import com.megaman.maverick.game.ConstVals

/** Standard implementation of a consumer for [PathfinderResult]. */
object StandardPathfinderResultConsumer {

  const val TAG = "StandardPathfinderResultConsumer"

  /**
   * Accepts the [result] of a pathfinding operation and applies it to the [body] at the given
   * [start] point with the given [speed].
   *
   * @param result The result of the pathfinding operation.
   * @param body The body to apply the result to.
   * @param start The start point of the pathfinding operation.
   * @param speed The speed of the body.
   * @return true if the pathfinding result was consumed, false otherwise.
   */
  fun consume(
      result: PathfinderResult,
      body: Body,
      start: Vector2,
      speed: Float,
      shapes: Array<IDrawableShape>? = null
  ): Boolean {
    val (_, worldPath, _) = result
    if (worldPath == null) {
      GameLogger.debug(TAG, "No path found for body $body")
      body.physics.velocity.setZero()
      return false
    }
    GameLogger.debug(TAG, "World path found: $worldPath. Body: $body")

    shapes?.let {
      for (i in 0 until worldPath.size - 1) {
        val node1 = worldPath[i]
        val node2 = worldPath[i + 1]
        it.add(GameLine(node1.getCenter(), node2.getCenter()))
      }
    }

    val iter = worldPath.iterator()
    var target: Vector2? = null
    while (iter.hasNext()) {
      val _target = iter.next()

      if (!body.overlaps(_target as Rectangle)) {
        shapes?.let {
          _target.color = Color.PURPLE
          it.add(_target)
        }

        target = _target.getCenter()
        break
      }
    }

    if (target == null) {
      GameLogger.debug(TAG, "No target found for body $body")
      body.physics.velocity.setZero()
      return false
    }
    GameLogger.debug(TAG, "Target found for body: $target. Body: $body")

    val angle = MathUtils.atan2(target.y - start.y, target.x - start.x)
    val trajectory = Vector2(MathUtils.cos(angle), MathUtils.sin(angle)).scl(speed)
    val scaledTrajectory = trajectory.scl(ConstVals.PPM.toFloat())
    body.physics.velocity.set(scaledTrajectory)

    GameLogger.debug(TAG, "Body is moving with velocity ${body.physics.velocity}. Body: $body")

    return true
  }
}
