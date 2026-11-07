package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class MegamanXBuster(private val x: MegamanX) {

    private val shots = ObjectSet<MegaGameEntity>()
    private val waitTimer = Timer(MegamanXValues.SHOT_WAIT).setToEnd()
    private val poseTimer = Timer(MegamanXValues.SHOT_POSE_TIME).setToEnd()

    val shooting: Boolean
        get() = !poseTimer.isFinished()

    fun update(delta: Float) {
        waitTimer.update(delta)
        poseTimer.update(delta)
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
            if (!shot.existenceState.liveAfterUpdate || (shot as IOwnable<*>).owner != x) iter.remove()
        }
        return shots.size
    }

    fun canShoot(status: MegaChargeStatus): Boolean {
        val limit = when (status) {
            MegaChargeStatus.FULLY_CHARGED -> MegamanXValues.FULL_SHOT_LIMIT
            else -> MegamanXValues.SHOT_LIMIT
        }
        return waitTimer.isFinished() && liveShots() < limit
    }

    fun shoot(status: MegaChargeStatus): Boolean {
        if (!canShoot(status)) return false

        val speed = when (status) {
            MegaChargeStatus.NOT_CHARGED -> MegamanXValues.SHOT_SPEED
            MegaChargeStatus.HALF_CHARGED -> MegamanXValues.HALF_SHOT_SPEED
            MegaChargeStatus.FULLY_CHARGED -> MegamanXValues.FULL_SHOT_SPEED
        }
        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(speed * ConstVals.PPM * x.facing.value, 0f)
            .rotateDeg(x.direction.rotation)

        val position = GameObjectPools.fetch(Vector2::class)
            .set(
                MegamanXValues.SHOT_OFFSET_X * x.facing.value,
                MegamanXValues.SHOT_OFFSET_Y
            )
            .also {
                if (x.running) it.x += x.facing.value * 0.5f
                if (!x.body.isSensing(BodySense.FEET_ON_GROUND)) it.y += 0.1f
            }
            .scl(ConstVals.PPM.toFloat())
            .rotateDeg(x.direction.rotation)
            .add(x.body.getCenter())

        val props = props(
            ConstKeys.OWNER pairTo x,
            ConstKeys.POSITION pairTo position,
            ConstKeys.DIRECTION pairTo x.direction,
            ConstKeys.TRAJECTORY pairTo trajectory
        )

        val shot: MegaGameEntity = when (status) {
            MegaChargeStatus.NOT_CHARGED -> {
                x.requestToPlaySound(SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, false)
                MegaEntityFactory.fetch(Bullet::class)!!
            }
            else -> {
                x.requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND, false)
                props.put(ConstKeys.BOOLEAN, status == MegaChargeStatus.FULLY_CHARGED)
                MegaEntityFactory.fetch(ChargedShot::class)!!
            }
        }
        shot.spawn(props)
        shots.add(shot)

        waitTimer.resetDuration(
            if (status == MegaChargeStatus.NOT_CHARGED) MegamanXValues.SHOT_WAIT else MegamanXValues.CHARGED_SHOT_WAIT
        )
        poseTimer.resetDuration(
            when (status) {
                MegaChargeStatus.NOT_CHARGED -> MegamanXValues.SHOT_POSE_TIME
                MegaChargeStatus.HALF_CHARGED -> MegamanXValues.HALF_SHOT_POSE_TIME
                MegaChargeStatus.FULLY_CHARGED -> MegamanXValues.FULL_SHOT_POSE_TIME
            }
        )

        return true
    }
}
