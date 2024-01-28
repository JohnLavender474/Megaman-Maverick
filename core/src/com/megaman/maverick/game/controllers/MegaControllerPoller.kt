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
      controllerConnected = true
      val mapping = ControllerUtils.getController()?.mapping
      if (mapping != null) {
        buttons.get(ControllerButton.LEFT)?.controllerCode = mapping.buttonDpadLeft
        buttons.get(ControllerButton.RIGHT)?.controllerCode = mapping.buttonDpadRight
        buttons.get(ControllerButton.UP)?.controllerCode = mapping.buttonDpadUp
        buttons.get(ControllerButton.DOWN)?.controllerCode = mapping.buttonDpadDown
        buttons.get(ControllerButton.A)?.controllerCode = mapping.buttonB
        buttons.get(ControllerButton.B)?.controllerCode = mapping.buttonY
        buttons.get(ControllerButton.START).controllerCode = mapping.buttonStart
      }
    }

    if (controllerConnected && !ControllerUtils.isControllerConnected()) {
      controllerConnected = false
      buttons.forEach { it.value.controllerCode = null }
    }

    super.run()
  }
}
