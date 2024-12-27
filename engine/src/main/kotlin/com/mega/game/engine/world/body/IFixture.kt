package com.mega.game.engine.world.body

import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.interfaces.ITypable
import com.mega.game.engine.common.shapes.IGameShape2D

interface IFixture : ITypable<Any>, IPropertizable {

    fun getShape(): IGameShape2D

    fun setShape(shape: IGameShape2D)

    fun setActive(active: Boolean)

    fun isActive(): Boolean
}
