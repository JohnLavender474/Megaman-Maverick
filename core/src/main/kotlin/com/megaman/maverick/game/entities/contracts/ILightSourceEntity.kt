package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.entities.IGameEntity

interface ILightSourceEntity : IGameEntity {

    val lightSourceKeys: ObjectSet<Int>
    val lightSourceCenter: Vector2
    var lightSourceRadius: Int
    var lightSourceRadiance: Float
}
