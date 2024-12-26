package com.mega.game.engine.cullables

import com.mega.game.engine.common.interfaces.Resettable


interface ICullable : Resettable {


    fun shouldBeCulled(delta: Float): Boolean


    override fun reset() {}
}
