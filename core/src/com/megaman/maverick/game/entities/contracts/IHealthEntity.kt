package com.megaman.maverick.game.entities.contracts

import com.engine.entities.contracts.IPointsEntity
import com.megaman.maverick.game.ConstKeys

interface IHealthEntity : IPointsEntity {

    fun getHealthPoints() = getPoints(ConstKeys.HEALTH)

    fun getCurrentHealth() = getHealthPoints().current

    fun setHealth(health: Int) = getHealthPoints().set(health)

    fun addHealth(health: Int) = getHealthPoints().set(getCurrentHealth() + health)

    fun getMinHealth() = getHealthPoints().min

    fun setMinHealth(minHealth: Int) {
        getHealthPoints().min = minHealth
    }

    fun getMaxHealth() = getHealthPoints().max

    fun setMaxHealth(maxHealth: Int) {
        getHealthPoints().max = maxHealth
    }

    fun hasMaxHealth() = getCurrentHealth() >= getMaxHealth()

    fun hasDepletedHealth() = getCurrentHealth() <= getMinHealth()
}
