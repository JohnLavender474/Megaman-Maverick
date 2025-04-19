package com.megaman.maverick.game.entities.special

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

// I was kinda dumb when writing the code for PreciousGemCluster, and I ended up writing a lot of the logic so that it
// is tightly coupled to Precious Woman. Rather than try to refactor that class to be more reusable, instead I've opted
// to write this new class which will be used by Megaman.
class MegaPreciousCluster(game: MegamanMaverickGame): MegaGameEntity(game) {

    companion object {
        const val TAG = "PreciousCluster"
    }

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
