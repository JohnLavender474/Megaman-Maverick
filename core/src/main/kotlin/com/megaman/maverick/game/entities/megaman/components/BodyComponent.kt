package com.megaman.maverick.game.entities.megaman.components

import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.movement.maxRunSpeed
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

const val MEGAMAN_BODY_WIDTH = 1f
const val MEGAMAN_BODY_HEIGHT = 1.5f

// slightly less than 1 so that Megaman can slide under spaces that are 1 tile in height
const val GROUNDSLIDE_CROUCH_HEIGHT = 0.9f

fun Megaman.getMaxRunSpeed(): Float {
    var threshold = maxRunSpeed(body, MegamanValues.RUN_MAX_SPEED, MegamanValues.WATER_RUN_MAX_SPEED_SCALAR)
    if (!body.isSensing(BodySense.IN_WATER) && services.levelState.currentLevel!! == LevelDefinition.MOON_MAN)
        threshold *= MegamanValues.MOON_RUN_MAX_SPEED_SCALAR
    return threshold
}
