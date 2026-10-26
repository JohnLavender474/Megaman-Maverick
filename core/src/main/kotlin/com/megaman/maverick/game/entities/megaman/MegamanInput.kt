package com.megaman.maverick.game.entities.megaman

import com.megaman.maverick.game.input.InputFrame
import com.megaman.maverick.game.players.asPlayer

internal val Megaman.input: InputFrame
    get() = asPlayer!!.input
