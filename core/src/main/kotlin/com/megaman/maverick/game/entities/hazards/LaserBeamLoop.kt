package com.megaman.maverick.game.entities.hazards

import com.maverick.game.engine.common.interfaces.Updatable
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.audio.MegaAudioManager

object LaserBeamLoop : Updatable {

    const val KEY = "LaserBeamLoop"
    private val SOUND = SoundAsset.LASER_LOOP_SOUND

    private var users = 0
    private var requested = false
    private var audio: MegaAudioManager? = null

    fun join(services: MegaServices) {
        users++
        audio = services.audio
        if (!services.updatables.containsKey(KEY)) services.updatables.put(KEY, this)
    }

    fun leave(services: MegaServices) {
        check(users > 0) { "$KEY: leave() without a join()" }
        users--
        if (users > 0) return
        services.updatables.remove(KEY)
        if (services.audio.isSoundPlaying(SOUND)) services.audio.stopSound(SOUND)
        requested = false
    }

    fun request() {
        requested = true
    }

    override fun update(delta: Float) {
        val audio = audio!!
        val playing = audio.isSoundPlaying(SOUND)
        if (requested && !playing) audio.playSound(SOUND, loop = true)
        if (!requested && playing) audio.stopSound(SOUND)
        requested = false
    }
}
