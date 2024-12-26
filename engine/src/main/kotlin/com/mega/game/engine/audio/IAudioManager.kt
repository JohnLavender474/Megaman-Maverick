package com.mega.game.engine.audio

interface IAudioManager {

    var soundVolume: Float
    var musicVolume: Float

    fun playMusic(key: Any? = null, loop: Boolean = false)

    fun stopMusic(key: Any? = null)

    fun pauseMusic(key: Any? = null)

    fun playSound(key: Any? = null, loop: Boolean = false)

    fun stopSound(key: Any? = null)

    fun pauseSound(key: Any? = null)
}
