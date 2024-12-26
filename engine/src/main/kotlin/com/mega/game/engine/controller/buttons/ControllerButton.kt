package com.mega.game.engine.controller.buttons

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.controller.ControllerUtils


class ControllerButton(
    var keyboardCode: Int,
    var controllerCode: Int? = null,
    var alternateActuators: OrderedMap<Any, () -> Boolean> = OrderedMap(),
    var enabled: Boolean = true
) : IControllerButton {

    fun isKeyboardKeyPressed() = Gdx.input.isKeyPressed(keyboardCode)

    fun isControllerKeyPressed() = ControllerUtils.isControllerConnected() &&
            controllerCode?.let { ControllerUtils.isControllerKeyPressed(it) } == true

    fun isAnyAlternateActuatorPressed() = alternateActuators.values().any { it.invoke() }

    override fun isPressed() = isKeyboardKeyPressed() || isControllerKeyPressed() || isAnyAlternateActuatorPressed()

    override fun isEnabled() = enabled
}
