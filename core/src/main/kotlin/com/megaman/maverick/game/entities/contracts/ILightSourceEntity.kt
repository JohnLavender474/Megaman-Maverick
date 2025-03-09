package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.entities.IGameEntity

interface ILightSourceEntity : IGameEntity {

    val keys: ObjectSet<Int>
    val center: Vector2

    var radius: Int
    var radiance: Float
}
