package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstVals

class JumpPlan {
    val worldVelocity = Vector2()
}

class JumpPlanner(
    private val owner: IBodyEntity,
    private val probe: GroundProbe,
    private val gravity: Float,
    private val maxSpeedForward: Float,
    private val maxSpeedUp: Float,
    private val maxFlightTime: Float,
    private val fixedStep: () -> Float
) {

    companion object {
        private const val EDGE_TOLERANCE = 0.01f
        private const val FLOOR_TOLERANCE = 0.1f
    }

    private val ppm = ConstVals.PPM.toFloat()
    private val bodyBounds = GameRectangle()
    private val arc = GameRectangle()
    private val localArea = GameRectangle()
    private val worldArea = GameRectangle()
    private val worldLanding = GameRectangle()

    fun plan(
        facing: Facing,
        direction: Direction,
        candidates: Array<Vector2>,
        goal: (landing: GameRectangle) -> Boolean,
        out: JumpPlan
    ): Boolean {
        for (i in 0 until candidates.size) {
            val candidate = candidates[i]
            val localX = facing.value * candidate.x * ppm
            val localY = candidate.y * ppm
            if (!flies(direction, localX, localY, goal)) continue
            GravityFrame.velocityToWorld(direction, localX, localY, out.worldVelocity)
            return true
        }
        return false
    }

    private fun flies(direction: Direction, startX: Float, startY: Float, goal: (GameRectangle) -> Boolean): Boolean {
        GravityFrame.toLocal(direction, owner.body.getBounds(bodyBounds), arc)
        val tolerance = EDGE_TOLERANCE * ppm
        val step = fixedStep()
        val pull = gravity * ppm
        val clampX = maxSpeedForward * ppm
        val clampY = maxSpeedUp * ppm
        var velocityX = startX
        var velocityY = startY
        var time = 0f
        while (time < maxFlightTime) {
            velocityY = (velocityY - pull * step).coerceIn(-clampY, clampY)
            velocityX = velocityX.coerceIn(-clampX, clampX)
            val oldFoot = arc.getY()
            arc.translate(velocityX * step, velocityY * step)
            time += step

            if (velocityY <= 0f) {
                localArea.set(
                    arc.getX() + tolerance, arc.getY(), arc.getWidth() - 2f * tolerance, oldFoot - arc.getY() + tolerance
                )
                val surface = probe.findGroundSurface(GravityFrame.toWorld(direction, localArea, worldArea), direction)
                if (surface != GroundProbe.NO_GROUND) {
                    arc.setY(surface)
                    return lands(direction, goal)
                }
            }

            if (hitsSolidOrDeath(direction)) return false
        }
        return false
    }

    private fun lands(direction: Direction, goal: (GameRectangle) -> Boolean): Boolean {
        if (hitsSolidOrDeath(direction)) return false
        val tolerance = EDGE_TOLERANCE * ppm
        val floorTolerance = FLOOR_TOLERANCE * ppm
        localArea.set(arc.getX() + tolerance, arc.getY() - floorTolerance, arc.getWidth() - 2f * tolerance, floorTolerance)
        if (probe.overlapsDeath(GravityFrame.toWorld(direction, localArea, worldArea))) return false
        return goal(GravityFrame.toWorld(direction, arc, worldLanding))
    }

    private fun hitsSolidOrDeath(direction: Direction): Boolean {
        val tolerance = EDGE_TOLERANCE * ppm
        localArea.set(
            arc.getX() + tolerance, arc.getY() + tolerance,
            arc.getWidth() - 2f * tolerance, arc.getHeight() - 2f * tolerance
        )
        GravityFrame.toWorld(direction, localArea, worldArea)
        return probe.overlapsBlockingSolid(worldArea) || probe.overlapsDeath(worldArea)
    }
}
