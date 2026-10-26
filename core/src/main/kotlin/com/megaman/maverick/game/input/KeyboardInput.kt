package com.megaman.maverick.game.input

import com.badlogic.gdx.Gdx
import com.maverick.game.engine.controller.buttons.ControllerButtons
import com.megaman.maverick.game.controllers.MegaControllerButton

class KeyboardInput(private val buttons: ControllerButtons) {

    fun held(): Long {
        var held = 0L
        val keys = buttons.orderedKeys()
        for (i in 0 until keys.size) {
            val button = keys[i] as MegaControllerButton
            val code = buttons[button].keyboardCode
            if (Gdx.input.isKeyPressed(code)) held = held or (1L shl button.index)
        }
        return held
    }
}
