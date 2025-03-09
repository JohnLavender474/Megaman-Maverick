package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameLine
import com.megaman.maverick.game.utils.GameObjectPools

fun GameLine.getFirstLocalPoint() = getFirstLocalPoint(GameObjectPools.fetch(Vector2::class))

fun GameLine.getSecondLocalPoint() = getSecondLocalPoint(GameObjectPools.fetch(Vector2::class))

fun GameLine.getWorldPoints(): GamePair<Vector2, Vector2> {
    val vec1 = GameObjectPools.fetch(Vector2::class)
    val vec2 = GameObjectPools.fetch(Vector2::class)
    calculateWorldPoints(vec1, vec2)
    return vec1 pairTo vec2
}
