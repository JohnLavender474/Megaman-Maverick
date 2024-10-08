package com.megaman.maverick.game.world.collisions

import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.collisions.ICollisionHandler
import com.mega.game.engine.world.collisions.StandardCollisionHandler
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButtons
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.world.body.*

class MegaCollisionHandler(private val game: MegamanMaverickGame) : ICollisionHandler {

    private fun trySpecialCollission(body1: Body, body2: Body): Boolean {
        if (!body1.physics.collisionOn || !body2.physics.collisionOn) return true

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

        if (staticBody.hasBlockFilter(dynamicBody.getEntity().getTag().uppercase())) return true

        if (staticBody.hasBodyLabel(BodyLabel.PRESS_UP_FALL_THRU) && dynamicBody == megaman.body &&
            megaman.isBehaviorActive(BehaviorType.CLIMBING) && game.controllerPoller.isJustPressed(MegaControllerButtons.UP)
        ) {
            dynamicBody.setMaxY(staticBody.getMaxY())
            return true
        }

        if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) {
            if (dynamicBody == megaman.body && megaman.isBehaviorActive(BehaviorType.CLIMBING)) return true

            val dynamicBodyEntity = dynamicBody.getEntity()
            val dynamicBodyDirection =
                if (dynamicBodyEntity is IDirectionRotatable) dynamicBodyEntity.directionRotation else Direction.UP

            when (dynamicBodyDirection) {
                Direction.UP, null -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setY(staticBody.getMaxY())
                        dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                        return true
                    }

                    return dynamicBody.y < staticBody.getMaxY()
                }

                Direction.DOWN -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setMaxY(staticBody.y)
                        dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                        return true
                    }

                    return dynamicBody.getMaxY() > staticBody.y
                }

                Direction.LEFT -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setMaxX(staticBody.x)
                        dynamicBody.physics.frictionOnSelf.y += staticBody.physics.frictionToApply.y
                        return true
                    }

                    return dynamicBody.getMaxX() > staticBody.x
                }

                Direction.RIGHT -> {
                    if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                        dynamicBody.setX(staticBody.getMaxX())
                        dynamicBody.physics.frictionOnSelf.y += staticBody.physics.frictionToApply.y
                        return true
                    }

                    return dynamicBody.x < staticBody.getMaxX()
                }
            }
        } else if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_UP_ONLY)) {
            // TODO: account for IDirectionRotatable logic similar to "collide up only" logic

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

    override fun handleCollision(body1: Body, body2: Body) =
        trySpecialCollission(body1, body2) || StandardCollisionHandler.handleCollision(body1, body2)
}
