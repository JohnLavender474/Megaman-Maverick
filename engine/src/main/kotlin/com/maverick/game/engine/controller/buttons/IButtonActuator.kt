package com.maverick.game.engine.controller.buttons

interface IButtonActuator {

    fun onJustPressed()

    fun onPressContinued(delta: Float)

    fun onJustReleased()

    fun onReleaseContinued(delta: Float)
}
