package com.megaman.maverick.game.players

import com.badlogic.gdx.Input
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.input.GamepadInput
import com.megaman.maverick.game.input.KeyboardInput

enum class KeyCapture { ACCEPT, KEEP, REFUSE }

class PlayerSettings {

    companion object {
        const val NO_CODE = Int.MIN_VALUE
    }

    val keyCodes = IntArray(MegaControllerButton.entries.size).also { codes ->
        MegaControllerButton.entries.forEach { codes[it.index] = it.defaultKeyboardKey }
    }

    val padCodes = IntArray(MegaControllerButton.entries.size) { NO_CODE }

    var selectButtonAction = SelectButtonAction.NEXT_WEAPON

    fun captureKey(button: MegaControllerButton, keycode: Int): KeyCapture {
        val oldCode = keyCodes[button.index]
        if (keycode == oldCode) return KeyCapture.KEEP
        if (KeyboardInput.isReservedKey(keycode)) return KeyCapture.REFUSE
        if (KeyboardInput.isReservedKey(oldCode) && keycode in keyCodes) return KeyCapture.REFUSE

        for (i in keyCodes.indices) if (keyCodes[i] == keycode) keyCodes[i] = oldCode
        keyCodes[button.index] = keycode
        return KeyCapture.ACCEPT
    }
}

fun keyLabel(code: Int): String {
    if (code == PlayerSettings.NO_CODE) return "NONE"
    if (code !in 0..255) return code.toString()
    return Input.Keys.toString(code) ?: code.toString()
}

fun padLabel(code: Int) = when (code) {
    PlayerSettings.NO_CODE -> "NONE"
    GamepadInput.TRIGGER_LEFT_CODE -> "L2"
    GamepadInput.TRIGGER_RIGHT_CODE -> "R2"
    else -> code.toString()
}

fun unbindClashingDefaults(codes: IntArray, defaulted: List<MegaControllerButton>) {
    defaulted.forEach { button ->
        val code = codes[button.index]
        val clash = MegaControllerButton.entries.any { it != button && it !in defaulted && codes[it.index] == code }
        if (clash) codes[button.index] = PlayerSettings.NO_CODE
    }
}
