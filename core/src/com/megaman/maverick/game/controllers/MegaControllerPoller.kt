package com.megaman.maverick.game.controllers

import com.engine.common.GameLogger
import com.engine.controller.ControllerUtils
import com.engine.controller.buttons.Buttons
import com.engine.controller.polling.ControllerPoller
import com.megaman.maverick.game.ControllerButton

class MegaControllerPoller(buttons: Buttons) : ControllerPoller(buttons) {

    companion object {
        const val TAG = "MegaControllerPoller"
    }

    private var controllerConnected = false

    override fun run() {
        GameLogger.debug(TAG, "run(): controllerConnected = $controllerConnected")

        if (!controllerConnected && ControllerUtils.isControllerConnected()) {
            val controller = ControllerUtils.getController()
            if (controller != null) {
                controllerConnected = true
                buttons.forEach {
                    it.value.controllerCode = ControllerUtils.getControllerCode(
                        controller, it.key as ControllerButton
                    )
                }
            }
        }

        if (controllerConnected && !ControllerUtils.isControllerConnected()) {
            controllerConnected = false
            buttons.forEach { it.value.controllerCode = null }
        }

        super.run()
    }
}
