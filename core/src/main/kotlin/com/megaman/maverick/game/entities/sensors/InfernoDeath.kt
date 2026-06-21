package com.megaman.maverick.game.entities.sensors

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.lessThan
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.getBounds

class InfernoDeath(game: MegamanMaverickGame) : Death(game), Updatable {

    companion object {
        const val TAG = "InfernoDeath"
        private const val MAX_MEGA_MAN_DIST = 10f
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false
        if (game.state.isLevelDefeated(LevelDefinition.INFERNO_MAN)) return true
        return !game.state.isLevelDefeated(LevelDefinition.GLACIER_MAN)
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.INSTANT, true)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }

    override fun update(delta: Float) {
        // If the distance between Mega Man and this Death is too far, then
        // set the Death's body and fixtures to inactive as an optimization.
        // This assumes that ONLY Mega Man can make contact with this Death.
        val active = body.getBounds().getCenter()
            .dst(megaman.body.getBounds().getCenter())
            .lessThan(MAX_MEGA_MAN_DIST * ConstVals.PPM)
        body.physics.collisionOn = active
        body.forEachFixture { fixture -> fixture.setActive(active) }
    }

    override fun getTag() = TAG
}
