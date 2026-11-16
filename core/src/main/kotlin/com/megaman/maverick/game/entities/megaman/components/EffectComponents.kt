package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.common.enums.Speed
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.UnderWaterBubble
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.weapons.TrailWhileFiring
import com.megaman.maverick.game.entities.megaman.sprites.MegamanTrailSpriteV2
import com.megaman.maverick.game.entities.state.BubbleEmitterComponent
import com.megaman.maverick.game.entities.state.TrailSpriteComponent
import com.megaman.maverick.game.entities.utils.isHeadUnderWater
import com.megaman.maverick.game.world.body.getCenter

internal fun Megaman.defineBubbleEmitterComponent() = BubbleEmitterComponent(
    MegamanValues.UNDER_WATER_BUBBLE_DELAY,
    canEmit = { isHeadUnderWater() },
    emit = { spawnBubble() }
)

internal fun Megaman.defineTrailSpriteComponent() = TrailSpriteComponent(
    MegamanValues.TRAIL_SPRITE_DELAY,
    trySpawn = { tryToSpawnTrailSprite() }
)

private fun Megaman.spawnBubble() {
    val offsetY = if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) 0.05f else 0.1f
    val offsetX = 0.2f * facing.value

    val spawn = body.getCenter().add(offsetX * ConstVals.PPM, offsetY * ConstVals.PPM)

    val bubble = MegaEntityFactory.fetch(UnderWaterBubble::class)!!
    bubble.spawn(
        props(
            ConstKeys.DIRECTION pairTo direction,
            ConstKeys.SPEED pairTo Speed.SLOW,
            ConstKeys.POSITION pairTo spawn
        )
    )
}

private fun Megaman.tryToSpawnTrailSprite(): Boolean {
    val animKey: String

    when {
        isBehaviorActive(BehaviorType.GROUND_SLIDING) -> {
            var key = "groundslide"
            if (shooting) {
                when (currentWeapon.trailWhileFiring) {
                    TrailWhileFiring.NONE -> return false
                    TrailWhileFiring.SHOOT_KEY -> key += "_shoot"
                    TrailWhileFiring.PLAIN_KEY -> {}
                }
            }
            animKey = key
        }

        isBehaviorActive(BehaviorType.AIR_DASHING) -> {
            var key = "airdash"
            if (shooting) {
                when (currentWeapon.trailWhileFiring) {
                    TrailWhileFiring.NONE -> return false
                    TrailWhileFiring.SHOOT_KEY -> key += "_shoot"
                    TrailWhileFiring.PLAIN_KEY -> {}
                }
            }
            animKey = key
        }

        else -> return false
    }

    val trailSprite = MegaEntityFactory.fetch(MegamanTrailSpriteV2::class)!!
    return trailSprite.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.KEY pairTo animKey))
}
