package com.megaman.maverick.game.utils.extensions

import com.maverick.game.engine.common.shapes.GamePolygon
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun GamePolygon.getBoundingRectangle() = getBoundingRectangle(GameObjectPools.fetch(GameRectangle::class))
