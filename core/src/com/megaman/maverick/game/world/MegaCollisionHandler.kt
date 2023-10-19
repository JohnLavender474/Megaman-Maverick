package com.megaman.maverick.game.world

import com.engine.world.Body
import com.engine.world.ICollisionHandler
import com.engine.world.StandardCollisionHandler

/**
 * Implementation of [ICollisionHandler] that calls a setBodySense of special collision rules if certain
 * conditions are met and otherwise uses the default collision handling of
 * [StandardCollisionHandler].
 */
class MegaCollisionHandler : ICollisionHandler {

  override fun handleCollision(body1: Body, body2: Body): Boolean {
    // handle special collection:
    // check if body1 or body2 is megaman and the specific conditions are met, then handle the
    // collision, otherwise use the default collision handler
    return StandardCollisionHandler.handleCollision(body1, body2)
    /*
    Megaman megaman = game.getMegaman();
    if (staticBody.labels.contains(BodyLabel.PRESS_UP_FALL_THRU) && dynamicBody == megaman.body &&
            !megaman.is(BehaviorType.CLIMBING) && game.getCtrlMan().isJustPressed(CtrlBtn.DPAD_UP)) {
        dynamicBody.setMaxY(staticBody.getMaxY());
        return true;
    }
    if (staticBody.labels.contains(BodyLabel.COLLIDE_DOWN_ONLY)) {
        if (dynamicBody == megaman.body && megaman.is(BehaviorType.CLIMBING)) {
            return true;
        }
        if (dynamicBody.is(BodySense.FEET_ON_GROUND)) {
            dynamicBody.setY(staticBody.getMaxY());
            dynamicBody.resistance.x += staticBody.friction.x;
            return true;
        }
        return dynamicBody.getY() < staticBody.getMaxY();
    } else if (staticBody.labels.contains(BodyLabel.COLLIDE_UP_ONLY)) {
        if (dynamicBody == megaman.body && megaman.is(BehaviorType.CLIMBING)) {
            return true;
        }
        if (dynamicBody.is(BodySense.FEET_ON_GROUND)) {
            dynamicBody.setMaxY(staticBody.getY());
            dynamicBody.resistance.x += staticBody.friction.x;
            return true;
        }
        return dynamicBody.getMaxY() > staticBody.getY();
    }
    return false;
     */
  }
}
