package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.IDrawOffsetSource
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.hasBody
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.players.forEach
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.focus
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.state.ViewEdgeRule
import com.megaman.maverick.game.state.ViewEdgeRules
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import com.megaman.maverick.game.entities.roomSubject
import com.maverick.game.engine.camera.IRoomSubject

class PlayersFocus(
    private val players: MegaPlayers,
    private val view: CameraView? = null,
    private val edges: ViewEdgeRules = ViewEdgeRules(),
    private val keepActive: () -> Boolean = { true },
    private val excluded: (IPlayer) -> Boolean = { false }
) : IFocusable, IRoomSubject, IDrawOffsetSource, ICameraPart, Resettable {

    companion object {
        const val EASE_DURATION = 0.5f
        const val JUMP_TO_EASE = 1f
        const val KEEP_MARGIN = 0.25f
    }

    private var keeperX: IPlayer? = null
    private var keeperY: IPlayer? = null

    private val position = Vector2()
    private val shown = Vector2()
    private val easeFrom = Vector2()
    private val lastTarget = Vector2()
    private var groupMask = -1
    private var easeTime = EASE_DURATION
    private var keeperMoved = false
    private val playerDrawOffset = Vector2()
    private val lastBounds = GameRectangle()
    private var hasGroup = false

    private val lead: IPlayer
        get() = players.get(PlayerSlot(0))!!

    fun inGroup(player: IPlayer) =
        player.hasBody && player.existenceState.liveNow && !players.isBot(player.slot) && !excluded(player)

    override fun getSubjectBounds(): GameRectangle {
        players.forEach { if (inGroup(it)) return lastBounds.set(it.roomSubject.getSubjectBounds()) }
        return if (hasGroup) lastBounds else lead.roomSubject.getSubjectBounds()
    }

    override fun getFocusPosition(): Vector2 {
        var count = 0
        var mask = 0
        position.setZero()
        players.forEach {
            if (inGroup(it)) {
                position.add(it.focus.getFocusPosition())
                count++
                mask = mask or (1 shl it.slot.index)
            }
        }
        when {
            count > 0 -> position.scl(1f / count)
            hasGroup -> position.set(lastTarget)
            else -> position.set(lead.focus.getFocusPosition())
        }
        if (count > 0) hasGroup = true

        val jumped = position.dst(lastTarget) > JUMP_TO_EASE * ConstVals.PPM
        if (groupMask != -1 && (mask != groupMask || jumped)) {
            easeFrom.set(shown)
            easeTime = 0f
        }
        groupMask = mask
        lastTarget.set(position)

        if (easeTime < EASE_DURATION) {
            val alpha = Interpolation.smooth.apply(easeTime / EASE_DURATION)
            position.set(easeFrom.x + (position.x - easeFrom.x) * alpha, easeFrom.y + (position.y - easeFrom.y) * alpha)
        }

        keeperMoved = false
        val view = view
        if (view != null && count > 1 && keepActive()) {
            keeperX = keeper(keeperX, true, edges.left.leaves(), edges.right.leaves())
            keeperY = keeper(keeperY, false, edges.bottom.leaves(), edges.top.leaves())
            val keptX = position.x
            val keptY = position.y
            keeperX?.let { position.x = keepInView(position.x, it, true, view.viewWidth) }
            keeperY?.let { position.y = keepInView(position.y, it, false, view.viewHeight) }
            keeperMoved = position.x != keptX || position.y != keptY
        }

        shown.set(position)
        return position
    }

    override fun getDrawOffset(offsets: IDrawOffsets, out: Vector2): Vector2 {
        out.setZero()
        if (easeTime < EASE_DURATION || keeperMoved) return out

        var count = 0
        players.forEach {
            if (inGroup(it)) {
                out.add(offsets.getDrawOffset(it.body, playerDrawOffset))
                count++
            }
        }
        return if (count == 0) offsets.getDrawOffset(lead.body, out) else out.scl(1f / count)
    }

    private fun ViewEdgeRule.leaves() = this == ViewEdgeRule.WARP || this == ViewEdgeRule.KILL

    private fun coordinate(player: IPlayer, horizontal: Boolean): Float {
        val center = player.body.getCenter()
        return if (horizontal) center.x else center.y
    }

    private fun keeper(last: IPlayer?, horizontal: Boolean, lowLeaves: Boolean, highLeaves: Boolean):
        IPlayer? {
        if (!lowLeaves && !highLeaves) return null

        var chosen: IPlayer? = null
        var best = 0f
        players.forEach {
            if (!inGroup(it)) return@forEach
            val value = coordinate(it, horizontal)
            val score = when {
                lowLeaves && highLeaves -> {
                    if (it == last) return it
                    -abs(value - if (horizontal) shown.x else shown.y)
                }

                lowLeaves -> value
                else -> -value
            }
            if (chosen == null || score > best) {
                chosen = it
                best = score
            }
        }
        return chosen
    }

    private fun keepInView(center: Float, keeper: IPlayer, horizontal: Boolean, viewSize: Float): Float {
        val bounds = keeper.body.getBounds()
        val margin = KEEP_MARGIN * ConstVals.PPM
        val half = viewSize / 2f
        val low = (if (horizontal) bounds.getMaxX() else bounds.getMaxY()) - half + margin
        val high = (if (horizontal) bounds.getX() else bounds.getY()) + half - margin
        return if (low <= high) center.coerceIn(low, high) else center
    }

    override fun update(camera: Camera, delta: Float): Boolean {
        if (easeTime < EASE_DURATION) easeTime = (easeTime + delta).coerceAtMost(EASE_DURATION)
        return false
    }

    override fun reset() {
        keeperX = null
        keeperY = null
        groupMask = -1
        easeTime = EASE_DURATION
        hasGroup = false
    }
}
