package com.mega.game.engine.world.collisions

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IBody

object StandardCollisionHandler : ICollisionHandler {

    private val gameRectOut = GameRectangle()
    private val rectOut1 = Rectangle()
    private val rectOut2 = Rectangle()
    private val overlap = Rectangle()

    override fun handleCollision(body1: IBody, body2: IBody): Boolean {
        val dynamicBody: IBody
        val staticBody: IBody

        if (body1.type == BodyType.DYNAMIC && body2.type == BodyType.STATIC) {
            dynamicBody = body1
            staticBody = body2
        } else if (body2.type == BodyType.DYNAMIC && body1.type == BodyType.STATIC) {
            dynamicBody = body2
            staticBody = body1
        } else return false

        val bounds1 = dynamicBody.getBounds(gameRectOut).get(rectOut1)
        val bounds2 = staticBody.getBounds(gameRectOut).get(rectOut2)
        if (Intersector.intersectRectangles(bounds1, bounds2, overlap)) {
            if (overlap.width > overlap.height) {
                if (dynamicBody.physics.receiveFrictionX)
                    dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x

                if (dynamicBody.getY() > staticBody.getY()) dynamicBody.translate(0f, overlap.height)
                else dynamicBody.translate(0f, -overlap.height)
            } else {
                if (dynamicBody.physics.receiveFrictionY)
                    dynamicBody.physics.frictionOnSelf.y += staticBody.physics.frictionToApply.y

                if (dynamicBody.getX() > staticBody.getX()) dynamicBody.translate(overlap.width, 0f)
                else dynamicBody.translate(-overlap.width, 0f)
            }

            return true
        }

        return false
    }
}
