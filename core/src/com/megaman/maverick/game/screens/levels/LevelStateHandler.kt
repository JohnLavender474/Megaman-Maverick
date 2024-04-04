package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.OrderedMap
import com.engine.drawables.sprites.SpritesSystem
import com.engine.systems.IGameSystem
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset

class LevelStateHandler(private val game: MegamanMaverickGame) {

    private val systemsOnPause = OrderedMap<IGameSystem, Boolean>()

    fun pause() {
        systemsOnPause.clear()

        game.engine.systems.forEach {
            systemsOnPause.put(it, it.on)
            if (it !is SpritesSystem) it.on = false
        }

        game.audioMan.pauseAllSound()
        game.audioMan.pauseMusic()
        game.audioMan.playSound(SoundAsset.PAUSE_SOUND, false)
    }

    fun resume() {
        systemsOnPause.forEach { it.key.on = it.value }

        game.audioMan.resumeAllSound()
        game.audioMan.playMusic()
        game.audioMan.playSound(SoundAsset.PAUSE_SOUND, false)
    }
}
