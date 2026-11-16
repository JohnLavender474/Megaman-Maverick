package com.megaman.maverick.game.players

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.state.running

enum class SabreState { FLOOR, AIR, WALL }

class SabreMove(
    val bodyRegion: String,
    val arcRegion: String,
    val stripFrames: Int,
    val firstFrame: Int,
    val frameTimes: FloatArray,
    val arcWidth: Int,
    val arcHeight: Int,
    val arcCenterAboveBody: Int,
    val stripFacesLeft: Boolean,
    val hitBoxes: Array<IntArray?>,
    artOnFrame: Int,
    val swingEndFrame: Int,
    artOffFrame: Int,
    val fullStrength: Boolean
) {

    val duration = frameTimes.sum()

    private val phaseEnds = floatArrayOf(
        0f, startOf(artOnFrame), startOf(swingEndFrame + 1), startOf(artOffFrame + 1), duration
    )

    private fun startOf(frame: Int) = frameTimes.take(frame - firstFrame).sum()

    fun timeIn(next: SabreMove, time: Float): Float {
        val phase = (0 until PHASES).firstOrNull { time < phaseEnds[it + 1] } ?: (PHASES - 1)
        val start = phaseEnds[phase]
        val length = phaseEnds[phase + 1] - start
        val fraction = if (length > 0f) ((time - start) / length).coerceIn(0f, 1f) else 0f
        val nextStart = next.phaseEnds[phase]
        return nextStart + fraction * (next.phaseEnds[phase + 1] - nextStart)
    }

    fun frameAt(time: Float): Int {
        var end = 0f
        frameTimes.forEachIndexed { index, frameTime ->
            end += frameTime
            if (time < end) return firstFrame + index
        }
        return firstFrame + frameTimes.size - 1
    }

    private companion object {
        const val PHASES = 4
    }
}

class SabreParams(
    val moves: ObjectMap<SabreState, Array<SabreMove>>,
    val atlas: TextureAsset,
    val spriteSize: Float,
    val artPxPerBodyFrame: Int,
    val sound: SoundAsset,
    val canStart: () -> Boolean,
    val chooseState: () -> SabreState,
    val spriteOffsetY: () -> Float,
    val endOnStateChange: Boolean,
    val landingMoves: ObjectMap<SabreMove, SabreMove>
) {

    val artPx: Float
        get() = spriteSize * ConstVals.PPM / artPxPerBodyFrame
}

class SabreStateMissingException(state: SabreState) :
    IllegalStateException("The sabre has no moves for the state $state. Add a move list for $state to the SabreParams.")

// TODO: widen the player type when an enemy clone calls this (item 121)
class PlayerSabre(private val player: MegaPlayer, val params: SabreParams) {

    var state: SabreState? = null
        private set
    var move: SabreMove? = null
        private set
    var time = 0f
        private set
    var pressHeld = false
    var slash: PlayerSabreSlash? = null
        private set

    private var step = 0
    private var queued = false
    private var chainLocked = false

    val active: Boolean
        get() = move != null

    val frame: Int
        get() = move!!.frameAt(time)

    fun canStart() = !active && player.playerCanAttack && params.canStart()

    private fun movesOf(state: SabreState) = params.moves[state] ?: throw SabreStateMissingException(state)

    private fun stopOnFloor(state: SabreState) {
        if (state != SabreState.FLOOR) return
        player.running = false
        player.setSideSpeed(0f)
    }

    fun start() {
        begin(params.chooseState(), 0)
        pressHeld = true
        val slash = MegaEntityFactory.fetch(PlayerSabreSlash::class)!!
        slash.spawn(props(ConstKeys.OWNER pairTo player, ConstKeys.PARENT pairTo this))
        this.slash = slash
    }

    private fun begin(state: SabreState, step: Int) {
        this.state = state
        this.step = step
        move = movesOf(state)[step]
        time = 0f
        queued = false
        chainLocked = false
        stopOnFloor(state)
        player.requestToPlaySound(params.sound, loop = false)
    }

    fun chain(): Boolean {
        val state = state ?: return false
        if (chainLocked || step >= movesOf(state).size - 1) return false
        queued = true
        return true
    }

    fun update(delta: Float) {
        var move = move ?: return
        if (!player.playerCanAttack) {
            end()
            return
        }
        val next = params.chooseState()
        val landing = params.landingMoves[move]
        if (next == SabreState.FLOOR && state == SabreState.AIR && landing != null &&
            move.frameAt(time) <= move.swingEndFrame
        ) {
            move = landing
            this.move = landing
            state = next
            queued = false
            chainLocked = true
            stopOnFloor(next)
        } else if (next != state && params.endOnStateChange) {
            end()
            return
        } else if (next != state) {
            val nextMove = movesOf(next)[0]
            time = move.timeIn(nextMove, time)
            move = nextMove
            this.move = nextMove
            state = next
            step = 0
            queued = false
            stopOnFloor(next)
        }
        time += delta
        if (time < move.duration) return
        if (queued) begin(state!!, step + 1) else end()
    }

    fun end() {
        state = null
        move = null
        time = 0f
        step = 0
        queued = false
        chainLocked = false
        slash?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        slash = null
    }

    fun reset() {
        end()
        pressHeld = false
    }
}
