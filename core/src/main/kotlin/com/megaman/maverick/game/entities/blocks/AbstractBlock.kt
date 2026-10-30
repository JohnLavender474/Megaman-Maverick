package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.setReceiver

open class AbstractBlock(services: MegaServices) : Block(services) {

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
        val blockFilters = objectSetOf(MoonScythe.TAG, SmallIceCube.TAG)
        (spawnProps.get(ConstKeys.BLOCK_FILTERS) as? String)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.forEach { blockFilters.add(it) }
        spawnProps.put(ConstKeys.BLOCK_FILTERS, blockFilters)
        spawnProps.put("${ConstKeys.FEET}_${ConstKeys.SOUND}", false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        body.forEachFixture { fixture ->
            fixture.setReceiver(Receivers.FILTER) { other ->
                other.getEntity().asPlayer != null
            }
        }
    }

    override fun getTag() = TAG
}
