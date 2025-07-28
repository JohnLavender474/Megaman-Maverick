package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setEntity

open class IceBlock(game: MegamanMaverickGame) : Block(game) {

    companion object {
        const val TAG = "IceBlock"
    }

    private lateinit var ice: Fixture

    override fun init() {
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
