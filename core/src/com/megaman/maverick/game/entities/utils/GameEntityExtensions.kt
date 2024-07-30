package com.megaman.maverick.game.entities.utils

import com.engine.common.extensions.overlaps
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.getMegamanMaverickGame

fun IGameEntity.getGameCamera() = getMegamanMaverickGame().getGameCamera()

fun IGameEntity.getMegaman() = getMegamanMaverickGame().megaman

fun IGameEntity.playSoundNow(soundKey: Any, loop: Boolean) = getMegamanMaverickGame().audioMan.playSound(soundKey, loop)

fun IGameEntity.stopSoundNow(soundKey: Any) = getMegamanMaverickGame().audioMan.stopSound(soundKey)

fun IGameEntity.isLoggingLifecyle() =
    properties.getOrDefault("${ConstKeys.LOG}_${ConstKeys.LIFECYCLE}", false, Boolean::class)

fun IBodyEntity.overlapsGameCamera() = getMegamanMaverickGame().getGameCamera().overlaps(body)