package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel

class LadderTop(services: MegaServices) : Block(services) {

    companion object {
        const val TAG = "LadderTop"
        private val FIXTURE_LABELS = objectSetOf(FixtureLabel.NO_PROJECTILE_COLLISION, FixtureLabel.NO_SIDE_TOUCHIE)
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.FRICTION_X, 0f)
        spawnProps.put(ConstKeys.FRICTION_Y, 0f)
        spawnProps.put(ConstKeys.FIXTURE_LABELS, FIXTURE_LABELS)
        spawnProps.put(ConstKeys.BODY_LABELS, objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY))
        spawnProps.put("${ConstKeys.FEET}_${ConstKeys.SOUND}", false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }

    override fun getTag() = TAG
}
