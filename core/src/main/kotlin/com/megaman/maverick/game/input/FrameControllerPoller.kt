package com.megaman.maverick.game.input

import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.controller.polling.IControllerPoller
import com.maverick.game.engine.input.InputSource
import com.megaman.maverick.game.controllers.MegaControllerButton

// TODO: the menus and skip screens read slot 0's frame through this poller; they move to frames, with Esc and the
//  back action, in the menu-input branch of item 031
class FrameControllerPoller(private val source: InputSource) : IControllerPoller {

    override var on = true

    override fun getStatus(key: Any): ButtonStatus? =
        if (key is MegaControllerButton) source.read().getStatus(key) else null

    override fun run() {}
}
