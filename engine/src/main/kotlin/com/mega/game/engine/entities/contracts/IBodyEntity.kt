package com.mega.game.engine.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.interfaces.IRectangle
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent

interface IBodyEntity : IGameEntity, IRectangle {

    val bodyComponent: BodyComponent
        get() {
            val key = BodyComponent::class
            return getComponent(key)!!
        }

    val body: Body
        get() = bodyComponent.body

    override fun setSize(size: Float): Body {
        body.setSize(size)
        return body
    }

    override fun set(
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ): Body {
        body.set(x, y, width, height)
        return body
    }

    override fun getX() = body.getX()

    override fun setX(x: Float): Body {
        body.setX(x)
        return body
    }

    override fun getY() = body.getY()

    override fun setY(y: Float): Body {
        body.setY(y)
        return body
    }

    override fun setPosition(x: Float, y: Float): Body {
        body.setPosition(x, y)
        return body
    }

    override fun getCenter(out: Vector2) = body.getCenter(out)

    override fun setCenter(center: Vector2) = body.setCenter(center)

    override fun setCenter(x: Float, y: Float) = body.setCenter(x, y)

    override fun getWidth() = body.getWidth()

    override fun setWidth(width: Float): Body {
        body.setWidth(width)
        return body
    }

    override fun getHeight() = body.getHeight()

    override fun setHeight(height: Float): Body {
        body.setHeight(height)
        return body
    }

    override fun setSize(width: Float, height: Float): Body {
        body.setSize(width, height)
        return body
    }

    override fun translateSize(width: Float, height: Float): Body {
        body.setWidth(body.getWidth() + width)
        body.setHeight(body.getHeight() + height)
        return body
    }

    override fun translate(x: Float, y: Float): Body {
        body.translate(x, y)
        return body
    }
}