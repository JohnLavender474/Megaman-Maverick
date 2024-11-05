package com.megaman.maverick.game.controllers

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.ControllerButton
import com.mega.game.engine.controller.buttons.ControllerButtons
import com.mega.game.engine.controller.polling.ControllerPoller

class MegaControllerPoller(controllerButtons: ControllerButtons) : ControllerPoller(controllerButtons) {

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
                controllerButtons.forEach {
                    val button = it.value as ControllerButton
                    button.controllerCode = ControllerUtils.getControllerCode(
                        controller, it.key as MegaControllerButton
                    )
                }
            }
        }

        if (controllerConnected && !ControllerUtils.isControllerConnected()) {
            controllerConnected = false
            controllerButtons.forEach {
                val button = it.value as ControllerButton
                button.controllerCode = null
            }
        }

        super.run()
    }
}
