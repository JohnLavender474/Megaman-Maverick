package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.interfaces.Resettable

interface IThawRule : Resettable {

    fun shouldThaw(delta: Float): Boolean
}
