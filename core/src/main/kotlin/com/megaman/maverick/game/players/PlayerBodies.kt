package com.megaman.maverick.game.players

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys

val IBodyEntity.bodyOverGround: Boolean
    get() = (body.getProperty(ConstKeys.BODY_OVER_GROUND) as () -> Boolean).invoke()
