package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.CameraRoomBounds
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.forEach
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.CameraEdgeBlock
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SideExit {
    var side = Direction.RIGHT
    var next: RectangleMapObject? = null
    var edge = 0f
    var low = 0f
    var high = 0f
}

private const val TOUCH_TOLERANCE = 0.5f

fun findSideExits(room: RectangleMapObject, rooms: Array<RectangleMapObject>, out: Array<SideExit>): Int {
    val r = room.rectangle
    var count = 0
    for (other in rooms) {
        if (other === room) continue
        val o = other.rectangle
        val side = when {
            abs(o.x - (r.x + r.width)) <= TOUCH_TOLERANCE -> Direction.RIGHT
            abs((o.x + o.width) - r.x) <= TOUCH_TOLERANCE -> Direction.LEFT
            else -> continue
        }
        val low = max(r.y, o.y)
        val high = min(r.y + r.height, o.y + o.height)
        if (high - low <= TOUCH_TOLERANCE) continue

        if (count == out.size) out.add(SideExit())
        val exit = out[count++]
        exit.side = side
        exit.next = other
        exit.edge = if (side == Direction.RIGHT) r.x + r.width else r.x
        exit.low = low
        exit.high = high
    }
    return count
}

// TODO: only side exits hold the players; a top or bottom room edge (a ladder, a jump or a fall into another room)
//  still lets one player leave alone; phase 3 of the shared screen (item 047, "Decisions", 2026-11-06)
class CameraRoomExits(
    private val players: MegaPlayers,
    private val roomBounds: CameraRoomBounds,
    private val enabled: () -> Boolean,
    private val held: (IPlayer) -> Boolean = { false }
) : ICameraPart, Resettable {

    companion object {
        const val AT_EXIT_TOLERANCE = 0.1f
    }

    val gates = Array<Rectangle>()

    private val exits = Array<SideExit>()
    private var count = 0
    private var room: RectangleMapObject? = null
    private val blocks = Array<CameraEdgeBlock?>()
    private val bounds = GameRectangle()

    override fun update(camera: Camera, delta: Float): Boolean {
        val current = roomBounds.currentGameRoom
        if (!enabled() || current == null || liveCount() < 2) {
            removeBlocks()
            return false
        }

        if (current !== room) {
            removeBlocks()
            room = current
            count = findSideExits(current, roomBounds.gameRooms!!, exits)
        }

        for (i in 0 until count) {
            val exit = exits[i]
            if (coveredByGate(exit)) {
                removeBlock(i)
                continue
            }

            if (everyoneAt(exit)) {
                removeBlocks()
                room = null
                // TODO: a held player is not at the exit but also keeps its own height; this happens only with 3 or
                //  more players (item 047, branch 047i)
                roomBounds.transitionToRoom(camera, exit.next!!.name, exit.side, axisOnly = true)
                return false
            }

            placeBlock(i, exit)
        }

        return false
    }

    override fun reset() {
        removeBlocks()
        room = null
        count = 0
    }

    private fun live(player: IPlayer) = players.isLive(player) && !held(player)

    private fun liveCount(): Int {
        var n = 0
        players.forEach { if (live(it)) n++ }
        return n
    }

    private fun everyoneAt(exit: SideExit): Boolean {
        val tolerance = AT_EXIT_TOLERANCE * ConstVals.PPM
        players.forEach { player ->
            if (!live(player)) return@forEach
            val body = player.body.getBounds()
            val reached = when (exit.side) {
                Direction.RIGHT -> body.getMaxX() >= exit.edge - tolerance
                else -> body.getX() <= exit.edge + tolerance
            }
            val centerY = player.body.getCenter().y
            if (!reached || centerY < exit.low || centerY > exit.high) return false
        }
        return true
    }

    // TODO: an exit that a gate covers gets no wall, so the gate keeps its own trigger, which any one player starts;
    //  gates with 2 players are phase 4 of the shared screen (item 047, "Decisions", 2026-11-06)
    private fun coveredByGate(exit: SideExit): Boolean {
        val thickness = ConstVals.PPM.toFloat()
        for (i in 0 until gates.size) {
            val gate = gates[i]
            val overlapsX = gate.x < exit.edge + thickness && gate.x + gate.width > exit.edge - thickness
            val overlapsY = gate.y < exit.high && gate.y + gate.height > exit.low
            if (overlapsX && overlapsY) return true
        }
        return false
    }

    private fun placeBlock(index: Int, exit: SideExit) {
        while (blocks.size <= index) blocks.add(null)
        if (blocks[index]?.existenceState?.liveAfterUpdate == true) return

        val thickness = ConstVals.PPM.toFloat()
        val x = if (exit.side == Direction.RIGHT) exit.edge else exit.edge - thickness
        bounds.set(x, exit.low, thickness, exit.high - exit.low)

        val block = MegaEntityFactory.fetch(CameraEdgeBlock::class)!!
        block.spawn(props(ConstKeys.BOUNDS pairTo bounds.copy(), ConstKeys.DIRECTION pairTo exit.side))
        blocks[index] = block
    }

    private fun removeBlock(index: Int) {
        if (index >= blocks.size) return
        val block = blocks[index] ?: return
        if (block.existenceState.liveAfterUpdate) block.destroy()
        blocks[index] = null
    }

    private fun removeBlocks() {
        for (i in 0 until blocks.size) removeBlock(i)
    }
}
