package com.maverick.game.engine.components

import com.maverick.game.engine.common.interfaces.Resettable

interface IGameComponent : Resettable {

    override fun reset() {}
}
