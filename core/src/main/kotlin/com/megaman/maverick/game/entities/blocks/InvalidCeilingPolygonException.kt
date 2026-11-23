package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys

class InvalidCeilingPolygonException(spawnProps: Properties, reason: String) :
    RuntimeException(
        "${CeilingPolygonBlock.TAG} (map object id=${spawnProps.get(ConstKeys.ID)}): $reason. " +
            "In Tiled, draw it as an unrotated convex polygon, and with each edge flat or at " +
            "exactly 45 degrees."
    )

class CeilingPolygonGravityException(spawnProps: Properties, gravity: Direction) :
    RuntimeException(
        "${CeilingPolygonBlock.TAG} (map object id=${spawnProps.get(ConstKeys.ID)}): spawned with gravity $gravity. " +
            "It supports only gravity UP; do not use it in a room or level that changes the gravity."
    )
