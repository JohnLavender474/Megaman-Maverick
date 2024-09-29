package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Input

enum class MegaControllerButtons(val defaultKeyboardKey: Int) {
    START(Input.Keys.ENTER),
    SELECT(Input.Keys.L),
    UP(Input.Keys.W),
    DOWN(Input.Keys.S),
    LEFT(Input.Keys.A),
    RIGHT(Input.Keys.D),
    A(Input.Keys.K),
    B(Input.Keys.J);
}
