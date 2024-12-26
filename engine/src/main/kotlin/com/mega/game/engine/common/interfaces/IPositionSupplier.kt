package com.mega.game.engine.common.interfaces

import com.badlogic.gdx.math.Vector2

interface IPositionSupplier {

    fun getX(): Float

    fun getY(): Float

    fun getPosition(out: Vector2) = out.set(getX(), getY())
}
