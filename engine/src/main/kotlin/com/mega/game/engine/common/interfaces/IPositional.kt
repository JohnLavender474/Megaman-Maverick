package com.mega.game.engine.common.interfaces

import com.badlogic.gdx.math.Vector2

interface IPositional: IPositionSupplier {

    fun setX(x: Float)

    fun setY(y: Float)

    fun setPosition(x: Float, y: Float) {
        setX(x)
        setY(y)
    }

    fun setPosition(position: Vector2) = setPosition(position.x, position.y)
}