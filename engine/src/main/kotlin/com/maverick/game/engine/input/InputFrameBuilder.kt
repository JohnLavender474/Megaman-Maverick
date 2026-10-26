package com.maverick.game.engine.input

import com.maverick.game.engine.controller.buttons.ButtonStatus

class InputFrameBuilder(buttons: Iterable<InputButton>) {

    private val buttons = buttons.toList().toTypedArray()

    var locked = false

    var frame = InputFrame.RELEASED
        private set

    fun build(held: Long): InputFrame {
        var next = frame
        for (button in buttons) {
            val old = frame.getStatus(button)
            val new = if (locked) when (old) {
                ButtonStatus.PRESSED, ButtonStatus.JUST_PRESSED -> ButtonStatus.JUST_RELEASED
                ButtonStatus.RELEASED, ButtonStatus.JUST_RELEASED -> ButtonStatus.RELEASED
            } else {
                val pressed = (held ushr button.index) and 1L != 0L
                when (old) {
                    ButtonStatus.RELEASED,
                    ButtonStatus.JUST_RELEASED -> if (pressed) ButtonStatus.JUST_PRESSED else ButtonStatus.RELEASED

                    else -> if (pressed) ButtonStatus.PRESSED else ButtonStatus.JUST_RELEASED
                }
            }
            next = next.with(button, new)
        }
        frame = next
        return next
    }
}
