package com.megaman.maverick.game.entities.sensors

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

class InstantDeath(game: MegamanMaverickGame): Death(game) {

    companion object {
        const val TAG = "InstantDeath"
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.INSTANT, true)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }
}
