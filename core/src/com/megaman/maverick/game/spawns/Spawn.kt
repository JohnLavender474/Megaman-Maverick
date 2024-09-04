package com.megaman.maverick.game.spawns

import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

/**
 * A spawn is a combination of an entity and properties.
 *
 * @param entity the entity to spawn
 * @param properties the properties to apply to the entity
 */
data class Spawn(val entity: MegaGameEntity, val properties: Properties)
