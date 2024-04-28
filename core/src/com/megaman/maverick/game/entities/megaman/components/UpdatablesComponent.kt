package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.GameLogger
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging

const val MEGAMAN_UPDATE_COMPONENT_TAG = "MegamanUpdateComponentTag"

internal fun Megaman.defineUpdatablesComponent() =
    UpdatablesComponent(
        this,
        { delta ->
            if (body.x < -10 * ConstVals.PPM || body.y < -10 * ConstVals.PPM) {
                GameLogger.error(MEGAMAN_UPDATE_COMPONENT_TAG, "Megaman is below game bounds, killing him")
                kill()
            }

            if (!weaponHandler.isChargeable(currentWeapon)) stopCharging()
            weaponHandler.update(delta)

            stunTimer.update(delta)
            damageTimer.update(delta)
            if (stunned || damaged) chargingTimer.reset()
            if (damageTimer.isJustFinished()) damageRecoveryTimer.reset()

            if (damageTimer.isFinished() && !damageRecoveryTimer.isFinished()) {
                damageRecoveryTimer.update(delta)
                damageFlashTimer.update(delta)
                if (damageFlashTimer.isFinished()) {
                    damageFlashTimer.reset()
                    damageFlash = !damageFlash
                }
            }
            if (damageRecoveryTimer.isJustFinished()) damageFlash = false

            shootAnimTimer.update(delta)
            wallJumpTimer.update(delta)

            GameLogger.debug(
                MEGAMAN_UPDATE_COMPONENT_TAG,
                buildString {
                    append("Body: $body\n")
                    append("Fixtures:\n")
                    body.fixtures.forEach { append("  $it\n") }
                })
        })
