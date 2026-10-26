package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Input
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.input.InputButton

enum class MegaControllerButton(override val index: Int, val defaultKeyboardKey: Int) : InputButton {
    UP(0, Input.Keys.W),
    DOWN(1, Input.Keys.S),
    LEFT(2, Input.Keys.A),
    RIGHT(3, Input.Keys.D),
    A(4, Input.Keys.K),
    B(5, Input.Keys.J),
    START(6, Input.Keys.ENTER),
    SELECT(7, Input.Keys.L);

    fun isDpadButton() = DPAD_BUTTONS.contains(this)

    fun isActionButton() = ACTION_BUTTONS.contains(this)

    companion object {
        val DPAD_BUTTONS = ImmutableCollection(hashSetOf(UP, DOWN, LEFT, RIGHT))
        val ACTION_BUTTONS = ImmutableCollection(hashSetOf(A, B))
    }
}
