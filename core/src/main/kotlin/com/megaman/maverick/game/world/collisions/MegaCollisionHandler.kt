package com.megaman.maverick.game.world.collisions

import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.collisions.ICollisionHandler
import com.mega.game.engine.world.collisions.StandardCollisionHandler
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.world.body.*

class MegaCollisionHandler(private val game: MegamanMaverickGame) : ICollisionHandler {

    private fun trySpecialCollission(body1: IBody, body2: IBody): Boolean {
        if (!body1.physics.collisionOn || !body2.physics.collisionOn) return true

        val staticBody: IBody
        val dynamicBody: IBody

        if (body1.type == BodyType.STATIC && body2.type == BodyType.DYNAMIC) {
            staticBody = body1
            dynamicBody = body2
        } else if (body2.type == BodyType.STATIC && body1.type == BodyType.DYNAMIC) {
            staticBody = body2
            dynamicBody = body1
        } else return false

        val staticBodyFilters = staticBody.getBlockFilters()
        if (staticBodyFilters != null &&
            staticBodyFilters.any { it.invoke(dynamicBody.getEntity(), staticBody.getEntity()) }
        ) return true

        val dynamicBodyFilters = dynamicBody.getBlockFilters()
        if (dynamicBodyFilters != null &&
            dynamicBodyFilters.any { it.invoke(staticBody.getEntity(), dynamicBody.getEntity()) }
        ) return true

        val megaman = game.megaman

        if (staticBody.hasBodyLabel(BodyLabel.PRESS_UP_FALL_THRU) &&
            dynamicBody == megaman.body &&
            megaman.isBehaviorActive(BehaviorType.CLIMBING) &&
            game.controllerPoller.isJustPressed(MegaControllerButton.UP)
        ) {
            dynamicBody.setMaxY(staticBody.getMaxY())
            return true
        }

        if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) {
            if (dynamicBody == megaman.body && megaman.isBehaviorActive(BehaviorType.CLIMBING)) return true

            val dynamicBodyEntity = dynamicBody.getEntity()
            val dynamicBodyDirection =
                if (dynamicBodyEntity is IDirectional) dynamicBodyEntity.direction else Direction.UP

            when (dynamicBodyDirection) {
                Direction.UP -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setY(staticBody.getMaxY())
                        dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                        return true
                    }

                    return dynamicBody.getY() < staticBody.getMaxY()
                }

                Direction.DOWN -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setMaxY(staticBody.getY())
                        dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                        return true
                    }

                    return dynamicBody.getMaxY() > staticBody.getY()
                }

                Direction.LEFT -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setMaxX(staticBody.getX())
                        dynamicBody.physics.frictionOnSelf.y += staticBody.physics.frictionToApply.y
                        return true
                    }

                    return dynamicBody.getMaxX() > staticBody.getX()
                }

                Direction.RIGHT -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setX(staticBody.getMaxX())
                        dynamicBody.physics.frictionOnSelf.y += staticBody.physics.frictionToApply.y
                        return true
                    }

                    return dynamicBody.getX() < staticBody.getMaxX()
                }
            }
        } else if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_UP_ONLY)) {
            // TODO: account for IDirectional logic similar to "collide up only" logic

            if (dynamicBody == megaman.body && megaman.isBehaviorActive(BehaviorType.CLIMBING)) return true

            if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                dynamicBody.setMaxY(staticBody.getY())
                dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                return true
            }

            return dynamicBody.getMaxY() > staticBody.getY()
        }

        return false
    }

    override fun handleCollision(body1: IBody, body2: IBody) =
        trySpecialCollission(body1, body2) || StandardCollisionHandler.handleCollision(body1, body2)
}
