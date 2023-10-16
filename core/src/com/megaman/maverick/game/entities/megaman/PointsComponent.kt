package com.megaman.maverick.game.entities.megaman

import com.engine.points.Points
import com.engine.points.PointsComponent
import com.engine.points.PointsHandle

/** Returns the [PointsComponent] of this [Megaman], or creates a new one if it doesn't have one. */
fun Megaman.pointsComponent(): PointsComponent {
  if (hasComponent(PointsComponent::class)) return getComponent(PointsComponent::class)!!

  // health
  val health =
      PointsHandle(
          Points(0, 10, 10),
          listener = { points ->
            if (points.current == 0) {
              dead = true
            }
          },
          onReset = { handle -> handle.points.setToMax() })

  return PointsComponent(this, "health" to health)
}
