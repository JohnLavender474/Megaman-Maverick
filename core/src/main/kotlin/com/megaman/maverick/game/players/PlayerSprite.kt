package com.megaman.maverick.game.players

import com.maverick.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.entities.special.PipePortal
import com.megaman.maverick.game.entities.state.frozen
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.teleporter
import com.megaman.maverick.game.entities.state.teleporting

fun MegaPlayer.applyHideAndFlash(sprite: GameSprite) {
    sprite.setAlpha(if (damageRecovery.flash) 0f else 1f)
    sprite.hidden = shouldHideSprite()
}

private fun MegaPlayer.shouldHideSprite(): Boolean {
    if (frozen) return false
    if (!inPlay) return true
    return teleporting && teleporter !is PipePortal
}
