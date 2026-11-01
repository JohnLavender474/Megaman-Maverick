package com.megaman.maverick.game.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.PlayerSettings

class KeyboardInput(private val settings: PlayerSettings) {

    companion object {
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

        fun isReservedKey(keycode: Int) = keycode == Input.Keys.ESCAPE || keycode in ALIAS_KEYS
    }

    fun held(): Long {
        var held = 0L
        val codes = settings.keyCodes
        for (i in codes.indices) {
            if (Gdx.input.isKeyPressed(codes[i])) held = held or (1L shl i)
        }
        for (i in ALIAS_KEYS.indices) {
            if (Gdx.input.isKeyPressed(ALIAS_KEYS[i])) held = held or ALIAS_BITS[i]
        }
        return held
    }

    fun systemHeld() = if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) 1L shl SystemButton.BACK.index else 0L
}
