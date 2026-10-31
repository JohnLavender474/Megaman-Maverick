package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.levels.LevelState

fun IBodyEntity.isOutsideLevel(levelState: LevelState, offsetX: Float, offsetY: Float) =
    body.getX() < -offsetX * ConstVals.PPM || body.getY() < -offsetY * ConstVals.PPM ||
        body.getMaxX() > (levelState.tiledMapLoadResult!!.map.properties.get(ConstKeys.WIDTH) as Int + offsetX) * ConstVals.PPM ||
        body.getMaxY() > (levelState.tiledMapLoadResult!!.map.properties.get(ConstKeys.HEIGHT) as Int + offsetY) * ConstVals.PPM
