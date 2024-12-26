package com.mega.game.engine.entities.contracts

import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.points.Points
import com.mega.game.engine.points.PointsComponent

interface IPointsEntity : IGameEntity {

    val pointsComponent: PointsComponent
        get() {
            val key = PointsComponent::class
            return getComponent(key)!!
        }

    fun getPoints(key: Any): Points {
        return this.pointsComponent.getPoints(key)
    }

    fun putPoints(key: Any, points: Points): Points? {
        return this.pointsComponent.putPoints(key, points)
    }

    fun putPoints(key: Any, min: Int, max: Int, current: Int): Points? {
        return this.pointsComponent.putPoints(key, Points(min, max, current))
    }

    fun putPoints(key: Any, value: Int): Points? {
        return this.pointsComponent.putPoints(key, Points(0, value, value))
    }

    fun removePoints(key: Any): Points? {
        return this.pointsComponent.removePoints(key)
    }
}
