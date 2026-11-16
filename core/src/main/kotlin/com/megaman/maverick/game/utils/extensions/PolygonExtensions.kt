package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Polygon
import com.maverick.game.engine.common.shapes.GamePolygon
import com.megaman.maverick.game.utils.GameObjectPools

fun Polygon.toGamePolygon(): GamePolygon = GameObjectPools.fetch(GamePolygon::class).set(this)
