package com.megaman.maverick.game.entities

import com.engine.entities.IGameEntity
import com.megaman.maverick.game.utils.getMegamanMaverickGame

fun IGameEntity.stopSoundNow(soundKey: Any) = getMegamanMaverickGame().audioMan.stopSound(soundKey)
