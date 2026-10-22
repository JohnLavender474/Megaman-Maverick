package com.maverick.game.engine.behaviors

import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable

interface IBehavior : Initializable, Updatable, Resettable {

    fun isActive(): Boolean

    fun evaluate(delta: Float): Boolean

    fun act(delta: Float)

    fun end()
}