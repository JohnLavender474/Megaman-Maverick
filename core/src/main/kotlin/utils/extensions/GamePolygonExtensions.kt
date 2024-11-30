package com.megaman.maverick.game.utils.extensions

import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.ObjectPools

fun GamePolygon.getBoundingRectangle() = getBoundingRectangle(ObjectPools.get(GameRectangle::class))
