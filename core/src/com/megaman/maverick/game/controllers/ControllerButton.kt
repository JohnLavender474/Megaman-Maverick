package com.megaman.maverick.game.controllers

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.Input

enum class ControllerButton(val defaultKeyboardKey: Int) {
    START(Input.Keys.ENTER),
    SELECT(Input.Keys.L),
    UP(Input.Keys.W),
    DOWN(Input.Keys.S),
    LEFT(Input.Keys.A),
    RIGHT(Input.Keys.D),
    A(Input.Keys.K),
    B(Input.Keys.J);
}
