package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.MathUtils
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.container.IWorldContainer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.levels.LevelState
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBlockFilters
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.hasBodyLabel

enum class GroundAhead { FLOOR, SAFE_DROP, PIT, WALL }

class GroundProbe(private val owner: IBodyEntity, private val levelState: LevelState) {

    companion object {
        const val NO_GROUND = Float.NEGATIVE_INFINITY
        private const val EDGE_TOLERANCE = 0.01f
        private const val FLOOR_TOLERANCE = 0.1f
        private const val SCAN_STRIP_WIDTH = 0.1f
        private val PASSABLE_LABELS = gdxArrayOf(BodyLabel.SLOPE, BodyLabel.COLLIDE_DOWN_ONLY, BodyLabel.COLLIDE_UP_ONLY)
    }

    private val ppm = ConstVals.PPM.toFloat()
    private val localBody = GameRectangle()
    private val localArea = GameRectangle()
    private val worldArea = GameRectangle()
    private val queryArea = GameRectangle()
    private val localQueryArea = GameRectangle()
    private val bodyBounds = GameRectangle()
    private val localBodyBounds = GameRectangle()
    private var queryDirection = Direction.UP
    private var found = false
    private var highestSurface = NO_GROUND

    private val blockingAction: (IBody, IWorldContainer) -> Boolean = { body, _ ->
        if (blocks(body)) found = true
        !found
    }

    private val deathAction: (IFixture, IWorldContainer) -> Boolean = { fixture, _ ->
        if (isActiveDeath(fixture)) found = true
        !found
    }

    private val surfaceAction: (IBody, IWorldContainer) -> Boolean = { body, _ ->
        if (isGround(body)) {
            GravityFrame.toLocal(queryDirection, bodyBounds, localBodyBounds)
            val surface = localBodyBounds.getMaxY()
            if (surface >= localQueryArea.getY() && surface <= localQueryArea.getMaxY() && surface > highestSurface)
                highestSurface = surface
        }
        true
    }

    fun checkAhead(facing: Facing, direction: Direction, lead: Float, maxDrop: Float): GroundAhead {
        GravityFrame.toLocal(direction, owner.body.getBounds(bodyBounds), localBody)
        val tolerance = EDGE_TOLERANCE * ppm
        val floorTolerance = FLOOR_TOLERANCE * ppm
        val stripWidth = SCAN_STRIP_WIDTH * ppm
        val width = localBody.getWidth()
        val foot = localBody.getY()
        val front = if (facing == Facing.RIGHT) localBody.getMaxX() else localBody.getX()
        val reach = lead + stripWidth
        val pathX = if (facing == Facing.RIGHT) front else front - reach
        val stripX = if (facing == Facing.RIGHT) front + lead else front - reach
        val catchX = if (facing == Facing.RIGHT) front + lead else front - lead - width

        localArea.set(pathX, foot + floorTolerance, reach, localBody.getHeight() - floorTolerance - tolerance)
        if (overlapsBlockingSolid(GravityFrame.toWorld(direction, localArea, worldArea))) return GroundAhead.WALL

        localArea.set(pathX, foot + tolerance, reach, localBody.getHeight() - 2f * tolerance)
        if (overlapsDeath(GravityFrame.toWorld(direction, localArea, worldArea))) return GroundAhead.PIT

        localArea.set(stripX, foot - floorTolerance, stripWidth, 2f * floorTolerance)
        if (findGroundSurface(GravityFrame.toWorld(direction, localArea, worldArea), direction) != NO_GROUND)
            return GroundAhead.FLOOR

        localArea.set(catchX, foot - maxDrop, width, maxDrop + floorTolerance)
        val surface = findGroundSurface(GravityFrame.toWorld(direction, localArea, worldArea), direction)
        if (surface == NO_GROUND) return GroundAhead.PIT

        localArea.set(catchX, surface, width, foot - surface)
        if (overlapsDeath(GravityFrame.toWorld(direction, localArea, worldArea))) return GroundAhead.PIT
        return GroundAhead.SAFE_DROP
    }

    fun overlapsBlockingSolid(area: GameRectangle): Boolean {
        queryArea.set(area)
        found = false
        container().forEachBody(minCellX(), minCellY(), maxCellX(), maxCellY(), blockingAction)
        return found
    }

    fun overlapsDeath(area: GameRectangle): Boolean {
        queryArea.set(area)
        found = false
        container().forEachFixture(minCellX(), minCellY(), maxCellX(), maxCellY(), deathAction)
        return found
    }

    fun findGroundSurface(area: GameRectangle, direction: Direction): Float {
        queryArea.set(area)
        queryDirection = direction
        GravityFrame.toLocal(direction, area, localQueryArea)
        highestSurface = NO_GROUND
        container().forEachBody(minCellX(), minCellY(), maxCellX(), maxCellY(), surfaceAction)
        return highestSurface
    }

    private fun container() = levelState.worldContainer
        ?: throw IllegalStateException("No world container: the level screen sets it at level start")

    private fun minCellX() = MathUtils.floor((queryArea.getX() - EDGE_TOLERANCE * ppm) / ppm)

    private fun minCellY() = MathUtils.floor((queryArea.getY() - EDGE_TOLERANCE * ppm) / ppm)

    private fun maxCellX() = MathUtils.floor((queryArea.getMaxX() + EDGE_TOLERANCE * ppm) / ppm)

    private fun maxCellY() = MathUtils.floor((queryArea.getMaxY() + EDGE_TOLERANCE * ppm) / ppm)

    private fun blocks(body: IBody): Boolean {
        if (!isSolidFor(body)) return false
        return PASSABLE_LABELS.none { body.hasBodyLabel(it) }
    }

    // TODO: COLLIDE_UP_ONLY is never ground here, also for a turned entity under it; it follows the direction TODO in MegaCollisionHandler
    private fun isGround(body: IBody): Boolean {
        if (!isSolidFor(body)) return false
        return !body.hasBodyLabel(BodyLabel.COLLIDE_UP_ONLY)
    }

    private fun isSolidFor(body: IBody): Boolean {
        if (body.type != BodyType.STATIC || !body.physics.collisionOn || !body.getBounds(bodyBounds).overlaps(queryArea))
            return false
        val solidFilters = body.getBlockFilters()
        val ownerFilters = owner.body.getBlockFilters()
        if ((solidFilters == null || solidFilters.isEmpty) && (ownerFilters == null || ownerFilters.isEmpty)) return true
        val solid = body.getEntity()
        if (solidFilters != null && solidFilters.any { it.invoke(owner, solid) }) return false
        if (ownerFilters != null && ownerFilters.any { it.invoke(solid, owner) }) return false
        return true
    }

    private fun isActiveDeath(fixture: IFixture) =
        fixture.getType() == FixtureType.DEATH && fixture.isActive() && fixture.getShape().overlaps(queryArea)
}
