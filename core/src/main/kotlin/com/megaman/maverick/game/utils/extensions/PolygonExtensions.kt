package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Polygon
import com.mega.game.engine.common.shapes.GamePolygon
import com.megaman.maverick.game.utils.GameObjectPools

fun Polygon.toGamePolygon(reclaim: Boolean = true): GamePolygon {
    val polygon = GameObjectPools.fetch(GamePolygon::class, reclaim)
    return polygon.set(this)
}
