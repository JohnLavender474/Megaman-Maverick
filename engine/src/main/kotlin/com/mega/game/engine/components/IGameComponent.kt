package com.mega.game.engine.components

import com.mega.game.engine.common.interfaces.Resettable

interface IGameComponent : Resettable {

    override fun reset() {}
}
