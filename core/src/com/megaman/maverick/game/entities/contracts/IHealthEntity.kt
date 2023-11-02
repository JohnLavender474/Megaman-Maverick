package com.megaman.maverick.game.entities.contracts

import com.engine.entities.contracts.IPointsEntity
import com.engine.points.Points
import com.engine.points.PointsHandle
import com.megaman.maverick.game.ConstKeys

/** An entity containing health points. */
interface IHealthEntity : IPointsEntity {

  /**
   * Sets the health points of this entity.
   *
   * @param min The minimum health points.
   * @param max The maximum health points.
   * @param listener The listener to be called when the health points change.
   * @param onReset The listener to be called when the health points are reset.
   */
  fun setHealthPoints(
      min: Int,
      max: Int,
      listener: ((Points) -> Unit)? = null,
      onReset: ((PointsHandle) -> Unit)? = null
  ) =
      getPointsComponent()
          .pointsMap
          .put(ConstKeys.HEALTH, PointsHandle(Points(min, max, max), listener, onReset))

  /**
   * Gets the health points of this entity.
   *
   * @return The health points of this entity.
   */
  fun getHealthPoints() = getPoints(ConstKeys.HEALTH)

  /**
   * Gets the health points of this entity.
   *
   * @return The health points of this entity.
   */
  fun getCurrentHealth() = getHealthPoints().current

  /**
   * Adds the given health to this entity.
   *
   * @param health The health to add.
   */
  fun addHealth(health: Int) = getHealthPoints().set(getCurrentHealth() + health)

  /**
   * Gets the health points of this entity.
   *
   * @return The health points of this entity.
   */
  fun getMaxHealth() = getHealthPoints().max

  /**
   * Gets the health points of this entity.
   *
   * @return The health points of this entity.
   */
  fun hasMaxHealth() = getCurrentHealth() == getMaxHealth()
}
