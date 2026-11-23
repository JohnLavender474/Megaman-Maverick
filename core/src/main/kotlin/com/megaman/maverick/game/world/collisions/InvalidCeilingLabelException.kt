package com.megaman.maverick.game.world.collisions

import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.blocks.CeilingPolygonBlock
import com.megaman.maverick.game.entities.state.mapObjectId

class InvalidCeilingLabelException(val entity: IGameEntity, cause: Throwable? = null) : RuntimeException(
    "Body has BodyLabel.CEILING, but its entity is not a ${CeilingPolygonBlock.TAG}: " +
        "class=${entity::class.simpleName}, mapObjectId=${entity.mapObjectId}. " +
        "Only ${CeilingPolygonBlock.TAG} may carry the CEILING label; in Tiled, draw a polygon named " +
        "${CeilingPolygonBlock.TAG} and do not set body_labels=ceiling on another block.",
    cause
)
