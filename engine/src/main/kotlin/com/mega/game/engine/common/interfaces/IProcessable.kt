package com.mega.game.engine.common.interfaces

interface IProcessable {

    fun preProcess()

    fun process(delta: Float)

    fun postProcess()
}