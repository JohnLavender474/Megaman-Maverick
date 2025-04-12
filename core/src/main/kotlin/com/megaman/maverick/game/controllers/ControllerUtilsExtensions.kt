package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.controllers.Controller
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.ControllerButton
import com.mega.game.engine.controller.buttons.ControllerButtons
import com.megaman.maverick.game.PreferenceFiles

fun getKeyboardPreferences(): Preferences =
    Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_KEYBOARD_PREFERENCES)

fun getControllerPreferences(controller: Controller): Preferences =
    Gdx.app.getPreferences("${PreferenceFiles.MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES} - ${controller.name}")

fun ControllerUtils.loadControllerButtons(): ControllerButtons {
    val buttons = ControllerButtons()

    MegaControllerButton.entries.forEach {
        val keyboardCode = it.defaultKeyboardKey
        buttons.put(it, ControllerButton(keyboardCode))
    }

    val controller = getController()
    if (controller != null) {
        MegaControllerButton.entries.forEach {
            val controllerCode = controller.mapping.getMapping(it)
            val button = buttons.get(it) as ControllerButton
            button.controllerCode = controllerCode
        }
    }

    return buttons
}

fun ControllerUtils.saveSettingsToPrefs(buttons: ControllerButtons, isKeyboardSettings: Boolean) {
    if (isKeyboardSettings) {
        val keyboardPrefs = getKeyboardPreferences()
        buttons.forEach {
            val button = it.value as ControllerButton
            keyboardPrefs.putInteger((it.key as MegaControllerButton).name, button.keyboardCode)
        }
        keyboardPrefs.flush()
    } else {
        val controller = getController() ?: return
        val controllerPrefs = getControllerPreferences(controller)
        buttons.forEach {
            val button = it.value as ControllerButton
            val code = button.controllerCode ?: controller.mapping.getMapping(it.key as MegaControllerButton)
            controllerPrefs.putInteger((it.key as MegaControllerButton).name, code)
        }
    }
}

fun ControllerUtils.resetSettingsToDefaults(buttons: ControllerButtons, isKeyboardSettings: Boolean) {
    if (isKeyboardSettings) buttons.forEach {
        val button = it.value as ControllerButton
        val keyboardCode = (it.key as MegaControllerButton).defaultKeyboardKey
        button.keyboardCode = keyboardCode
    } else {
        val controller = getController() ?: return
        buttons.forEach {
            val button = it.value as ControllerButton
            val controllerCode = controller.mapping.getMapping(it.key as MegaControllerButton)
            button.controllerCode = controllerCode
        }
    }
}

fun ControllerUtils.getControllerCode(controller: Controller, button: MegaControllerButton): Int? {
    val controllerPreferences =
        Gdx.app.getPreferences("${PreferenceFiles.MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES} - ${controller.name}")
    val defaultMapping = getController()?.mapping
    return when {
        controllerPreferences.contains(button.name) -> controllerPreferences.getInteger(button.name)
        else -> defaultMapping?.getMapping(button)
    }
}
