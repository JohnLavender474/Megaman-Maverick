package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.OrderedMap
import com.engine.IGame2D
import com.engine.drawables.sprites.SpriteSystem
import com.engine.systems.IGameSystem
import com.test.game.assets.SoundAsset

class LevelStateHandler(private val game: IGame2D) {

  private var systemsOnPause: OrderedMap<IGameSystem, Boolean>? = null

  fun pause() {
    val pauseMap = OrderedMap<IGameSystem, Boolean>()
    game.gameEngine.systems.forEach {
      pauseMap.put(it, it.on)
      if (it !is SpriteSystem) {
        it.on = false
      }
    }
    game.audioMan.pauseAllSounds()
    game.audioMan.pauseAllMusic()
    game.audioMan.playSound(SoundAsset.PAUSE_SOUND.source, false)
  }

  fun resume() {
    systemsOnPause?.forEach { it.key.on = it.value }
    game.audioMan.resumeAllSounds()
    game.audioMan.resumeAllMusic()
    game.audioMan.playSound(SoundAsset.PAUSE_SOUND.source, false)
  }
}
