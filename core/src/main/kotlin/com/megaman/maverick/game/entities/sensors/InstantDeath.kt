package com.megaman.maverick.game.entities.sensors

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices

class InstantDeath(services: MegaServices): Death(services) {

    companion object {
        const val TAG = "InstantDeath"
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.INSTANT, true)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }
}
