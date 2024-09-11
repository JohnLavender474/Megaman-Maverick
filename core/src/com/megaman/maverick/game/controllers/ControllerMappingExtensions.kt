package com.megaman.maverick.game.controllers

import com.badlogic.gdx.controllers.ControllerMapping

fun ControllerMapping.getMapping(button: MegaControllerButtons) = when (button) {
    MegaControllerButtons.LEFT -> buttonDpadLeft
    MegaControllerButtons.RIGHT -> buttonDpadRight
    MegaControllerButtons.UP -> buttonDpadUp
    MegaControllerButtons.DOWN -> buttonDpadDown
    MegaControllerButtons.A -> buttonB
    MegaControllerButtons.B -> buttonY
    MegaControllerButtons.START -> buttonStart
    MegaControllerButtons.SELECT -> buttonA
}