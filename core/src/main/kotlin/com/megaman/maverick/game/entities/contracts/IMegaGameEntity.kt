package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.common.interfaces.ITaggable
import com.maverick.game.engine.common.interfaces.ITypable
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.world.body.getBounds

interface IMegaGameEntity : IGameEntity, ITaggable, ITypable<EntityType> {

    val game: MegamanMaverickGame

    fun getGameCamera() = game.getGameCamera()

    fun playSoundNow(soundKey: Any, loop: Boolean) = game.audioMan.playSound(soundKey, loop)

    fun stopSoundNow(soundKey: Any) = game.audioMan.stopSound(soundKey)
}

val IMegaGameEntity.players
    get() = game.players

// TODO: every entity gets this extension, but only entities with a target need it, and each one targets in its own
//  way; it moves to the entities that need it (see the TODO at MegaPlayers.targetFor; item 047)
val IMegaGameEntity.targetPlayer: MegaGameEntity
    get() = game.players.targetFor(this)

fun IBodyEntity.overlapsGameCamera() =
    (this as MegaGameEntity).game.getGameCamera().getRotatedBounds().overlaps(body.getBounds())
