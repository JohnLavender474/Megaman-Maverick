package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.entities.headFixture
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getContactWater
import com.megaman.maverick.game.world.body.isSensing

fun IBodyEntity.isHeadUnderWater() =
    body.isSensing(BodySense.IN_WATER) &&
        !body.isSensing(BodySense.FORCE_APPLIED) &&
        body.getContactWater().any { water ->
            water.body.getBounds().contains(
                this.headFixture.getShape().getCenter()
            )
        }
