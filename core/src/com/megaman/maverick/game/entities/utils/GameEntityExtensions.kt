package com.megaman.maverick.game.entities.utils

import com.engine.entities.IGameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.getMegamanMaverickGame

fun IGameEntity.stopSoundNow(soundKey: Any) = getMegamanMaverickGame().audioMan.stopSound(soundKey)

fun IGameEntity.isLoggingLifecyle() =
    properties.getOrDefault("${ConstKeys.LOG}_${ConstKeys.LIFECYCLE}", false, Boolean::class)
