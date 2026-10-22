package com.megaman.maverick.game.spawns

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

data class Spawn(
    val entity: MegaGameEntity,
    val properties: Properties,
    var onRejected: (() -> Unit)? = null
)
