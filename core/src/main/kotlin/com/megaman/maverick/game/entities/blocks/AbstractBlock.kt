package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.world.body.FixtureLabel

class AbstractBlock(game: MegamanMaverickGame) : Block(game) {

    companion object {
        const val TAG = "AbstractBlock"
        private val FIXTURE_LABELS = objectSetOf(
            FixtureLabel.NO_BODY_TOUCHIE,
            FixtureLabel.NO_SIDE_TOUCHIE,
            FixtureLabel.NO_FEET_TOUCHIE,
            FixtureLabel.NO_PROJECTILE_COLLISION
        )
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.FRICTION_X, 0f)
        spawnProps.put(ConstKeys.FRICTION_Y, 0f)
        spawnProps.put(ConstKeys.FIXTURE_LABELS, FIXTURE_LABELS)
        spawnProps.put(ConstKeys.BLOCK_FILTERS, objectSetOf(MoonScythe.TAG))
        spawnProps.put("${ConstKeys.FEET}_${ConstKeys.SOUND}", false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }
}
