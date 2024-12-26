package com.mega.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.interfaces.IRectangle

fun Sprite.setPosition(position: Vector2) = setPosition(position.x, position.y)

fun Sprite.setPosition(p: Vector2, pos: Position) {
    when (pos) {
        Position.BOTTOM_LEFT -> setPosition(p.x, p.y)
        Position.BOTTOM_CENTER -> setPosition(p.x - width / 2f, p.y)
        Position.BOTTOM_RIGHT -> setPosition(p.x - width, p.y)
        Position.CENTER_LEFT -> {
            setCenter(p.x, p.y)
            x += width / 2f
        }

        Position.CENTER -> setCenter(p.x, p.y)
        Position.CENTER_RIGHT -> {
            setCenter(p.x, p.y)
            x -= width / 2f
        }

        Position.TOP_LEFT -> setPosition(p.x, p.y - height)
        Position.TOP_CENTER -> {
            setCenter(p.x, p.y)
            y -= height / 2f
        }

        Position.TOP_RIGHT -> setPosition(p.x - width, p.y - height)
    }
}

fun Sprite.setPosition(p: Vector2, pos: Position, xOffset: Float, yOffset: Float) {
    setPosition(p, pos)
    translate(xOffset, yOffset)
}

fun Sprite.setPosition(p: Vector2, pos: Position, offset: Vector2) = setPosition(p, pos, offset.x, offset.y)

fun Sprite.setSize(size: Float) = setSize(size, size)

fun Sprite.setSize(size: Vector2) = setSize(size.x, size.y)

fun Sprite.setBounds(bounds: Rectangle) = setBounds(bounds.x, bounds.y, bounds.width, bounds.height)

fun Sprite.setBounds(bounds: IRectangle) = setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())

fun Sprite.setCenter(center: Vector2) = setCenter(center.x, center.y)
