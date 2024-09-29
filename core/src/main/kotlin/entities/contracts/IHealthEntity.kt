package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.mega.game.engine.entities.contracts.IPointsEntity
import com.mega.game.engine.points.Points
import com.megaman.maverick.game.ConstKeys

interface IHealthEntity : IPointsEntity {

    fun getHealthRatio() = getCurrentHealth().toFloat() / getMaxHealth().toFloat()

    fun getHealthPoints(): Points = getPoints(ConstKeys.HEALTH)

    fun getCurrentHealth() = getHealthPoints().current

    fun setHealth(health: Int) = getHealthPoints().set(health)

    fun translateHealth(health: Int) = getHealthPoints().set(getCurrentHealth() + health)

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
