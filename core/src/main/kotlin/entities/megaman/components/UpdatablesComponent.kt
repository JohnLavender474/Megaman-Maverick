package com.megaman.maverick.game.entities.megaman.components

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.pairTo
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
import com.megaman.maverick.game.entities.megaman.sprites.MegamanTrailSprite
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

const val MEGAMAN_UPDATE_COMPONENT_TAG = "MegamanUpdateComponentTag"

private const val UNDER_WATER_BUBBLE_DELAY = 2f
private const val DEATH_X_OFFSET = 1.5f
private const val DEATH_Y_OFFSET = 1.5f
private const val TRAIL_SPRITE_DELAY = 0.1f

private val trailSpriteTimer = Timer(TRAIL_SPRITE_DELAY)
private val underWaterBubbleTimer = Timer(UNDER_WATER_BUBBLE_DELAY)

internal fun Megaman.defineUpdatablesComponent() = UpdatablesComponent({ delta ->
    if (body.getX() < -DEATH_X_OFFSET * ConstVals.PPM || body.getY() < -DEATH_Y_OFFSET * ConstVals.PPM ||
        body.getMaxX() > (game.getTiledMapLoadResult().map.properties.get("width") as Int + DEATH_X_OFFSET) * ConstVals.PPM ||
        body.getMaxY() > (game.getTiledMapLoadResult().map.properties.get("height") as Int + DEATH_Y_OFFSET) * ConstVals.PPM
    ) {
        GameLogger.error(MEGAMAN_UPDATE_COMPONENT_TAG, "Megaman is below game bounds, killing him")
        destroy()
    }

    if (!weaponHandler.isChargeable(currentWeapon)) stopCharging()
    weaponHandler.update(delta)

    if (body.isSensing(BodySense.FEET_ON_GROUND)) stunTimer.update(delta)

    damageTimer.update(delta)
    if (damageTimer.isJustFinished()) damageRecoveryTimer.reset()

    if (stunned || damaged) chargingTimer.reset()

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

    trailSpriteTimer.update(delta)
    if (trailSpriteTimer.isFinished()) {
        val spawnTrailSprite = when {
            isBehaviorActive(BehaviorType.GROUND_SLIDING) -> {
                val type = if (shooting) MegamanTrailSprite.GROUND_SLIDE_SHOOT else MegamanTrailSprite.GROUND_SLIDE
                spawnTrailSprite(type)
            }

            isBehaviorActive(BehaviorType.AIR_DASHING) -> spawnTrailSprite(MegamanTrailSprite.AIR_DASH)
            else -> false
        }
        if (spawnTrailSprite) trailSpriteTimer.reset()
    }

    if (ready) spawningTimer.update(delta)
})

private fun Megaman.spawnTrailSprite(type: String): Boolean {
    /*
    val trailSprite = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.MEGAMAN_TRAIL_SPRITE)!!
    return trailSprite.spawn(props(ConstKeys.TYPE pairTo type))
     */
    val trailSprite = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.MEGAMAN_TRAIL_SPRITE_V2)!!
    return trailSprite.spawn(props())
}

private fun Megaman.spawnBubbles() {
    val bubbles = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.UNDER_WATER_BUBBLE)!!
    val offsetY = if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) 0.05f else 0.1f
    val offsetX = 0.2f * facing.value
    val spawn = body.getCenter().add(offsetX * ConstVals.PPM, offsetY * ConstVals.PPM)
    bubbles.spawn(props(ConstKeys.POSITION pairTo spawn))
}
