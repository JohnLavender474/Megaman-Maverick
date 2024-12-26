package com.mega.game.engine.common.interfaces

import com.badlogic.gdx.math.Vector2

interface ISizable {

    fun getWidth(): Float

    fun getHeight(): Float

    fun setSize(size: Float) = setSize(size, size)

    fun setSize(size: Vector2) = setSize(size.x, size.y)

    fun setSize(width: Float, height: Float): ISizable {
        setWidth(width)
        setHeight(height)
        return this
    }

    fun getSize(out: Vector2): Vector2 = out.set(getWidth(), getHeight())

    fun setWidth(width: Float): ISizable

    fun setHeight(height: Float): ISizable

    fun translateSize(width: Float, height: Float) = setSize(getWidth() + width, getHeight() + height)
}
