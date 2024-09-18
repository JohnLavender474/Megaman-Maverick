package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Rectangle
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

fun IBodyEntity.overlapsGameCamera() = (this as MegaGameEntity).game.getGameCamera().getRotatedBounds().overlaps(
    body as Rectangle
)