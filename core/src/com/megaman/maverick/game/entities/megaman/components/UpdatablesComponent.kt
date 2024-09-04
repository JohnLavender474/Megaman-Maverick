package com.megaman.maverick.game.entities.megaman.components

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

const val MEGAMAN_UPDATE_COMPONENT_TAG = "MegamanUpdateComponentTag"

private const val UNDER_WATER_BUBBLE_DELAY = 2f

private val underWaterBubbleTimer = Timer(UNDER_WATER_BUBBLE_DELAY)

internal fun Megaman.defineUpdatablesComponent() = UpdatablesComponent({ delta ->
    if (body.x < -10 * ConstVals.PPM || body.y < -10 * ConstVals.PPM) {
        GameLogger.error(MEGAMAN_UPDATE_COMPONENT_TAG, "Megaman is below game bounds, killing him")
        destroy()
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
    roomTransPauseTimer.update(delta)

    if (body.isSensing(BodySense.IN_WATER)) {
        underWaterBubbleTimer.update(delta)
        if (underWaterBubbleTimer.isFinished()) {
            spawnBubbles()
            underWaterBubbleTimer.reset()
        }
    }
})

private fun Megaman.spawnBubbles() {
    val bubbles = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.UNDER_WATER_BUBBLE)!!
    val offsetY = if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) 0.05f else 0.1f
    val offsetX = 0.2f * facing.value
    val spawn = body.getCenter().add(offsetX * ConstVals.PPM, offsetY * ConstVals.PPM)
    bubbles.spawn(props(ConstKeys.POSITION to spawn))
}
