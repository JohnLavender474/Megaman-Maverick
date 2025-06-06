package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet

interface ILightSource  {

    val lightSourceKeys: ObjectSet<Int>
    val lightSourceCenter: Vector2
    var lightSourceRadius: Int
    var lightSourceRadiance: Float
}
