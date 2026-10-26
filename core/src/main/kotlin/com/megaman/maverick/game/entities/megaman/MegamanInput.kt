package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.input.InputFrame

internal val Megaman.input: InputFrame
    get() = controllerComponent.input!!.read()
