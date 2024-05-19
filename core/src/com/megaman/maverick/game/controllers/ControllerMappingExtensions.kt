package com.megaman.maverick.game.controllers

import com.badlogic.gdx.controllers.ControllerMapping

fun ControllerMapping.getMapping(button: ControllerButton) = when (button) {
    ControllerButton.LEFT -> buttonDpadLeft
    ControllerButton.RIGHT -> buttonDpadRight
    ControllerButton.UP -> buttonDpadUp
    ControllerButton.DOWN -> buttonDpadDown
    ControllerButton.A -> buttonB
    ControllerButton.B -> buttonY
    ControllerButton.START -> buttonStart
}