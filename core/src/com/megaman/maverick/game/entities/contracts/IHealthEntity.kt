package com.megaman.maverick.game.entities.contracts

import com.engine.entities.contracts.IPointsEntity
import com.engine.points.Points
import com.engine.points.PointsHandle
import com.megaman.maverick.game.ConstKeys

interface IHealthEntity : IPointsEntity {

  fun setHealthPoints(
      min: Int,
      max: Int,
      listener: ((Points) -> Unit)? = null,
      onReset: ((PointsHandle) -> Unit)? = null
  ) =
      getPointsComponent()
          .pointsMap
          .put(ConstKeys.HEALTH, PointsHandle(Points(min, max, max), listener, onReset))

  fun getHealthPoints() = getPoints(ConstKeys.HEALTH)
}
