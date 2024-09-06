package com.megaman.maverick.game.controllers

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.controllers.Controller
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.Button
import com.mega.game.engine.controller.buttons.Buttons
import com.megaman.maverick.game.PreferenceFiles

fun getKeyboardPreferences(): Preferences =
    Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_KEYBOARD_PREFERENCES)

fun getControllerPreferences(controller: Controller): Preferences =
    Gdx.app.getPreferences("${PreferenceFiles.MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES} - ${controller.name}")

fun ControllerUtils.loadButtons(): Buttons {
    val buttons = Buttons()

    val keyboardPreferences = getKeyboardPreferences()
    ControllerButton.values().forEach {
        val keyboardCode = keyboardPreferences.getInteger(it.name, it.defaultKeyboardKey)
        buttons.put(it, Button(keyboardCode))
    }

    val controller = getController()
    if (controller != null) {
        val controllerPreferences = getControllerPreferences(controller)
        ControllerButton.values().forEach {
            val controllerCode = controllerPreferences.getInteger(it.name, controller.mapping.getMapping(it))
            val button = buttons.get(it)
            button.controllerCode = controllerCode
        }
    }

    return buttons
}

fun ControllerUtils.resetToDefaults(buttons: Buttons, isKeyboardSettings: Boolean) {
    if (isKeyboardSettings) {
        val keyboardPreferences = Gdx.app.getPreferences(PreferenceFiles.MEGAMAN_MAVERICK_KEYBOARD_PREFERENCES)
        buttons.forEach {
            val keyboardCode = (it.key as ControllerButton).defaultKeyboardKey
            it.value.keyboardCode = keyboardCode
            keyboardPreferences.putInteger((it.key as ControllerButton).name, keyboardCode)
        }
        keyboardPreferences.flush()
    } else {
        val controller = getController() ?: return
        val controllerPreferences = getControllerPreferences(controller)
        buttons.forEach {
            val controllerCode = controller.mapping.getMapping(it.key as ControllerButton)
            it.value.controllerCode = controllerCode
            controllerPreferences.putInteger((it.key as ControllerButton).name, controllerCode)
        }
        controllerPreferences.flush()
    }
}

fun ControllerUtils.getControllerCode(controller: Controller, button: ControllerButton): Int? {
    val controllerPreferences =
        Gdx.app.getPreferences("${PreferenceFiles.MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES} - ${controller.name}")
    val defaultMapping = getController()?.mapping
    return if (controllerPreferences.contains(button.name)) controllerPreferences.getInteger(button.name)
    else defaultMapping?.getMapping(button)
}