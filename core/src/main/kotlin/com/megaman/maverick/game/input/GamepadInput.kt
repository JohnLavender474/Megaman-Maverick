package com.megaman.maverick.game.input

import com.megaman.maverick.game.controllers.ControllerUtils
import com.maverick.game.engine.controller.buttons.ControllerButtons
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.getMapping
import com.megaman.maverick.game.controllers.getPreferredControllerCode

class GamepadInput(private val buttons: ControllerButtons) {

    var loadPrefsOnConnect = false

    private var connected = false

    fun held(): Long {
        if (!connected && ControllerUtils.isControllerConnected()) {
            val controller = ControllerUtils.getController()
            if (controller != null) {
                connected = true
                buttons.forEach {
                    val key = it.key as MegaControllerButton
                    val button = it.value
                    var code: Int? = null
                    if (loadPrefsOnConnect) code = getPreferredControllerCode(controller, key)
                    if (code == null) code = controller.mapping.getMapping(key)
                    button.controllerCode = code
                }
            }
        }

        if (connected && !ControllerUtils.isControllerConnected()) {
            connected = false
            buttons.forEach { it.value.controllerCode = null }
        }

        var held = 0L
        val keys = buttons.orderedKeys()
        for (i in 0 until keys.size) {
            val button = keys[i] as MegaControllerButton
            val code = buttons[button].controllerCode ?: continue
            if (ControllerUtils.isControllerConnected() && ControllerUtils.isControllerKeyPressed(code))
                held = held or (1L shl button.index)
        }
        return held
    }
}
