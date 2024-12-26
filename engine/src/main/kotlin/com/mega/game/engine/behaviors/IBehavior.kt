package com.mega.game.engine.behaviors

import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable

interface IBehavior : Initializable, Updatable, Resettable {

    fun isActive(): Boolean

    fun evaluate(delta: Float): Boolean

    fun act(delta: Float)

    fun end()
}