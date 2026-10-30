package com.megaman.maverick.game.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.controllers.ControllerUtils
import com.maverick.game.engine.controller.buttons.ControllerButtons
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.getMapping
import com.megaman.maverick.game.controllers.getPreferredControllerCode

class GamepadInput(private val buttons: ControllerButtons) {

    companion object {
        const val TRIGGER_LEFT_AXIS = 4
        const val TRIGGER_RIGHT_AXIS = 5
        const val TRIGGER_LEFT_CODE = 1004
        const val TRIGGER_RIGHT_CODE = 1005
        const val TRIGGER_THRESHOLD = 0.5f
        private const val STICK_THRESHOLD = 0.5f
    }

    var loadPrefsOnConnect = false

    private var primary: Controller? = null
    private val padsOfTick = Array<Controller>()

    fun held(): Long {
        val pads = ControllerUtils.getControllers()
        val first = if (pads.size > 0) pads[0] else null

        if (first !== primary) {
            if (first != null) buttons.forEach {
                val key = it.key as MegaControllerButton
                val button = it.value
                var code: Int? = null
                if (loadPrefsOnConnect) code = getPreferredControllerCode(first, key)
                if (code == null) code = first.mapping.getMapping(key)
                button.controllerCode = code
            } else buttons.forEach { it.value.controllerCode = null }
            primary = first
        }

        padsOfTick.clear()
        padsOfTick.addAll(pads)

        var held = 0L
        val keys = buttons.orderedKeys()
        for (p in 0 until padsOfTick.size) {
            val pad = padsOfTick[p]
            for (i in 0 until keys.size) {
                val button = keys[i] as MegaControllerButton
                val code = buttons[button].controllerCode ?: continue
                val pressed = when (code) {
                    TRIGGER_LEFT_CODE -> pad.getAxis(TRIGGER_LEFT_AXIS) >= TRIGGER_THRESHOLD
                    TRIGGER_RIGHT_CODE -> pad.getAxis(TRIGGER_RIGHT_AXIS) >= TRIGGER_THRESHOLD
                    else -> pad.getButton(code)
                }
                if (pressed) held = held or (1L shl button.index)
            }

            val mapping = pad.mapping
            val x = pad.getAxis(mapping.axisLeftX)
            if (x <= -STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.LEFT.index)
            else if (x >= STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.RIGHT.index)
            val y = pad.getAxis(mapping.axisLeftY)
            if (y <= -STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.UP.index)
            else if (y >= STICK_THRESHOLD) held = held or (1L shl MegaControllerButton.DOWN.index)
        }
        return held
    }
}
