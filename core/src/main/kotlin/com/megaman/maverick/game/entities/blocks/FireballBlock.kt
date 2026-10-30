package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices

class FireballBlock(services: MegaServices): AnimatedBlock(services) {

    companion object {
        const val TAG = "FireballBlock"
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ANIMATION, TAG)
        super.onSpawn(spawnProps)
    }
}
