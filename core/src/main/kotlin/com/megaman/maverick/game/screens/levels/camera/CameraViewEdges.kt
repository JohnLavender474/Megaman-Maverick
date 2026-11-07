package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.CameraEdgeBlock
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.depleteHealth
import com.megaman.maverick.game.entities.hazards.ViewEdgeHazard
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.state.ViewEdgeRule
import com.megaman.maverick.game.state.ViewEdgeRules
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

fun viewEdgeInsideRoom(viewCenter: Vector3, viewWidth: Float, viewHeight: Float, room: Rectangle, edge: Direction):
    Float? {
    val tolerance = 0.5f
    return when (edge) {
        Direction.LEFT -> (viewCenter.x - viewWidth / 2f).takeIf { it > room.x + tolerance }
        Direction.RIGHT -> (viewCenter.x + viewWidth / 2f).takeIf { it < room.x + room.width - tolerance }
        Direction.DOWN -> (viewCenter.y - viewHeight / 2f).takeIf { it > room.y + tolerance }
        Direction.UP -> (viewCenter.y + viewHeight / 2f).takeIf { it < room.y + room.height - tolerance }
    }
}

class CameraViewEdges(
    private val view: CameraView,
    private val viewCenter: Vector3,
    private val players: MegaPlayers,
    private val rules: ViewEdgeRules,
    private val hazard: ViewEdgeHazard,
    private val room: () -> Rectangle?,
    private val enabled: () -> Boolean
) : ICameraPart, Resettable {

    companion object {
        const val BOUNCE_SPEED = 12f
        const val WARP_GUIDE_RADIUS = 0.5f
    }

    private val directions = Direction.entries.toTypedArray()
    private val blocks = ObjectMap<Direction, CameraEdgeBlock>()
    private val outTimes = Array(players.capacity) { FloatArray(directions.size) }
    private val bounds = GameRectangle()

    private val held = BooleanArray(players.capacity)
    private val heldTimes = FloatArray(players.capacity)
    private val heldPositions = Array(players.capacity) { Vector2() }
    private val hasWarpSpot = BooleanArray(players.capacity)
    private val warpSpots = Array(players.capacity) { Vector2() }

    fun isHeld(player: MegaGameEntity) = held[player.playerSlot!!.index]

    override fun update(camera: Camera, delta: Float): Boolean {
        val on = enabled()
        if (!on) releaseAll()
        val room = if (on) room() else null

        for (i in directions.indices) {
            val direction = directions[i]
            val edge = room?.let { viewEdgeInsideRoom(viewCenter, view.viewWidth, view.viewHeight, it, direction) }
            val rule = rules.of(direction)

            if (edge != null && rule == ViewEdgeRule.WALL && !anyPlayerCenterPast(direction, edge))
                placeBlock(direction, edge, room)
            else removeBlock(direction)

            when {
                edge == null -> clearOutTimes(direction)
                rule == ViewEdgeRule.WARP -> checkWarpOut(direction, edge)
                rule == ViewEdgeRule.KILL -> checkKillOut(direction, edge, delta)
                rule == ViewEdgeRule.BOUNCE -> checkBounce(direction, edge)
                else -> clearOutTimes(direction)
            }
        }

        if (on) updateHolds(delta)

        return false
    }

    override fun reset() {
        blocks.values().forEach { if (it.existenceState.liveAfterUpdate) it.destroy() }
        blocks.clear()
        outTimes.forEach { it.fill(0f) }
        releaseAll()
    }

    fun drawWarpGuides(renderer: ShapeRenderer) {
        players.forEach { player ->
            val slot = player.playerSlot!!.index
            if (!held[slot] || !hasWarpSpot[slot]) return@forEach
            val center = player.body.getCenter()
            val spot = warpSpots[slot]
            renderer.color = Color.WHITE
            renderer.line(center.x, center.y, spot.x, spot.y)
            renderer.circle(spot.x, spot.y, WARP_GUIDE_RADIUS * ConstVals.PPM)
        }
    }

    private fun anyPlayerCenterPast(direction: Direction, edge: Float): Boolean {
        players.forEach { player ->
            if (!players.isLive(player)) return@forEach
            val center = player.body.getCenter()
            val past = when (direction) {
                Direction.LEFT -> center.x < edge
                Direction.RIGHT -> center.x > edge
                Direction.DOWN -> center.y < edge
                Direction.UP -> center.y > edge
            }
            if (past) return true
        }
        return false
    }

    private fun placeBlock(direction: Direction, edge: Float, room: Rectangle) {
        cameraEdgeBlockBounds(bounds, direction, edge, room, ConstVals.PPM.toFloat())

        val block = blocks[direction]?.takeIf { it.existenceState.liveAfterUpdate }
        if (block != null) {
            block.body.set(bounds)
            return
        }

        val spawned = MegaEntityFactory.fetch(CameraEdgeBlock::class)!!
        spawned.spawn(
            props(
                ConstKeys.BOUNDS pairTo bounds.copy(),
                ConstKeys.DIRECTION pairTo direction,
                ConstKeys.FEET pairTo (direction == Direction.DOWN)
            )
        )
        blocks.put(direction, spawned)
    }

    private fun removeBlock(direction: Direction) {
        val block = blocks.remove(direction) ?: return
        if (block.existenceState.liveAfterUpdate) block.destroy()
    }

    private fun clearOutTimes(direction: Direction) {
        outTimes.forEach { it[direction.ordinal] = 0f }
    }

    private fun checkKillOut(direction: Direction, edge: Float, delta: Float) {
        players.forEach { player ->
            val times = outTimes[player.playerSlot!!.index]
            if (!players.isLive(player) || !isPastCameraEdge(player.body.getBounds(), direction, edge)) {
                times[direction.ordinal] = 0f
                return@forEach
            }

            times[direction.ordinal] += delta
            if (times[direction.ordinal] < rules.delay) return@forEach
            times[direction.ordinal] = 0f

            player.depleteHealth()
        }
    }

    // TODO: WARP on a side edge is experimental and not tuned: with no side walls, both players steer the group view,
    //  and the play test found it odd; the sides stay WALL by default until more play tests choose a group or a leader
    //  camera for it (item 047, branch 047g play test, 2026-11-06)
    private fun checkWarpOut(direction: Direction, edge: Float) {
        players.forEach { player ->
            if (players.isLive(player) && !isHeld(player) && isPastCameraEdge(player.body.getBounds(), direction, edge))
                hold(player)
        }
    }

    // TODO: a held player is frozen out of view until the warp; the sprite should float back into the view, and
    //  bubbles should move to the warp spot before the warp; this first draft only draws a guide line and circle
    //  (item 047, branch 047g play test, 2026-11-06)
    private fun hold(player: MegaGameEntity) {
        val slot = player.playerSlot!!.index
        held[slot] = true
        heldTimes[slot] = 0f
        heldPositions[slot].set(player.body.getX(), player.body.getY())
        hasWarpSpot[slot] = false

        player.lock(MegaLockGroup.MOVEMENT, MegaLockReason.VIEW_EDGE)
        player.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.VIEW_EDGE)
        player.lock(MegaLockGroup.DAMAGE, MegaLockReason.VIEW_EDGE)
        player.body.physics.gravityOn = false
        player.body.physics.collisionOn = false
        player.body.physics.velocity.setZero()
    }

    private fun release(player: MegaGameEntity) {
        val slot = player.playerSlot!!.index
        if (!held[slot]) return
        held[slot] = false
        hasWarpSpot[slot] = false

        player.unlock(MegaLockGroup.MOVEMENT, MegaLockReason.VIEW_EDGE)
        player.unlock(MegaLockGroup.BEHAVIORS, MegaLockReason.VIEW_EDGE)
        player.unlock(MegaLockGroup.DAMAGE, MegaLockReason.VIEW_EDGE)
        player.body.physics.gravityOn = true
        player.body.physics.collisionOn = true
        player.body.physics.velocity.setZero()
    }

    private fun releaseAll() {
        players.forEach { release(it) }
    }

    private fun updateHolds(delta: Float) {
        players.forEach { player ->
            val slot = player.playerSlot!!.index
            if (!held[slot]) return@forEach
            if (!players.isLive(player)) {
                release(player)
                return@forEach
            }

            player.body.setPosition(heldPositions[slot])
            player.body.physics.velocity.setZero()

            hasWarpSpot[slot] = findWarpSpot(player, warpSpots[slot])
            heldTimes[slot] += delta
            if (heldTimes[slot] < rules.delay || !hasWarpSpot[slot]) return@forEach

            val spot = warpSpots[slot]
            release(player)
            player.body.setCenter(spot.x, spot.y)
            damage(player)
        }
    }

    // TODO: the warp spot is not tested for solids, so a player that warps beside a partner next to a wall or a
    //  platform can land inside it (047g docs/TODO.md, "Backlog candidates")
    private fun findWarpSpot(player: MegaGameEntity, out: Vector2): Boolean {
        var partner: MegaGameEntity? = null
        players.forEach {
            if (partner == null && it != player && players.isLive(it) && !isHeld(it) && isInView(it)) partner = it
        }
        val target = partner ?: return false

        val center = target.body.getCenter()
        val side = if (center.x > viewCenter.x) -1f else 1f
        out.set(center.x + side * ConstVals.PPM, center.y)
        return true
    }

    private fun isInView(player: MegaGameEntity): Boolean {
        val center = player.body.getCenter()
        return center.x > viewCenter.x - view.viewWidth / 2f && center.x < viewCenter.x + view.viewWidth / 2f &&
            center.y > viewCenter.y - view.viewHeight / 2f && center.y < viewCenter.y + view.viewHeight / 2f
    }

    // TODO: the bounce is awkward: a player can be bounced between the edge and a solid, or held at the edge while the
    //  partner holds the view, with no way out; the speed and the rule need tuning after more play tests (item 047,
    //  "Open questions", tall rooms)
    private fun checkBounce(direction: Direction, edge: Float) {
        players.forEach { player ->
            if (!players.isLive(player)) return@forEach

            val body = player.body
            val playerBounds = body.getBounds()
            val velocity = body.physics.velocity
            val speed = BOUNCE_SPEED * ConstVals.PPM

            when (direction) {
                Direction.LEFT -> if (playerBounds.getX() >= edge) return@forEach else {
                    body.setX(edge)
                    velocity.x = speed
                }

                Direction.RIGHT -> if (playerBounds.getMaxX() <= edge) return@forEach else {
                    body.setX(edge - playerBounds.getWidth())
                    velocity.x = -speed
                }

                Direction.DOWN -> if (playerBounds.getY() >= edge) return@forEach else {
                    body.setY(edge)
                    velocity.y = speed
                }

                Direction.UP -> if (playerBounds.getMaxY() <= edge) return@forEach else {
                    body.setY(edge - playerBounds.getHeight())
                    velocity.y = -speed
                }
            }

            damage(player)
        }
    }

    private fun damage(player: MegaGameEntity) {
        if (player is IDamageableEntity && player.canBeDamagedBy(hazard)) player.takeDamageFrom(hazard)
    }
}
