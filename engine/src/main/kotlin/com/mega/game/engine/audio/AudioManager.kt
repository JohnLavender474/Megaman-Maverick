package com.mega.game.engine.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

object AudioManager : IAudioManager {

    private const val MIN_VOLUME = 0f
    private const val MAX_VOLUME = 10f
    private const val DEFAULT_VOLUME = 5f

    override var soundVolume = DEFAULT_VOLUME
        set(value) {
            field = restrictVolume(value)
        }
        get() = field / MAX_VOLUME

    override var musicVolume = DEFAULT_VOLUME
        set(value) {
            field = restrictVolume(value)
        }
        get() = field / MAX_VOLUME

    override fun playMusic(key: Any?, loop: Boolean) {
        if (key is Music) {
            key.volume = musicVolume
            key.isLooping = loop
            key.play()
        }
    }

    override fun stopMusic(key: Any?) {
        if (key is Music) key.stop()
    }

    override fun pauseMusic(key: Any?) {
        if (key is Music) key.pause()
    }

    override fun playSound(key: Any?, loop: Boolean) {
        if (key is Sound) {
            if (loop) key.loop(soundVolume)
            else key.play(soundVolume)
        }
    }

    override fun stopSound(key: Any?) {
        if (key is Sound) key.stop()
    }

    override fun pauseSound(key: Any?) {
        if (key is Sound) key.pause()
    }

    private fun restrictVolume(requestedVolume: Float): Float {
        var volume = requestedVolume
        if (volume > MAX_VOLUME) volume = MAX_VOLUME
        if (volume < MIN_VOLUME) volume = MIN_VOLUME
        return volume
    }
}
