package com.megaman.maverick.game.entities.megaman.handlers

import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon

class MegamanJetpackHandler(private val megaman: Megaman) : Updatable, Resettable {

    val timePerBitTimer = Timer(MegamanValues.JETPACK_TIME_PER_BIT)
    val recoveryTimer = Timer(MegamanValues.JETPACK_BIT_RECOVERY_DUR)
    val delayBeforeRecovery = Timer(MegamanValues.JETPACK_BIT_RECOVERY_DELAY)
    val rushJetSoundTimer = Timer(SoundAsset.JETPACK_SOUND.seconds).setToEnd()

    private fun isActive(): Boolean {
        if (megaman.dead) return false

        if (megaman.isBehaviorActive(BehaviorType.JETPACKING)) return true

        if (megaman.isBehaviorActive(BehaviorType.AIR_DASHING) &&
            megaman.currentWeapon == MegamanWeapon.RUSH_JET
        ) return true

        return false
    }

    override fun update(delta: Float) {
        if (isActive()) {
            timePerBitTimer.update(delta)
            if (timePerBitTimer.isFinished()) {
                megaman.weaponsHandler.translateAmmo(MegamanWeapon.RUSH_JET, -1)
                timePerBitTimer.reset()
            }

            rushJetSoundTimer.update(delta)
            if (rushJetSoundTimer.isFinished()) {
                megaman.requestToPlaySound(SoundAsset.JETPACK_SOUND, false)
                rushJetSoundTimer.reset()
            }
        } else {
            delayBeforeRecovery.update(delta)

            if (delayBeforeRecovery.isFinished()) {
                recoveryTimer.update(delta)

                if (recoveryTimer.isFinished() &&
                    !megaman.weaponsHandler.isAtMaxAmmo(MegamanWeapon.RUSH_JET)
                ) {
                    megaman.translateAmmo(1, MegamanWeapon.RUSH_JET)
                    recoveryTimer.reset()
                }
            }
        }
    }

    override fun reset() {
        recoveryTimer.reset()
        timePerBitTimer.reset()
        delayBeforeRecovery.reset()
        rushJetSoundTimer.setToEnd()
    }
}
