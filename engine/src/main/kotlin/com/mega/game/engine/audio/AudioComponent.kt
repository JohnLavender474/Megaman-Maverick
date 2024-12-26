package com.mega.game.engine.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.components.IGameComponent

data class SoundRequest(val source: Any, val loop: Boolean = false)

data class MusicRequest(
    val source: Any, val loop: Boolean = true, val onCompletionListener: ((Music) -> Unit)? = null
)

class AudioComponent : IGameComponent {

    val playSoundRequests = Array<SoundRequest>()
    val stopSoundRequests = Array<Any>()

    val playMusicRequests = Array<MusicRequest>()
    val stopMusicRequests = Array<Any>()

    fun requestToPlaySound(source: Any, loop: Boolean) = playSoundRequests.add(SoundRequest(source, loop))

    fun requestToPlayMusic(
        source: Any, loop: Boolean = true, onCompletionListener: ((Music) -> Unit)? = null
    ) = playMusicRequests.add(MusicRequest(source, loop, onCompletionListener))

    fun requestToPlayMusic(
        source: Any, loop: Boolean = true, onCompletionListener: Runnable? = null
    ) = playMusicRequests.add(MusicRequest(source, loop) { onCompletionListener?.run() })

    override fun reset() {
        playSoundRequests.clear()
        stopSoundRequests.clear()
        playMusicRequests.clear()
        stopMusicRequests.clear()
    }
}
