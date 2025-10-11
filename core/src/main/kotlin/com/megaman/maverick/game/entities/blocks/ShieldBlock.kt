package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setEntity

open class ShieldBlock(game: MegamanMaverickGame): Block(game) {

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        val shieldBounds = GameRectangle()
        val shieldFixture = Fixture(body, FixtureType.SHIELD, shieldBounds)
        // The "full" property signals that the shield fixture covers the full body
        // of this block.
        shieldFixture.putProperty(ConstKeys.FULL, true)
        shieldFixture.setEntity(this)
        body.addFixture(shieldFixture)

        // The "shield" property signals that this block has a shield which covers
        // the full body.
        body.putProperty(ConstKeys.SHIELD, true)

        body.preProcess.put(ConstKeys.SHIELD) { shieldBounds.set(body) }

        return component
    }
}
