package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.world.body.Body

enum class VelocityAlterationType {
    SET,
    ADD
}

data class VelocityAlteration(
    var forceX: Float = 0f,
    var forceY: Float = 0f,
    var actionX: VelocityAlterationType = VelocityAlterationType.ADD,
    var actionY: VelocityAlterationType = VelocityAlterationType.ADD
) {

    companion object {
        fun add(forceX: Float, forceY: Float) =
            VelocityAlteration(forceX, forceY, VelocityAlterationType.ADD, VelocityAlterationType.ADD)

        fun addNone() = add(0f, 0f)

        fun set(forceX: Float, forceY: Float) =
            VelocityAlteration(forceX, forceY, VelocityAlterationType.SET, VelocityAlterationType.SET)
    }
}

object VelocityAlterator {

    fun alterate(body: Body, alteration: VelocityAlteration) =
        alterate(body.physics.velocity, alteration)

    fun alterate(velocity: Vector2, alteration: VelocityAlteration) {
        velocity.x =
            when (alteration.actionX) {
                VelocityAlterationType.SET -> alteration.forceX
                VelocityAlterationType.ADD -> velocity.x + alteration.forceX
            }
        velocity.y =
            when (alteration.actionY) {
                VelocityAlterationType.SET -> alteration.forceY
                VelocityAlterationType.ADD -> velocity.y + alteration.forceY
            }
    }
}
