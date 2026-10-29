package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices

class SMB3_GoldBreakableBlock(game: MegaServices): BreakableBlock(game) {

    companion object {
        const val TAG = "SMB3_GoldBreakableBlock"
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.TYPE, SMB3_GOLD_TYPE)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }
}
