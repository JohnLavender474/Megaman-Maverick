package com.maverick.game.engine.common.interfaces

interface IProcessable {

    fun preProcess()

    fun process(delta: Float)

    fun postProcess()
}