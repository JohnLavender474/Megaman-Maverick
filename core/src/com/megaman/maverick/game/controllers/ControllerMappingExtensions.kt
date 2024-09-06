package com.megaman.maverick.game.controllers

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.controllers.ControllerMapping

fun ControllerMapping.getMapping(button: ControllerButton) = when (button) {
    ControllerButton.LEFT -> buttonDpadLeft
    ControllerButton.RIGHT -> buttonDpadRight
    ControllerButton.UP -> buttonDpadUp
    ControllerButton.DOWN -> buttonDpadDown
    ControllerButton.A -> buttonB
    ControllerButton.B -> buttonY
    ControllerButton.START -> buttonStart
    ControllerButton.SELECT -> buttonA
}