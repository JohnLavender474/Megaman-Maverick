package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel

class CollideDownOnlyBlock(services: MegaServices): Block(services) {

    companion object {
        const val TAG = "CollideDownOnlyBlock"

        private val BODY_LABELS = objectSetOf(
            BodyLabel.COLLIDE_DOWN_ONLY
        )

        private val FIXTURE_LABELS = objectSetOf(
            FixtureLabel.NO_SIDE_TOUCHIE,
            FixtureLabel.NO_PROJECTILE_COLLISION,
        )
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.BODY_LABELS, BODY_LABELS)
        spawnProps.put(ConstKeys.FIXTURE_LABELS, FIXTURE_LABELS)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }
}
