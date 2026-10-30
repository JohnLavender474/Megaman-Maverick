package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setEntity

open class IceBlock(services: MegaServices) : ShieldBlock(services) {

    companion object {
        const val TAG = "IceBlock"
    }

    private lateinit var ice: Fixture

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()

        ice = Fixture(body, FixtureType.ICE, GameRectangle())
        ice.setEntity(this)

        body.addFixture(ice)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        (ice.rawShape as GameRectangle).set(body)
    }
}
