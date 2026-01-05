package com.mega.game.engine.world.body

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.interfaces.ICopyable
import com.mega.game.engine.common.interfaces.Resettable

class PhysicsData(
    var gravity: Vector2 = Vector2(),
    var velocity: Vector2 = Vector2(),
    var velocityClamp: Vector2 = Vector2(Float.MAX_VALUE, Float.MAX_VALUE),
    var frictionToApply: Vector2 = Vector2(),
    var frictionOnSelf: Vector2 = Vector2(1f, 1f),
    var defaultFrictionOnSelf: Vector2 = Vector2(1f, 1f),
    var gravityOn: Boolean = true,
    var collisionOn: Boolean = true,
    var applyFrictionX: Boolean = true,
    var applyFrictionY: Boolean = true,
    var receiveFrictionX: Boolean = true,
    var receiveFrictionY: Boolean = true
) : Resettable, ICopyable<PhysicsData> {

    override fun copy() =
        PhysicsData(
            Vector2(gravity),
            Vector2(velocity),
            Vector2(velocityClamp),
            Vector2(frictionToApply),
            Vector2(frictionOnSelf),
            Vector2(defaultFrictionOnSelf),
            gravityOn,
            collisionOn,
            applyFrictionX,
            applyFrictionY
        )

    override fun reset() {
        velocity.setZero()
        frictionOnSelf.set(defaultFrictionOnSelf)
    }

    override fun toString() =
        "PhysicsData(gravity=$gravity, velocity=$velocity, velocityClamp=$velocityClamp, " +
                "frictionToApply=$frictionToApply, frictionOnSelf=$frictionOnSelf, " +
                "defaultFrictionOnSelf=$defaultFrictionOnSelf, gravityOn=$gravityOn, " +
                "collisionOn=$collisionOn, applyFrictionX=$applyFrictionX, applyFrictionY=$applyFrictionY)"
}
