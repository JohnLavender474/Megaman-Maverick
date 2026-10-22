package com.maverick.game.engine.common.interfaces

interface IFinishable {

    fun isFinished(): Boolean
}

interface IJustFinishable: IFinishable {

    fun isJustFinished(): Boolean
}
