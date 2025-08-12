package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

class FireballBlock(game: MegamanMaverickGame): AnimatedBlock(game) {

    companion object {
        const val TAG = "FireballBlock"
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ANIMATION, TAG)
        super.onSpawn(spawnProps)
    }
}
