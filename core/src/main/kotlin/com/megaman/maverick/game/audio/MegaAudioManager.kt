package com.megaman.maverick.game.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.audio.IAudioManager
import com.mega.game.engine.audio.SoundRequest
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
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

    private class SoundEntry(
        val id: Long,
        val ass: SoundAsset,
        var loop: Boolean = false,
        var time: Float = 0f,
    ) {

        override fun hashCode() = ass.hashCode()

        override fun equals(other: Any?) = other is SoundEntry && other.ass == ass
    }

    override var soundVolume: Float = DEFAULT_VOLUME
        set(value) {
            field = value.coerceIn(MIN_VOLUME, MAX_VOLUME)
            for (e in playingSoundEntries) {
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
    private val playingSoundEntries = Array<SoundEntry>()
    private val playingSoundsMap = ObjectMap<SoundAsset, Int>()

    private var currentMusic: Music? = null
    private var currentMusicAss: MusicAsset? = null
    private var fadeOutMusicTimer: Timer? = null
    private var musicPaused = false

    init {
        musicVolume = DEFAULT_VOLUME
        soundVolume = DEFAULT_VOLUME
    }

    override fun update(delta: Float) {
        soundQueue.forEach { request ->
            val source = request.source as SoundAsset

            if (!request.allowOverlap && playingSoundsMap.containsKey(source)) return@forEach

            val sound = sounds.get(source)
            val id = if (request.loop) sound.loop(soundVolume) else sound.play(soundVolume)

            playingSoundEntries.add(SoundEntry(id, source, request.loop))

            val count = playingSoundsMap.get(source, 0)
            playingSoundsMap.put(source, count + 1)
        }
        soundQueue.clear()

        val eIter = playingSoundEntries.iterator()
        while (eIter.hasNext()) {
            val e = eIter.next()
            if (e.loop) continue

            e.time += delta
            if (e.time >= e.ass.seconds) {
                eIter.remove()

                val count = playingSoundsMap.get(e.ass, 0) - 1
                if (count <= 0) playingSoundsMap.remove(e.ass) else playingSoundsMap.put(e.ass, count)
            }
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
        if (key == null) {
            if (currentMusic?.isPlaying != true) currentMusic?.play()
            return
        }

        if (currentMusic?.isPlaying == true) currentMusic?.stop()

        key as MusicAsset

        currentMusic = music.get(key)
        currentMusicAss = key

        if (currentMusic == null) return

        currentMusic!!.let {
            it.isLooping = loop
            it.volume = musicVolume
            it.setOnCompletionListener { key.onCompletion?.invoke(this) }
            it.play()
        }

        musicPaused = false
        fadeOutMusicTimer = null
    }

    fun getCurrentMusicAsset() = currentMusicAss

    fun unsetMusic() {
        currentMusic?.stop()
        currentMusic = null
        currentMusicAss = null
    }

    override fun stopMusic(key: Any?) {
        if (currentMusic?.isPlaying == true) currentMusic?.stop()
        fadeOutMusicTimer = null
        musicPaused = true
    }

    override fun pauseMusic(key: Any?) {
        if (currentMusic?.isPlaying == true) currentMusic?.pause()
        musicPaused = true
    }

    override fun playSound(key: Any?, loop: Boolean) {
        if (key == null) return
        playSound(SoundRequest(key, loop))
    }

    fun playSound(soundRequest: SoundRequest) = soundQueue.add(soundRequest)

    override fun stopSound(key: Any?) = sounds.get(key as SoundAsset).stop()

    override fun pauseSound(key: Any?) = sounds.get(key as SoundAsset).pause()

    fun stopAllLoopingSounds() {
        playingSoundEntries.filter { it.loop }.forEach {
            val sound = sounds[it.ass]
            sound.stop(it.id)

            playingSoundEntries.removeValue(it, false)
            playingSoundsMap.remove(it.ass)
        }
    }

    fun pauseAllSound() = sounds.values().forEach { it.pause() }

    fun resumeAllSound() = sounds.values().forEach { it.resume() }

    fun isSoundPlaying(sound: SoundAsset) = playingSoundEntries.any { it.ass == sound }

    fun fadeOutMusic(time: Float) {
        fadeOutMusicTimer = Timer(time)
        fadeOutMusicTimer!!.setRunOnJustFinished { currentMusic?.volume = musicVolume }
    }

    fun stopFadingOutMusic() {
        fadeOutMusicTimer = null
        currentMusic?.volume = musicVolume
    }
}
