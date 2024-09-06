package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.mega.game.engine.common.extensions.overlaps
import com.mega.game.engine.common.interfaces.ITaggable
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType

interface IMegaGameEntity : ITaggable {

    val game: MegamanMaverickGame

    fun getEntityType(): EntityType

    fun getGameCamera() = game.getGameCamera()

    fun getMegaman() = game.megaman

    fun playSoundNow(soundKey: Any, loop: Boolean) = game.audioMan.playSound(soundKey, loop)

    fun stopSoundNow(soundKey: Any) = game.audioMan.stopSound(soundKey)
}

fun IBodyEntity.overlapsGameCamera() = (this as MegaGameEntity).game.getGameCamera().overlaps(body)