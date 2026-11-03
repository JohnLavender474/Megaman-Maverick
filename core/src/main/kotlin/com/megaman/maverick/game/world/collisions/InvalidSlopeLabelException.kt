package com.megaman.maverick.game.world.collisions

import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.blocks.GroundSlope
import com.megaman.maverick.game.entities.state.mapObjectId

class InvalidSlopeLabelException(val entity: IGameEntity, cause: Throwable? = null) : RuntimeException(
    "Body has BodyLabel.SLOPE, but its entity is not a ${GroundSlope.TAG}: " +
        "class=${entity::class.simpleName}, mapObjectId=${entity.mapObjectId}. " +
        "Only ${GroundSlope.TAG} may carry the SLOPE label; in Tiled, name the object ${GroundSlope.TAG} " +
        "and do not set body_labels=slope on another block.",
    cause
)
