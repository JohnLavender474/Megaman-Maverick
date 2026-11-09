package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.IDrawOffsetSource
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.IDrawOffsets
import com.megaman.maverick.game.players.MegaPlayers

class PlayerDrawOffset(private val players: MegaPlayers) : IDrawOffsetSource {

    override fun getDrawOffset(offsets: IDrawOffsets, out: Vector2): Vector2 =
        offsets.getDrawOffset(players.get(PlayerSlot(0))!!.body, out)
}
