package com.megaman.maverick.game.controllers

import com.badlogic.gdx.controllers.ControllerMapping

fun ControllerMapping.getMapping(button: MegaControllerButton) = when (button) {
    MegaControllerButton.LEFT -> buttonDpadLeft
    MegaControllerButton.RIGHT -> buttonDpadRight
    MegaControllerButton.UP -> buttonDpadUp
    MegaControllerButton.DOWN -> buttonDpadDown
    MegaControllerButton.A -> buttonB
    MegaControllerButton.B -> buttonY
    MegaControllerButton.START -> buttonStart
    MegaControllerButton.SELECT -> buttonA
}
