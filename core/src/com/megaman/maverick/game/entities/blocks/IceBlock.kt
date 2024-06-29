package com.megaman.maverick.game.entities.blocks

import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.world.Fixture
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.setEntity

open class IceBlock(game: MegamanMaverickGame) : Block(game) {

    companion object {
        const val TAG = "IceBlock"
    }

    private lateinit var ice: Fixture

    override fun init() {
        super.init()
        ice = Fixture(body, FixtureType.ICE, GameRectangle())
        ice.setEntity(this)
        body.addFixture(ice)
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        (ice.rawShape as GameRectangle).set(body)
    }
}
