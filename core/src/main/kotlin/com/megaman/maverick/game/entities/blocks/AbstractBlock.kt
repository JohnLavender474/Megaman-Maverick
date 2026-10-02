package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.setFilter

open class AbstractBlock(game: MegamanMaverickGame) : Block(game) {

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
        // Tags from the TMX "block_filters" prop (e.g. "Met,SniperJoe") are added to the default filters
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
            fixture.setFilter { other ->
                other.getEntity() == megaman
            }
        }
    }

    override fun getTag() = TAG
}
