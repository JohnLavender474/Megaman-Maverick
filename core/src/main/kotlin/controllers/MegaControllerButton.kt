package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.OrderedSet

enum class MegaControllerButton(val defaultKeyboardKey: Int) {
    START(Input.Keys.ENTER),
    SELECT(Input.Keys.L),
    UP(Input.Keys.W),
    DOWN(Input.Keys.S),
    LEFT(Input.Keys.A),
    RIGHT(Input.Keys.D),
    A(Input.Keys.K),
    B(Input.Keys.J);

    companion object {

        fun getDpadButtons(): OrderedSet<MegaControllerButton> {
            val set = OrderedSet<MegaControllerButton>()
            set.add(UP)
            set.add(DOWN)
            set.add(LEFT)
            set.add(RIGHT)
            return set
        }
    }
}
