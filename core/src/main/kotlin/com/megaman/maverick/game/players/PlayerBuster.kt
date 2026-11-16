package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class BusterShot(
    val speed: Float,
    val wait: Float,
    val poseTime: Float,
    val limit: Int?,
    val sound: SoundAsset,
    val fetch: (Properties) -> MegaGameEntity
)

class BusterOffset(val x: Float, val y: Float)

class BusterParams(
    val notCharged: BusterShot,
    val halfCharged: BusterShot?,
    val fullyCharged: BusterShot?,
    val offsetX: Float,
    val offsetY: Float,
    val runOffsetX: Float,
    val airOffsetY: Float,
    val crouchOffset: BusterOffset?,
    val climbOffset: BusterOffset?,
    val slideOffset: BusterOffset?
)

class BusterCrouchOffsetMissingException :
    IllegalStateException("A crouched shot needs a crouch offset. Set crouchOffset in the BusterParams of the player.")

class BusterClimbOffsetMissingException :
    IllegalStateException("A shot on a ladder needs a climb offset. Set climbOffset in the BusterParams of the player.")

class BusterSlideOffsetMissingException :
    IllegalStateException("A shot in a slide needs a slide offset. Set slideOffset in the BusterParams of the player.")

// TODO: widen the player type when an enemy clone calls this (item 121)
class PlayerBuster(private val player: MegaPlayer, private val params: BusterParams) {

    private val shots = ObjectSet<MegaGameEntity>()
    private val waitTimer = Timer(params.notCharged.wait).setToEnd()
    private val poseTimer = Timer(params.notCharged.poseTime).setToEnd()

    val shooting: Boolean
        get() = !poseTimer.isFinished()

    fun update(delta: Float) {
        waitTimer.update(delta)
        poseTimer.update(delta)
    }

    fun endPose() {
        poseTimer.setToEnd()
    }

    fun reset() {
        shots.clear()
        waitTimer.setToEnd()
        poseTimer.setToEnd()
    }

    fun liveShots(): Int {
        val iter = shots.iterator()
        while (iter.hasNext()) {
            val shot = iter.next()
            if (!shot.existenceState.liveAfterUpdate || (shot as IOwnable<*>).owner != player) iter.remove()
        }
        return shots.size
    }

    private fun shotOf(status: MegaChargeStatus): BusterShot = when (status) {
        MegaChargeStatus.NOT_CHARGED -> params.notCharged
        MegaChargeStatus.HALF_CHARGED -> params.halfCharged ?: params.notCharged
        MegaChargeStatus.FULLY_CHARGED -> params.fullyCharged ?: params.halfCharged ?: params.notCharged
    }

    fun canShoot(status: MegaChargeStatus): Boolean {
        val limit = shotOf(status).limit
        return waitTimer.isFinished() && (limit == null || liveShots() < limit)
    }

    fun shoot(
        status: MegaChargeStatus,
        crouched: Boolean = false,
        climbing: Boolean = false,
        sliding: Boolean = false
    ): Boolean {
        val poseOffset = when {
            crouched -> params.crouchOffset ?: throw BusterCrouchOffsetMissingException()
            climbing -> params.climbOffset ?: throw BusterClimbOffsetMissingException()
            sliding -> params.slideOffset ?: throw BusterSlideOffsetMissingException()
            else -> null
        }
        if (!canShoot(status)) return false
        val shot = shotOf(status)

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(shot.speed * ConstVals.PPM * player.facing.value, 0f)
            .rotateDeg(player.direction.rotation)

        val position = GameObjectPools.fetch(Vector2::class)
            .also {
                if (poseOffset != null) {
                    it.set(poseOffset.x * player.facing.value, poseOffset.y)
                    return@also
                }
                it.set(params.offsetX * player.facing.value, params.offsetY)
                if (player.running) it.x += player.facing.value * params.runOffsetX
                if (!player.body.isSensing(BodySense.FEET_ON_GROUND)) it.y += params.airOffsetY
            }
            .scl(ConstVals.PPM.toFloat())
            .rotateDeg(player.direction.rotation)
            .add(player.body.getCenter())

        val props = props(
            ConstKeys.OWNER pairTo player,
            ConstKeys.POSITION pairTo position,
            ConstKeys.DIRECTION pairTo player.direction,
            ConstKeys.TRAJECTORY pairTo trajectory
        )

        player.requestToPlaySound(shot.sound, loop = false)
        val entity = shot.fetch(props)
        entity.spawn(props)
        shots.add(entity)

        waitTimer.resetDuration(shot.wait)
        poseTimer.resetDuration(shot.poseTime)

        return true
    }
}
