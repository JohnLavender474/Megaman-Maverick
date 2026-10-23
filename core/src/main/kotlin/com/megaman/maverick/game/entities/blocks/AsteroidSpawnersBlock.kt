package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.setReceiver

class AsteroidSpawnersBlock(game: MegamanMaverickGame) : Block(game) {

    companion object {
        const val TAG = "AsteroidSpawnersBlock"
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(
            ConstKeys.FIXTURE_LABELS, objectSetOf(
                FixtureLabel.NO_PROJECTILE_COLLISION, FixtureLabel.NO_SIDE_TOUCHIE
            )
        )
        spawnProps.put(ConstKeys.BLOCK_FILTERS, objectSetOf(Asteroid.TAG))
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        blockFixture.setReceiver(Receivers.NO_PROJECTILE_COLLISION_EXCEPTION) { projectile, _ ->
            isExceptionForNoProjectileCollision(projectile)
        }
    }

    private fun isExceptionForNoProjectileCollision(projectile: IProjectileEntity) = projectile is MoonScythe
}
