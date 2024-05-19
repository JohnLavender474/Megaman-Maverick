package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Input

enum class ControllerButton(val defaultKeyboardKey: Int) {
    START(Input.Keys.ENTER),
    UP(Input.Keys.W),
    DOWN(Input.Keys.S),
    LEFT(Input.Keys.A),
    RIGHT(Input.Keys.D),
    A(Input.Keys.K),
    B(Input.Keys.J);
}
