package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.common.interfaces.ITaggable
import com.maverick.game.engine.common.interfaces.ITypable
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.world.body.getBounds

interface IMegaGameEntity : IGameEntity, ITaggable, ITypable<EntityType>

val MegaGameEntity.players
    get() = game.players

// TODO: every entity gets this extension, but only entities with a target need it, and each one targets in its own
//  way; it moves to the entities that need it (see the TODO at MegaPlayers.targetFor; item 047)
val MegaGameEntity.targetPlayer: MegaGameEntity
    get() = game.players.targetFor(this)

fun <T> T.overlapsGameCamera() where T : MegaGameEntity, T : IBodyEntity =
    game.getGameCamera().getRotatedBounds().overlaps(body.getBounds())
