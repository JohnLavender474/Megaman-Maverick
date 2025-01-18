package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.common.interfaces.ITaggable
import com.mega.game.engine.common.interfaces.ITypable
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.world.body.getBounds

interface IMegaGameEntity : ITaggable, ITypable<EntityType> {

    val game: MegamanMaverickGame

    fun getGameCamera() = game.getGameCamera()

    fun playSoundNow(soundKey: Any, loop: Boolean) = game.audioMan.playSound(soundKey, loop)

    fun stopSoundNow(soundKey: Any) = game.audioMan.stopSound(soundKey)
}

val IMegaGameEntity.megaman
    get() = game.megaman

fun IBodyEntity.overlapsGameCamera() =
    (this as MegaGameEntity).game.getGameCamera().getRotatedBounds().overlaps(body.getBounds())
