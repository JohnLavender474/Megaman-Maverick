package com.mega.game.engine.entities.contracts

import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.entities.IGameEntity

interface IAudioEntity : IGameEntity {

    val audioComponent: AudioComponent
        get() {
            val key = AudioComponent::class
            return getComponent(key)!!
        }

    fun requestToPlaySound(source: Any, loop: Boolean, allowOverlap: Boolean = true) {
        this.audioComponent.requestToPlaySound(source, loop, allowOverlap)
    }

    fun stopSound(source: Any?) {
        this.audioComponent.stopSoundRequests.add(source)
    }

    fun stopMusic(source: Any?) {
        this.audioComponent.stopMusicRequests.add(source)
    }
}

