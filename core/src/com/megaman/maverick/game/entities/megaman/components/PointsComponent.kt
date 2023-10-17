package com.megaman.maverick.game.entities.megaman.components

import com.engine.points.Points
import com.engine.points.PointsComponent
import com.engine.points.PointsHandle
import com.megaman.maverick.game.entities.megaman.Megaman

internal fun Megaman.definePointsComponent(): PointsComponent {
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
