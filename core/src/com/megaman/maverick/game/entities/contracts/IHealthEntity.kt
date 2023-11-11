package com.megaman.maverick.game.entities.contracts

import com.engine.entities.contracts.IPointsEntity
import com.megaman.maverick.game.ConstKeys

/** An entity containing health points. */
interface IHealthEntity : IPointsEntity {

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
