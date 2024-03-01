package com.megaman.maverick.game.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.engine.audio.IAudioManager
import com.engine.audio.SoundRequest
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset

class MegaAudioManager(
    private val sounds: OrderedMap<SoundAsset, Sound>, private val music: OrderedMap<MusicAsset, Music>
) : Updatable, IAudioManager {

    companion object {
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val DEFAULT_VOLUME = 0.5f
    }

    private class SoundEntry(val id: Long, val ass: SoundAsset, var loop: Boolean = false, var time: Float = 0f) {

        override fun hashCode() = ass.hashCode()

        override fun equals(other: Any?) = other is SoundEntry && other.ass == ass

    }

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

    private val soundQueue = OrderedSet<SoundRequest>()
    private val playingSounds = Array<SoundEntry>()

    private var currentMusic: Music? = null
    private var fadeOutMusicTimer: Timer? = null
    private var musicPaused = false

    init {
        musicVolume = DEFAULT_VOLUME
        soundVolume = DEFAULT_VOLUME
    }

    override fun update(delta: Float) {
        soundQueue.forEach {
            val key = it.source as SoundAsset
            val sound = sounds.get(key)
            val id = if (it.loop) sound.loop(soundVolume) else sound.play(soundVolume)
            playingSounds.add(SoundEntry(id, key, it.loop))
        }
        soundQueue.clear()

        val eIter = playingSounds.iterator()
        while (eIter.hasNext()) {
            val e = eIter.next()
            if (e.loop) continue

            e.time += delta
            if (e.time > e.ass.seconds) eIter.remove()
        }

        if (!musicPaused && fadeOutMusicTimer != null) {
            fadeOutMusicTimer!!.update(delta)
            currentMusic?.volume = musicVolume - (musicVolume * fadeOutMusicTimer!!.getRatio())
            if (fadeOutMusicTimer!!.isFinished()) {
                fadeOutMusicTimer = null
                stopMusic()
            }
        }
    }

    override fun playMusic(key: Any?, loop: Boolean) {
        if (key == null) throw IllegalArgumentException("Key cannot be null")
        currentMusic?.stop()
        currentMusic = music.get(key as MusicAsset)
        fadeOutMusicTimer = null
        currentMusic!!.isLooping = loop
        currentMusic!!.volume = musicVolume
        currentMusic!!.play()
        musicPaused = false
    }

    override fun stopMusic(key: Any?) {
        currentMusic?.stop()
        fadeOutMusicTimer = null
        musicPaused = true
    }

    override fun pauseMusic(key: Any?) {
        currentMusic?.pause()
        musicPaused = true
    }

    override fun playSound(key: Any?, loop: Boolean) {
        if (key == null) return
        playSound(SoundRequest(key, loop))
    }

    fun playSound(soundRequest: SoundRequest) {
        soundQueue.add(soundRequest)
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

    fun isSoundPlaying(sound: SoundAsset) = playingSounds.any { it.ass == sound }

    fun fadeOutMusic(time: Float) {
        fadeOutMusicTimer = Timer(time)
        fadeOutMusicTimer!!.runOnFinished = { currentMusic?.volume = musicVolume }
    }

    fun stopFadingOutMusic() {
        fadeOutMusicTimer = null
        currentMusic?.volume = musicVolume
    }
}
