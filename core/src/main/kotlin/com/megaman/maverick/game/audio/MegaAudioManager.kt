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
        var loop: Boolean = false,
        var time: Float = 0f,
    )

    override var soundVolume: Float = DEFAULT_VOLUME
        set(value) {
            field = value.coerceIn(MIN_VOLUME, MAX_VOLUME)
            for (mapEntry in playingSoundsMap) {
                val sound = sounds[mapEntry.key]
                for (e in mapEntry.value) sound.setVolume(e.id, soundVolume)
            }
        }

    override var musicVolume: Float = DEFAULT_VOLUME
        set(value) {
            field = value.coerceIn(MIN_VOLUME, MAX_VOLUME)
            for (m in music.values()) m.volume = musicVolume
        }

    private val soundQueue = OrderedSet<SoundRequest>()
    private val playingSoundsMap = ObjectMap<SoundAsset, Array<SoundEntry>>()

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
            val sound = sounds.get(source)

            val allowOverlap = request.allowOverlap ?: source.defaultAllowOverlap
            if (!allowOverlap) {
                playingSoundsMap.get(source)?.forEach { e -> sound.stop(e.id) }
                playingSoundsMap.remove(source)
            }

            val id = if (request.loop) sound.loop(soundVolume) else sound.play(soundVolume)

            var entries = playingSoundsMap.get(source)
            if (entries == null) {
                entries = Array()
                playingSoundsMap.put(source, entries)
            }
            entries.add(SoundEntry(id, request.loop))
        }
        soundQueue.clear()

        val mapIter = playingSoundsMap.iterator()
        while (mapIter.hasNext()) {
            val mapEntry = mapIter.next()
            val ass = mapEntry.key
            val sound = sounds[ass]
            val entries = mapEntry.value

            val entryIter = entries.iterator()
            while (entryIter.hasNext()) {
                val e = entryIter.next()
                if (e.loop) continue

                e.time += delta
                if (e.time >= ass.seconds) {
                    sound.stop(e.id)
                    entryIter.remove()
                }
            }

            if (entries.size == 0) mapIter.remove()
        }

        if (!musicPaused && fadeOutMusicTimer != null) {
            fadeOutMusicTimer!!.update(delta)

            currentMusic?.volume = musicVolume - (musicVolume * fadeOutMusicTimer!!.getRatio())

            if (fadeOutMusicTimer!!.isFinished()) {
                fadeOutMusicTimer = null

                stopMusic()

                currentMusic = null
                currentMusicAss = null
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

    override fun stopSound(key: Any?) {
        val ass = key as SoundAsset
        val sound = sounds.get(ass)
        playingSoundsMap.get(ass)?.forEach { e -> sound.stop(e.id) }
        playingSoundsMap.remove(ass)
    }

    override fun pauseSound(key: Any?) = sounds.get(key as SoundAsset).pause()

    fun stopAllLoopingSounds() {
        val mapIter = playingSoundsMap.iterator()
        while (mapIter.hasNext()) {
            val mapEntry = mapIter.next()
            val sound = sounds[mapEntry.key]
            val entries = mapEntry.value

            val entryIter = entries.iterator()
            while (entryIter.hasNext()) {
                val e = entryIter.next()
                if (e.loop) {
                    sound.stop(e.id)
                    entryIter.remove()
                }
            }

            if (entries.size == 0) mapIter.remove()
        }
    }

    fun pauseAllSound() = sounds.values().forEach { it.pause() }

    fun resumeAllSound() = sounds.values().forEach { it.resume() }

    fun isSoundPlaying(sound: SoundAsset) = (playingSoundsMap.get(sound)?.size ?: 0) > 0

    fun fadeOutMusic(time: Float, onFinished: (() -> Unit)? = null) {
        fadeOutMusicTimer = Timer(time)
        fadeOutMusicTimer!!.setRunOnJustFinished {
            currentMusic?.volume = musicVolume
            onFinished?.invoke()
        }
    }
}
