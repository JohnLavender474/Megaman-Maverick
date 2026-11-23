package com.megaman.maverick.game.world.collisions

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.collisions.ICollisionHandler
import com.maverick.game.engine.world.collisions.StandardCollisionHandler
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.blocks.CeilingPolygonBlock
import com.megaman.maverick.game.entities.blocks.GroundSlope
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class MegaCollisionHandler : ICollisionHandler {

    companion object {
        private const val SLOPE_EDGE_TOLERANCE = 0.01f * ConstVals.PPM
    }

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

        val staticEntity = staticBody.getEntity()
        val dynamicEntity = dynamicBody.getEntity()

        val staticBodyFilters = staticBody.getBlockFilters()
        if (staticBodyFilters != null &&
            staticBodyFilters.any { it.invoke(dynamicEntity, staticEntity) }
        ) return true

        val dynamicBodyFilters = dynamicBody.getBlockFilters()
        if (dynamicBodyFilters != null &&
            dynamicBodyFilters.any { it.invoke(staticEntity, dynamicEntity) }
        ) return true

        // TODO: the ceiling and slope checks here run for every static-dynamic collision in every level (accepted O5-R2
        //  gap of 068a; 182a measures with one --diagnostics pair, see docs/mega-refactor/objectives/O5-performance.md; item 009)
        if (staticBody.hasBodyLabel(BodyLabel.CEILING)) {
            val ceiling = try {
                staticEntity as CeilingPolygonBlock
            } catch (e: ClassCastException) {
                throw InvalidCeilingLabelException(staticEntity, e)
            }
            if (directionOf(dynamicEntity) == Direction.UP) ceiling.collide(dynamicBody)
            return true
        }

        if (staticBody.hasBodyLabel(BodyLabel.SLOPE)) {
            val slope = try {
                staticEntity as GroundSlope
            } catch (e: ClassCastException) {
                throw InvalidSlopeLabelException(staticEntity, e)
            }
            if (directionOf(dynamicEntity) != Direction.UP) return false
            collideWithSlope(dynamicBody, staticBody, slope)
            return true
        }

        if (staticBody.hasBodyLabel(BodyLabel.PRESS_UP_FALL_THRU) &&
            dynamicBody.getProperty(BodyKeys.FALL_THROUGH_PRESS_UP)?.invoke() == true
        ) {
            dynamicBody.setMaxY(staticBody.getMaxY())
            return true
        }

        if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) {
            if (dynamicBody.getProperty(BodyKeys.PASS_THROUGH_ONE_WAY)?.invoke() == true) return true

            when (directionOf(dynamicEntity)) {
                Direction.UP -> {
                    if (dynamicBody.physics.velocity.y > 0f) return true

                    if (dynamicBody.hasFeetBlock(staticEntity as Block)) {
                        dynamicBody.setY(staticBody.getMaxY())
                        // TODO: a body across a wrap edge on a one-way block that spans both edges gets this friction twice per step (item 109)
                        dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                        return true
                    }

                    return dynamicBody.getY() < staticBody.getMaxY()
                }
                Direction.DOWN -> {
                    if (dynamicBody.physics.velocity.y < 0f) return true

                    if (dynamicBody.hasFeetBlock(staticEntity as Block)) {
                        dynamicBody.setMaxY(staticBody.getY())
                        dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                        return true
                    }

                    return dynamicBody.getMaxY() > staticBody.getY()
                }
                Direction.LEFT -> {
                    if (dynamicBody.physics.velocity.x < 0f) return true

                    if (dynamicBody.hasFeetBlock(staticEntity as Block)) {
                        dynamicBody.setMaxX(staticBody.getX())
                        dynamicBody.physics.frictionOnSelf.y += staticBody.physics.frictionToApply.y
                        return true
                    }

                    return dynamicBody.getMaxX() > staticBody.getX()
                }
                Direction.RIGHT -> {
                    if (dynamicBody.physics.velocity.x > 0f) return true

                    if (dynamicBody.hasFeetBlock(staticEntity as Block)) {
                        dynamicBody.setX(staticBody.getMaxX())
                        dynamicBody.physics.frictionOnSelf.y += staticBody.physics.frictionToApply.y
                        return true
                    }

                    return dynamicBody.getX() < staticBody.getMaxX()
                }
            }
        } else if (staticBody.hasBodyLabel(BodyLabel.COLLIDE_UP_ONLY)) {
            // TODO: account for IDirectional logic similar to "collide down only" logic

            if (dynamicBody.getProperty(BodyKeys.PASS_THROUGH_ONE_WAY)?.invoke() == true) return true

            if (dynamicBody.isSensing(BodySense.FEET_ON_GROUND)) {
                dynamicBody.setMaxY(staticBody.getY())
                dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
                return true
            }

            return dynamicBody.getMaxY() > staticBody.getY()
        }

        if (standsOnSlopeBeside(dynamicBody, dynamicEntity, staticBody)) return true

        return false
    }

    private fun directionOf(entity: MegaGameEntity) =
        if (entity is IDirectional) entity.direction else Direction.UP

    private fun bottomCenterX(body: IBody) = body.getX() + body.getWidth() / 2f

    private fun collideWithSlope(dynamicBody: IBody, staticBody: IBody, slope: GroundSlope) {
        val x = bottomCenterX(dynamicBody)
        if (x < staticBody.getX() || x > staticBody.getMaxX()) return
        if (dynamicBody.physics.velocity.y > 0f) return

        val surface = slope.surfaceAt(x)
        if (dynamicBody.getY() < surface) {
            dynamicBody.setY(surface)
            if (dynamicBody.physics.receiveFrictionX)
                dynamicBody.physics.frictionOnSelf.x += staticBody.physics.frictionToApply.x
        }
    }

    private fun standsOnSlopeBeside(dynamicBody: IBody, dynamicEntity: MegaGameEntity, staticBody: IBody): Boolean {
        if (!dynamicBody.hasProperty(ConstKeys.FEET_BLOCKS)) return false
        if (directionOf(dynamicEntity) != Direction.UP) return false

        val x = bottomCenterX(dynamicBody)
        for (block in dynamicBody.getFeetBlocks()) {
            if (block !is GroundSlope) continue

            val slopeBody = block.body
            if (x < slopeBody.getX() || x > slopeBody.getMaxX()) continue
            if (abs(staticBody.getMaxY() - slopeBody.getMaxY()) > SLOPE_EDGE_TOLERANCE) continue

            val touchesHighSide =
                if (block.highOnRight) abs(staticBody.getX() - slopeBody.getMaxX()) <= SLOPE_EDGE_TOLERANCE
                else abs(staticBody.getMaxX() - slopeBody.getX()) <= SLOPE_EDGE_TOLERANCE
            if (touchesHighSide) return true
        }

        return false
    }

    override fun handleCollision(body1: IBody, body2: IBody) =
        trySpecialCollission(body1, body2) || StandardCollisionHandler.handleCollision(body1, body2)
}
