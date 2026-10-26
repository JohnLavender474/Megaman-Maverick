package com.maverick.game.engine.input

import com.maverick.game.engine.controller.buttons.ButtonStatus

@JvmInline
value class InputFrame(val bits: Long) {

    companion object {
        const val MAX_BUTTONS = 32

        private const val BITS_PER_BUTTON = 2
        private const val STATUS_MASK = 3L

        private val STATUSES = ButtonStatus.entries

        val RELEASED = InputFrame(filledWith(ButtonStatus.RELEASED))

        private fun filledWith(status: ButtonStatus): Long {
            var bits = 0L
            for (index in 0 until MAX_BUTTONS) bits = bits or (status.ordinal.toLong() shl (index * BITS_PER_BUTTON))
            return bits
        }
    }

    fun getStatus(button: InputButton) =
        STATUSES[((bits ushr (button.index * BITS_PER_BUTTON)) and STATUS_MASK).toInt()]

    fun isPressed(button: InputButton): Boolean {
        val status = getStatus(button)
        return status == ButtonStatus.PRESSED || status == ButtonStatus.JUST_PRESSED
    }

    fun isJustPressed(button: InputButton) = getStatus(button) == ButtonStatus.JUST_PRESSED

    fun isJustReleased(button: InputButton) = getStatus(button) == ButtonStatus.JUST_RELEASED

    fun with(button: InputButton, status: ButtonStatus): InputFrame {
        val shift = button.index * BITS_PER_BUTTON
        return InputFrame((bits and (STATUS_MASK shl shift).inv()) or (status.ordinal.toLong() shl shift))
    }
}
