package com.megaman.maverick.game.screens.levels.respawn

import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class RespawnContext(
    val game: MegamanMaverickGame,
    val deathFade: DeathFadeOut,
    val beam: BeamInView,
    val resetLevel: () -> Unit,
    val levelMusic: () -> MusicAsset?
) {

    // TODO: the death and the respawn act on the lead player in slot 0; with more players, each player needs its own
    //  flow (item 047)
    val lead: MegaGameEntity
        get() = game.players.get(PlayerSlot(0))!!

    var delta = 0f

    internal var gameOver = false
    internal var eventsRunMark = 0L
    internal var respawnBegun = false
}
