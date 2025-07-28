package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.drawables.sprites.SpritesSystem
import com.mega.game.engine.systems.GameSystem
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

        game.audioMan.pauseAllSound()
        game.audioMan.playSound(SoundAsset.PAUSE_SOUND, false)
    }

    fun resume() {
        systems.forEach { it.on = true }

        game.audioMan.resumeAllSound()
        game.audioMan.playSound(SoundAsset.PAUSE_SOUND, false)
    }
}
