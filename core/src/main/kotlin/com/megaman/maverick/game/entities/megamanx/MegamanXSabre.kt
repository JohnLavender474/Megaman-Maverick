package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.players.bodyOverGround

enum class SabreKind(
    val bodyRegion: String,
    val arcRegion: String,
    val stripFrames: Int,
    val firstFrame: Int,
    val frameTimes: FloatArray,
    val arcWidth: Int,
    val arcHeight: Int,
    val arcCenterAboveBody: Int,
    val stripFacesLeft: Boolean,
    artOnFrame: Int,
    swingEndFrame: Int,
    artOffFrame: Int
) {
    FLOOR("sabre_floor", "sabre_floor_arc", 9, 1, MegamanXValues.SABRE_FLOOR_FRAME_TIMES, 112, 64, 2, false, 1, 4, 7),
    AIR("sabre_air", "sabre_air_arc", 9, 0, MegamanXValues.SABRE_AIR_FRAME_TIMES, 104, 54, -3, false, 2, 5, 6),
    WALL("sabre_wall", "sabre_wall_arc", 8, 0, MegamanXValues.SABRE_WALL_FRAME_TIMES, 104, 54, -3, true, 1, 3, 6);

    val duration = frameTimes.sum()

    private val phaseEnds = floatArrayOf(
        0f, startOf(artOnFrame), startOf(swingEndFrame + 1), startOf(artOffFrame + 1), duration
    )

    private fun startOf(frame: Int) = frameTimes.take(frame - firstFrame).sum()

    fun timeIn(next: SabreKind, time: Float): Float {
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

class MegamanXSabre(private val x: MegamanX) {

    var kind: SabreKind? = null
        private set
    var time = 0f
        private set
    var pressHeld = false
    internal var slash: MegamanXSabreSlash? = null
        private set

    val active: Boolean
        get() = kind != null

    val frame: Int
        get() = kind!!.frameAt(time)

    fun canStart() = !active && x.canAttack() &&
        !x.isAnyBehaviorActive(BehaviorType.AIR_DASHING, BehaviorType.CLIMBING) &&
        !x.kickLocked && !(x.isBehaviorActive(BehaviorType.WALL_SLIDING) && !x.wallGrabTimer.isFinished())

    private fun kindNow() = when {
        x.isBehaviorActive(BehaviorType.WALL_SLIDING) -> SabreKind.WALL
        x.bodyOverGround -> SabreKind.FLOOR
        else -> SabreKind.AIR
    }

    private fun stopOnFloor(kind: SabreKind) {
        if (kind != SabreKind.FLOOR) return
        x.running = false
        x.setSideSpeed(0f)
    }

    fun start() {
        val kind = kindNow()
        this.kind = kind
        time = 0f
        pressHeld = true
        x.stopCharging()
        stopOnFloor(kind)
        val slash = MegaEntityFactory.fetch(MegamanXSabreSlash::class)!!
        slash.spawn(props(ConstKeys.OWNER pairTo x, ConstKeys.TYPE pairTo kind))
        this.slash = slash
        x.requestToPlaySound(SoundAsset.WHIP_V2_SOUND, false)
    }

    fun update(delta: Float) {
        var kind = kind ?: return
        if (!x.canAttack()) {
            end()
            return
        }
        val next = kindNow()
        if (next != kind) {
            time = kind.timeIn(next, time)
            kind = next
            this.kind = next
            slash?.kind = next
            stopOnFloor(next)
        }
        time += delta
        if (time >= kind.duration) end()
    }

    fun end() {
        kind = null
        time = 0f
        slash?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        slash = null
    }

    fun reset() {
        end()
        pressHeld = false
    }
}
