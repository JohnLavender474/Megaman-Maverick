package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.math.MathUtils
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.container.IWorldContainer
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.players.playerAllowsDamageFrom
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBlockFilters
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.hasBodyLabel

class WarpSpotTest(private val ppm: Float, private val world: () -> IWorldContainer?) {

    companion object {
        private const val EDGE_TOLERANCE = 0.01f
        private const val GROUND_DEPTH = 1f
        private const val GROUND_WIDTH = 0.5f
        private val PASSABLE_LABELS = arrayOf(BodyLabel.SLOPE, BodyLabel.COLLIDE_DOWN_ONLY, BodyLabel.COLLIDE_UP_ONLY)
        private val UNSAFE_FIXTURES = arrayOf(FixtureType.DEATH, FixtureType.TELEPORTER)
    }

    private val area = GameRectangle()
    private val bodyBounds = GameRectangle()
    private var player: MegaGameEntity? = null
    private var found = false

    private val blockAction: (IBody, IWorldContainer) -> Boolean = { body, _ ->
        if (blocks(body)) found = true
        !found
    }

    private val hurtAction: (IFixture, IWorldContainer) -> Boolean = { fixture, _ ->
        if (hurts(fixture)) found = true
        !found
    }

    private val groundAction: (IBody, IWorldContainer) -> Boolean = { body, _ ->
        if (isGround(body)) found = true
        !found
    }

    private val waterAction: (IFixture, IWorldContainer) -> Boolean = { fixture, _ ->
        if (fixture.getType() == FixtureType.WATER && fixture.getShape().overlaps(area)) found = true
        !found
    }

    fun isSafe(player: MegaGameEntity, centerX: Float, centerY: Float): Boolean {
        if (!isFree(player, centerX, centerY)) return false
        return !findFixture(player, hurtAction)
    }

    fun isFree(player: MegaGameEntity, centerX: Float, centerY: Float): Boolean {
        setArea(player, centerX, centerY)
        return !findBody(player, blockAction)
    }

    fun hasGround(player: MegaGameEntity, centerX: Float, centerY: Float): Boolean {
        setArea(player, centerX, centerY)
        if (findFixture(player, waterAction)) return true
        val feet = area.getY() - EDGE_TOLERANCE * ppm
        area.set(
            centerX - GROUND_WIDTH * ppm / 2f, feet - GROUND_DEPTH * ppm,
            GROUND_WIDTH * ppm, GROUND_DEPTH * ppm + 2f * EDGE_TOLERANCE * ppm
        )
        if (findFixture(player, hurtAction)) return false
        return findBody(player, groundAction)
    }

    private fun setArea(player: MegaGameEntity, centerX: Float, centerY: Float) {
        val size = player.body
        val tolerance = EDGE_TOLERANCE * ppm
        area.setSize(size.getWidth() - 2f * tolerance, size.getHeight() - 2f * tolerance).setCenter(centerX, centerY)
    }

    private fun findBody(player: MegaGameEntity, action: (IBody, IWorldContainer) -> Boolean): Boolean {
        val container = container()
        this.player = player
        found = false
        container.forEachBody(minCellX(), minCellY(), maxCellX(), maxCellY(), action)
        this.player = null
        return found
    }

    private fun findFixture(player: MegaGameEntity, action: (IFixture, IWorldContainer) -> Boolean): Boolean {
        val container = container()
        this.player = player
        found = false
        container.forEachFixture(minCellX(), minCellY(), maxCellX(), maxCellY(), action)
        this.player = null
        return found
    }

    private fun container() = world()
        ?: throw IllegalStateException("No world container: the level screen sets it at level start")

    private fun minCellX() = MathUtils.floor(area.getX() / ppm)

    private fun minCellY() = MathUtils.floor(area.getY() / ppm)

    private fun maxCellX() = MathUtils.floor(area.getMaxX() / ppm)

    private fun maxCellY() = MathUtils.floor(area.getMaxY() / ppm)

    private fun blocks(body: IBody): Boolean {
        if (!isSolidFor(body)) return false
        return PASSABLE_LABELS.none { body.hasBodyLabel(it) }
    }

    private fun isGround(body: IBody): Boolean {
        if (!isSolidFor(body)) return false
        return !body.hasBodyLabel(BodyLabel.COLLIDE_UP_ONLY)
    }

    private fun isSolidFor(body: IBody): Boolean {
        if (body.type != BodyType.STATIC || !body.physics.collisionOn || !body.getBounds(bodyBounds).overlaps(area))
            return false
        val target = player!!
        val solid = body.getEntity()
        if (body.getBlockFilters()?.any { it.invoke(target, solid) } == true) return false
        if (target.body.getBlockFilters()?.any { it.invoke(solid, target) } == true) return false
        return true
    }

    private fun hurts(fixture: IFixture): Boolean {
        val target = player!!
        val type = fixture.getType()
        if (type !in UNSAFE_FIXTURES && type != FixtureType.DAMAGER) return false
        val owner = fixture.getEntity()
        if (owner == target || !fixture.getShape().overlaps(area)) return false
        if (type in UNSAFE_FIXTURES) return true
        return owner is IDamagerEntity && target.playerAllowsDamageFrom(owner)
    }
}
