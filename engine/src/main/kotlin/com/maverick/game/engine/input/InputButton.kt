package com.maverick.game.engine.input

interface InputButton {

    val index: Int
}

fun checkInputButtons(buttons: Iterable<InputButton>) {
    val taken = BooleanArray(InputFrame.MAX_BUTTONS)
    for (button in buttons) {
        val index = button.index
        if (index !in 0 until InputFrame.MAX_BUTTONS)
            throw IllegalArgumentException("$button has index $index, outside 0 until ${InputFrame.MAX_BUTTONS}")
        if (taken[index]) throw IllegalArgumentException("$button has index $index, which another button already has")
        taken[index] = true
    }
}
