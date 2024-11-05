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

fun ControllerUtils.loadButtons(): ControllerButtons {
    val buttons = ControllerButtons()

    val keyboardPreferences = getKeyboardPreferences()
    MegaControllerButton.entries.forEach {
        val keyboardCode = keyboardPreferences.getInteger(it.name, it.defaultKeyboardKey)
        buttons.put(it, ControllerButton(keyboardCode))
    }

    val controller = getController()
    if (controller != null) {
        val controllerPreferences = getControllerPreferences(controller)
        MegaControllerButton.entries.forEach {
            val controllerCode = controllerPreferences.getInteger(it.name, controller.mapping.getMapping(it))
            val button = buttons.get(it) as ControllerButton
            button.controllerCode = controllerCode
        }
    }

    return buttons
}

fun ControllerUtils.resetToDefaults(buttons: ControllerButtons, isKeyboardSettings: Boolean) {
    if (isKeyboardSettings) {
        val keyboardPreferences = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_KEYBOARD_PREFERENCES)
        buttons.forEach {
            val button = it.value as ControllerButton
            val keyboardCode = (it.key as MegaControllerButton).defaultKeyboardKey
            button.keyboardCode = keyboardCode
            keyboardPreferences.putInteger((it.key as MegaControllerButton).name, keyboardCode)
        }
        keyboardPreferences.flush()
    } else {
        val controller = getController() ?: return
        val controllerPreferences = getControllerPreferences(controller)
        buttons.forEach {
            val button = it.value as ControllerButton
            val controllerCode = controller.mapping.getMapping(it.key as MegaControllerButton)
            button.controllerCode = controllerCode
            controllerPreferences.putInteger((it.key as MegaControllerButton).name, controllerCode)
        }
        controllerPreferences.flush()
    }
}

fun ControllerUtils.getControllerCode(controller: Controller, button: MegaControllerButton): Int? {
    val controllerPreferences =
        Gdx.app.getPreferences("${PreferenceFiles.MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES} - ${controller.name}")
    val defaultMapping = getController()?.mapping
    return if (controllerPreferences.contains(button.name)) controllerPreferences.getInteger(button.name)
    else defaultMapping?.getMapping(button)
}
