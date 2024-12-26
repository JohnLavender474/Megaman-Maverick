package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Shape2D
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.interfaces.ICopyable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.drawables.shapes.IDrawableShape

interface IGameShape2D : Shape2D, IDrawableShape, ICopyable<IGameShape2D> {

    fun getProps(out: Properties): Properties

    fun setWithProps(props: Properties): IGameShape2D

    fun overlaps(other: IGameShape2D): Boolean

    fun getBoundingRectangle(out: GameRectangle): GameRectangle

    fun getMaxX(): Float

    fun getMaxY(): Float
    
    fun setCenter(x: Float, y: Float): IGameShape2D

    fun setCenter(center: Vector2) = setCenter(center.x, center.y)

    fun getCenter(out: Vector2): Vector2

    fun getX(): Float

    fun getY(): Float

    fun setX(x: Float): IGameShape2D

    fun setY(y: Float): IGameShape2D

    fun setPosition(x: Float, y: Float): IGameShape2D {
        setX(x)
        setY(y)
        return this
    }

    fun setPosition(position: Vector2) = setPosition(position.x, position.y)

    fun translate(x: Float, y: Float) = setPosition(getX() + x, getY() + y)

    fun translate(delta: Vector2) = translate(delta.x, delta.y)
}
