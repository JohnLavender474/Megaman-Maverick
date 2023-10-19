package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.engine.world.Body

/** Determines if the velocity should be set or if an amount should be added to it. */
enum class VelocityAlterationType {
  SET,
  ADD
}

/**
 * Defines a velocity alteration. A velocity alteration is a change in velocity that is applied. By
 * default, the velocity alteration is to add 0 to the x and y velocities.
 *
 * @param forceX the force to apply to the x velocity
 * @param forceY the force to apply to the y velocity
 * @param actionX the action to apply to the x velocity
 * @param actionY the action to apply to the y velocity
 */
data class VelocityAlteration(
    val forceX: Float = 0f,
    val forceY: Float = 0f,
    val actionX: VelocityAlterationType = VelocityAlterationType.ADD,
    val actionY: VelocityAlterationType = VelocityAlterationType.ADD
)

/** Convenience object for velocity alteration. */
object VelocityAlterator {

  /**
   * Alterates the velocity of the [Body] using the provided [VelocityAlteration]. The alteration is
   * applied to the body's velocity. The [delta] param is optional and is used to scale the force
   * applied to the velocity. The default value is 1.
   *
   * @param body the body whose velocity to alterate
   * @param alteration the velocity alteration to apply
   * @param delta the delta time
   */
  fun alterate(body: Body, alteration: VelocityAlteration, delta: Float = 1f) =
      alterate(body.physics.velocity, alteration, delta)

  /**
   * Alterates the provided velocity using the provided [VelocityAlteration]. The alteration is
   * * applied to the body's velocity. The [delta] param is optional and is used to scale the force
   * * applied to the velocity. The default value is 1.
   *
   * @param velocity the velocity to alterate
   * @param alteration the velocity alteration to apply
   * @param delta the delta time
   */
  fun alterate(velocity: Vector2, alteration: VelocityAlteration, delta: Float = 1f) {
    velocity.x =
        when (alteration.actionX) {
          VelocityAlterationType.SET -> alteration.forceX * delta
          VelocityAlterationType.ADD -> velocity.x + alteration.forceX * delta
        }
    velocity.y =
        when (alteration.actionY) {
          VelocityAlterationType.SET -> alteration.forceY * delta
          VelocityAlterationType.ADD -> velocity.y + alteration.forceY * delta
        }
  }
}
