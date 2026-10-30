package com.megaman.maverick.game.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.maverick.game.engine.controller.buttons.ControllerButtons
import com.megaman.maverick.game.controllers.MegaControllerButton

class KeyboardInput(private val buttons: ControllerButtons) {

    companion object {
        // TODO: a key that is bound to a button and is also an alias gives both buttons (for example a button bound to the up arrow also gives UP); 031e refuses the reserved keys on the bindings screen
        private val ALIAS_KEYS = intArrayOf(
            Input.Keys.ENTER,
            Input.Keys.NUMPAD_ENTER,
            Input.Keys.UP,
            Input.Keys.DOWN,
            Input.Keys.LEFT,
            Input.Keys.RIGHT,
        )
        private val ALIAS_BITS = longArrayOf(
            1L shl MegaControllerButton.START.index,
            1L shl MegaControllerButton.START.index,
            1L shl MegaControllerButton.UP.index,
            1L shl MegaControllerButton.DOWN.index,
            1L shl MegaControllerButton.LEFT.index,
            1L shl MegaControllerButton.RIGHT.index,
        )
    }

    fun held(): Long {
        var held = 0L
        val keys = buttons.orderedKeys()
        for (i in 0 until keys.size) {
            val button = keys[i] as MegaControllerButton
            val code = buttons[button].keyboardCode
            if (Gdx.input.isKeyPressed(code)) held = held or (1L shl button.index)
        }
        for (i in ALIAS_KEYS.indices) {
            if (Gdx.input.isKeyPressed(ALIAS_KEYS[i])) held = held or ALIAS_BITS[i]
        }
        return held
    }
}
