package com.maverick.game.engine.cullables

import com.maverick.game.engine.common.interfaces.Resettable


interface ICullable : Resettable {


    fun shouldBeCulled(delta: Float): Boolean


    override fun reset() {}
}
