package com.megaman.maverick.game.world

import com.engine.world.Body
import com.engine.world.BodyType
import com.engine.world.ICollisionHandler
import com.engine.world.StandardCollisionHandler
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.behaviors.BehaviorType

class MegaCollisionHandler(private val game: MegamanMaverickGame) : ICollisionHandler {

    private fun trySpecialCollission(body1: Body, body2: Body): Boolean {
        val megaman = game.megaman

        val staticBody: Body
        val dynamicBody: Body

        if (body1.bodyType == BodyType.STATIC && body2.bodyType == BodyType.DYNAMIC) {
            staticBody = body1
            dynamicBody = body2
        } else if (body2.bodyType == BodyType.STATIC && body1.bodyType == BodyType.DYNAMIC) {
            staticBody = body2
            dynamicBody = body1
        } else return false

        if (staticBody.getEntity().getProperty(ConstKeys.PARENT) == dynamicBody.getEntity()) return true

        if (staticBody.hasBodyLabel(BodyLabel.PRESS_UP_FALL_THRU) &&
            dynamicBody == megaman.body &&
            !megaman.isBehaviorActive(BehaviorType.CLIMBING) &&
            game.controllerPoller.isJustPressed(ControllerButton.UP)
        ) {
            dynamicBody.setMaxY(staticBody.getMaxY())
            return true
        }

        if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) {
            if (dynamicBody == megaman.body && megaman.isBehaviorActive(BehaviorType.CLIMBING))
                return true

            if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                dynamicBody.setY(staticBody.getMaxY())
                dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                return true
            }

            return dynamicBody.getY() < staticBody.getMaxY()
        } else if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_UP_ONLY)) {
            if (dynamicBody == megaman.body && megaman.isBehaviorActive(BehaviorType.CLIMBING))
                return true

            if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                dynamicBody.setMaxY(staticBody.getY())
                dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                return true
            }

            return dynamicBody.getMaxY() > staticBody.getY()
        }

        return false
    }

    override fun handleCollision(body1: Body, body2: Body) =
        trySpecialCollission(body1, body2) || StandardCollisionHandler.handleCollision(body1, body2)
}
