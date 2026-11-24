package com.megaman.maverick.game.players

import com.badlogic.gdx.Gdx
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction

class PlayerSettingsStore(slot: PlayerSlot, fileNamePrefix: String) {

    companion object {
        private const val KEYBOARD_PREFIX = "KEYBOARD"
        private const val GAMEPAD_PREFIX = "GAMEPAD"
        private const val SELECT_BUTTON_ACTION_KEY = "SELECT_BUTTON_ACTION"
    }

    private val fileName = "$fileNamePrefix - Slot ${slot.index}"

    private fun prefs() = Gdx.app.getPreferences(fileName)

    private fun keyboardKey(button: MegaControllerButton) = "$KEYBOARD_PREFIX.${button.name}"

    private fun gamepadKey(padName: String, button: MegaControllerButton) = "$GAMEPAD_PREFIX.$padName.${button.name}"

    // TODO: a saved code that is not a number throws here, as on master, so with --loadKeyboardPrefs the game does not
    //  start until --resetBindings runs; a hand-edited duplicate or reserved key loads as written and an alias then
    //  gives 2 buttons; follow-up in item 034 (see 031e CONTRACT.md, question 3)
    private fun readCode(key: String): Int? {
        val prefs = prefs()
        return if (prefs.contains(key)) prefs.getInteger(key) else null
    }

    fun clear() {
        val prefs = prefs()
        prefs.clear()
        prefs.flush()
    }

    fun clearKeyboard() {
        val prefs = prefs()
        MegaControllerButton.entries.forEach { prefs.remove(keyboardKey(it)) }
        prefs.remove(SELECT_BUTTON_ACTION_KEY)
        prefs.flush()
    }

    fun clearGamepad(padName: String) {
        val prefs = prefs()
        MegaControllerButton.entries.forEach { prefs.remove(gamepadKey(padName, it)) }
        prefs.remove(SELECT_BUTTON_ACTION_KEY)
        prefs.flush()
    }

    fun saveKeyboard(settings: PlayerSettings) {
        val prefs = prefs()
        MegaControllerButton.entries.forEach { prefs.putInteger(keyboardKey(it), settings.keyCodes[it.index]) }
        prefs.flush()
    }

    fun loadKeyboard(settings: PlayerSettings) {
        val defaulted = ArrayList<MegaControllerButton>()
        MegaControllerButton.entries.forEach { button ->
            val code = readCode(keyboardKey(button))
            if (code != null) settings.keyCodes[button.index] = code else defaulted.add(button)
        }
        unbindClashingDefaults(settings.keyCodes, defaulted)
    }

    fun saveGamepad(padName: String, settings: PlayerSettings) {
        val prefs = prefs()
        MegaControllerButton.entries.forEach { prefs.putInteger(gamepadKey(padName, it), settings.padCodes[it.index]) }
        prefs.flush()
    }

    fun loadGamepad(padName: String, settings: PlayerSettings) {
        val defaulted = ArrayList<MegaControllerButton>()
        MegaControllerButton.entries.forEach { button ->
            val code = readGamepadCode(padName, button)
            if (code != null) settings.padCodes[button.index] = code else defaulted.add(button)
        }
        unbindClashingDefaults(settings.padCodes, defaulted)
    }

    fun readGamepadCode(padName: String, button: MegaControllerButton) = readCode(gamepadKey(padName, button))

    fun saveSelectButtonAction(settings: PlayerSettings) {
        val prefs = prefs()
        prefs.putString(SELECT_BUTTON_ACTION_KEY, settings.selectButtonAction.name)
        prefs.flush()
    }

    fun loadSelectButtonAction(): SelectButtonAction? {
        val prefs = prefs()
        if (!prefs.contains(SELECT_BUTTON_ACTION_KEY)) return null
        val name = prefs.getString(SELECT_BUTTON_ACTION_KEY) ?: return null
        return try {
            SelectButtonAction.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

fun applyBootSettings(
    store: PlayerSettingsStore,
    settings: PlayerSettings,
    loadKeyboardPrefs: Boolean,
    resetBindings: Boolean
) {
    if (resetBindings) store.clear()
    store.loadSelectButtonAction()?.let { settings.selectButtonAction = it }
    if (loadKeyboardPrefs) store.loadKeyboard(settings)
}
