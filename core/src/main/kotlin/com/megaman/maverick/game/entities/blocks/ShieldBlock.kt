package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setEntity

open class ShieldBlock(game: MegamanMaverickGame): Block(game) {

    private lateinit var shieldFixture: IFixture

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        if (spawnProps.containsKey("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}")) {
            val reflectDir = Direction.valueOf(
                spawnProps.get("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}", String::class)!!.uppercase()
            )
            shieldFixture.putProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}", reflectDir)
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        shieldFixture.removeProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}")
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        val shieldBounds = GameRectangle()
        shieldFixture = Fixture(body, FixtureType.SHIELD, shieldBounds)
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
