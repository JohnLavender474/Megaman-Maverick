package com.megaman.maverick.game.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.engine.audio.IAudioManager
import com.engine.common.interfaces.Updatable
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset

class MegaAudioManager(
    private val sounds: OrderedMap<SoundAsset, Sound>,
    private val music: OrderedMap<MusicAsset, Music>
) : Updatable, IAudioManager {

  companion object {
    const val MIN_VOLUME = 0f
    const val MAX_VOLUME = 1f
    const val DEFAULT_VOLUME = .5f
  }

  private data class SoundEntry(val id: Long, val ass: SoundAsset, var time: Float = 0f)

  override var soundVolume: Float = DEFAULT_VOLUME
    set(value) {
      field = value.coerceIn(MIN_VOLUME, MAX_VOLUME)
      for (e in playingSounds) {
        val s = sounds[e.ass]
        s.setVolume(e.id, soundVolume)
      }
    }

  override var musicVolume: Float = DEFAULT_VOLUME
    set(value) {
      field = value.coerceIn(MIN_VOLUME, MAX_VOLUME)
      for (m in music.values()) m.volume = musicVolume
    }

  private val playingSounds = Array<SoundEntry>()
  private var currentMusic: Music? = null

  init {
    musicVolume = DEFAULT_VOLUME
    soundVolume = DEFAULT_VOLUME
  }

  override fun update(delta: Float) {
    val eIter = playingSounds.iterator()
    while (eIter.hasNext()) {
      val e = eIter.next()
      e.time += delta
      if (e.time > e.ass.seconds) eIter.remove()
    }
  }

  override fun playMusic(key: Any?, loop: Boolean) {
    if (key != null) {
      currentMusic?.stop()
      currentMusic = music.get(key as MusicAsset)
    }
    currentMusic?.isLooping = true
    currentMusic?.volume = musicVolume
    currentMusic?.play()
  }

  override fun stopMusic(key: Any?) {
    currentMusic?.stop()
  }

  override fun pauseMusic(key: Any?) {
    currentMusic?.pause()
  }

  override fun playSound(key: Any?, loop: Boolean) {
    val sound = sounds.get(key as SoundAsset)
    val id = sound.play(soundVolume)
    playingSounds.add(SoundEntry(id, key))
  }

  override fun stopSound(key: Any?) {
    sounds.get(key as SoundAsset).stop()
  }

  override fun pauseSound(key: Any?) {
    sounds.get(key as SoundAsset).pause()
  }

  fun pauseAllSound() {
    sounds.values().forEach { it.pause() }
  }

  fun resumeAllSound() {
    sounds.values().forEach { it.resume() }
  }
}
