package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.drawables.sprites.SpritesSystem
import com.maverick.game.engine.systems.GameSystem
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset

class LevelStateHandler(private val game: MegamanMaverickGame) {

    private val onBeforePause = OrderedMap<GameSystem, Boolean>()
    private var paused = false

    fun pause() {
        if (!paused) {
            onBeforePause.clear()
            game.engine.systems.forEach { onBeforePause.put(it, it.on) }
            paused = true
        }
        game.engine.systems.forEach { if (it !is SpritesSystem) it.on = false }

        game.audio.pauseAllSound()
        game.audio.playSound(SoundAsset.PAUSE_SOUND, false)
    }

    fun resume() {
        if (paused) {
            onBeforePause.forEach { it.key.on = it.value }
            clear()
        }

        game.audio.resumeAllSound()
        game.audio.playSound(SoundAsset.PAUSE_SOUND, false)
    }

    fun turnOnAllSystems() = game.engine.systems.forEach { setOn(it, on = true) }

    fun turnOffAllSystemsButSprites() =
        game.engine.systems.forEach { if (it !is SpritesSystem) setOn(it, on = false) }

    fun clear() {
        onBeforePause.clear()
        paused = false
    }

    private fun setOn(system: GameSystem, on: Boolean) {
        if (paused) onBeforePause.put(system, on) else system.on = on
    }
}
