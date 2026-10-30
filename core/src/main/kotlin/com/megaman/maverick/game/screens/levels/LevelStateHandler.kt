package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.drawables.sprites.SpritesSystem
import com.maverick.game.engine.systems.GameSystem
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset

class LevelStateHandler(private val game: MegamanMaverickGame) {

    private val systems = OrderedSet<GameSystem>()

    fun pause() {
        systems.clear()

        game.engine.systems.forEach {
            systems.add(it)
            if (it !is SpritesSystem) it.on = false
        }

        game.audio.pauseAllSound()
        game.audio.playSound(SoundAsset.PAUSE_SOUND, false)
    }

    fun resume() {
        systems.forEach { it.on = true }

        game.audio.resumeAllSound()
        game.audio.playSound(SoundAsset.PAUSE_SOUND, false)
    }
}
