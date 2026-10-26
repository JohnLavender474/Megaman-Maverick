package com.megaman.maverick.game.input

import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.controller.polling.IControllerPoller
import com.megaman.maverick.game.controllers.MegaControllerButton

@JvmInline
value class InputFrame internal constructor(internal val bits: Int) {

    companion object {
        private const val BITS_PER_BUTTON = 2
        private const val STATUS_MASK = 3

        private val BUTTONS = MegaControllerButton.entries
        private val STATUSES = ButtonStatus.entries

        val RELEASED = of { ButtonStatus.RELEASED }

        fun of(poller: IControllerPoller) = of { poller.getStatus(it) ?: ButtonStatus.RELEASED }

        private inline fun of(statusOf: (MegaControllerButton) -> ButtonStatus): InputFrame {
            var bits = 0
            for (button in BUTTONS) bits = bits or (statusOf(button).ordinal shl (button.ordinal * BITS_PER_BUTTON))
            return InputFrame(bits)
        }
    }

    fun getStatus(button: MegaControllerButton) =
        STATUSES[(bits ushr (button.ordinal * BITS_PER_BUTTON)) and STATUS_MASK]

    fun isPressed(button: MegaControllerButton): Boolean {
        val status = getStatus(button)
        return status == ButtonStatus.PRESSED || status == ButtonStatus.JUST_PRESSED
    }

    fun isJustPressed(button: MegaControllerButton) = getStatus(button) == ButtonStatus.JUST_PRESSED

    fun isJustReleased(button: MegaControllerButton) = getStatus(button) == ButtonStatus.JUST_RELEASED
}
