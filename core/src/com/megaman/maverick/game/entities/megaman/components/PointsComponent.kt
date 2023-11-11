package com.megaman.maverick.game.entities.megaman.components

import com.engine.points.PointsComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues

internal fun Megaman.definePointsComponent(): PointsComponent {
  val pointsComponent = PointsComponent(this)
  pointsComponent.putPoints(ConstKeys.HEALTH, MegamanValues.START_HEALTH)
  return pointsComponent
}
