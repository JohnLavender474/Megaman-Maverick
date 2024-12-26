package com.mega.game.engine.entities.contracts

import com.badlogic.gdx.audio.Music
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.entities.IGameEntity

interface IAudioEntity : IGameEntity {

    val audioComponent: AudioComponent
        get() {
            val key = AudioComponent::class
            return getComponent(key)!!
        }

    fun requestToPlaySound(source: Any, loop: Boolean) {
        this.audioComponent.requestToPlaySound(source, loop)
    }

    fun requestToPlayMusic(source: Any, loop: Boolean, onCompletionListener: ((Music) -> Unit)?) {
        this.audioComponent.requestToPlayMusic(source, loop, onCompletionListener)
    }

    fun stopSound(source: Any?) {
        this.audioComponent.stopSoundRequests.add(source)
    }

    fun stopMusic(source: Any?) {
        this.audioComponent.stopMusicRequests.add(source)
    }
}

