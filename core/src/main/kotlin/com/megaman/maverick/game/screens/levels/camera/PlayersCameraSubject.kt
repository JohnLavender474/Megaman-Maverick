package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.CameraSubject
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.entities.focus
import com.megaman.maverick.game.entities.roomSubject
import com.megaman.maverick.game.players.MegaPlayers

class PlayersCameraSubject(private val players: MegaPlayers, private val playersFocus: PlayersFocus?) {

    private val playerDrawOffset = PlayerDrawOffset(players)

    fun applyTo(subject: CameraSubject) {
        val lead = players.get(PlayerSlot(0))!!
        subject.set(playersFocus ?: lead.focus, lead.roomSubject, playersFocus ?: playerDrawOffset)
    }
}
