package com.megaman.maverick.game.input

import com.maverick.game.engine.players.PlayerSlot

class PlayerInputs(capacity: Int) {

    private val frames = IntArray(capacity) { InputFrame.RELEASED.bits }

    operator fun get(slot: PlayerSlot) = InputFrame(frames[slot.index])

    operator fun set(slot: PlayerSlot, frame: InputFrame) {
        frames[slot.index] = frame.bits
    }
}
