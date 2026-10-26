package com.maverick.game.engine.controller.buttons

class ButtonActuator(
    var onJustPressed: (() -> Unit)? = null,
    var onPressContinued: ((Float) -> Unit)? = null,
    var onJustReleased: (() -> Unit)? = null,
    var onReleaseContinued: ((Float) -> Unit)? = null
) : IButtonActuator {

    override fun onJustPressed() {
        onJustPressed?.invoke()
    }

    override fun onPressContinued(delta: Float) {
        onPressContinued?.invoke(delta)
    }

    override fun onJustReleased() {
        onJustReleased?.invoke()
    }

    override fun onReleaseContinued(delta: Float) {
        onReleaseContinued?.invoke(delta)
    }
}
