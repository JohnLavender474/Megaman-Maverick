package com.megaman.maverick.game.entities.blocks

import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.world.Fixture
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.setEntity

class IceBlock(game: MegamanMaverickGame) : Block(game) {

    private lateinit var ice: Fixture

    override fun init() {
        super.init()
        ice = Fixture(GameRectangle(), FixtureType.ICE)
        ice.setEntity(this)
        body.addFixture(ice)
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        // body.physics.frictionToApply.x = 0f
        (ice.shape as GameRectangle).set(body)
    }
}
