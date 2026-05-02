package com.megaman.maverick.game.entities.sensors

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.setFilter

open class DeathForPlayerOnly(game: MegamanMaverickGame) : Death(game) {

    companion object {
        const val TAG = "DeathForPlayerOnly"
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        body.forEachFixture { fixture ->
            fixture.setFilter { other ->
                other.getEntity() == megaman
            }
        }
    }

    override fun getTag() = TAG
}
