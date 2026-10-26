package com.megaman.maverick.game.input

import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.controller.polling.IControllerPoller
import com.maverick.game.engine.input.InputFrame
import com.megaman.maverick.game.controllers.MegaControllerButton

fun InputFrame.Companion.of(poller: IControllerPoller): InputFrame {
    var frame = RELEASED
    for (button in MegaControllerButton.entries)
        frame = frame.with(button, poller.getStatus(button) ?: ButtonStatus.RELEASED)
    return frame
}
