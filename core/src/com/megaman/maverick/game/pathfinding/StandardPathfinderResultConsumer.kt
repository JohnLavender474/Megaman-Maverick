package com.megaman.maverick.game.pathfinding

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.engine.pathfinding.PathfinderResult
import com.engine.world.Body
import com.megaman.maverick.game.ConstVals

/** Standard implementation of a consumer for [PathfinderResult]. */
object StandardPathfinderResultConsumer {

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
  fun consume(result: PathfinderResult, body: Body, start: Vector2, speed: Float): Boolean {
    val (_, worldPath, targetReached) = result
    if (!targetReached || worldPath == null) return false

    val iter = worldPath.iterator()
    var target: Vector2? = null
    while (iter.hasNext()) {
      target = iter.next()
      if (!body.contains(target)) break
    }

    if (target == null) return false

    val angle = MathUtils.atan2(target.y - start.y, target.x - start.x)
    val trajectory = Vector2(MathUtils.cos(angle), MathUtils.sin(angle)).scl(speed)

    val scaledTrajectory = trajectory.scl(ConstVals.PPM.toFloat())
    body.physics.velocity.set(scaledTrajectory)

    return true
  }
}
