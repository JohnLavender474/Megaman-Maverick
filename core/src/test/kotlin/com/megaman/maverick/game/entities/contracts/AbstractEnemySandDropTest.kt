package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.state.SandReaction
import com.megaman.maverick.game.entities.state.SandReactionComponent
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private class TestEnemy(services: MegaServices) : AbstractEnemy(services) {

    val dropsItem get() = dropItemOnDeath

    override fun defineBodyComponent(): BodyComponent = BodyComponentCreator.create(this, Body(BodyType.DYNAMIC))

    override fun defineSpritesComponent() = SpritesComponent()
}

class AbstractEnemySandDropTest : DescribeSpec({

    fun enemy(onSand: Boolean): TestEnemy {
        val enemy = TestEnemy(testServices(engine = mockk<GameEngine>(relaxed = true)))
        enemy.init()
        enemy.body.setBodySense(BodySense.FEET_ON_SAND, onSand)
        return enemy
    }

    describe("the item drop of an enemy when its destroy is queued") {
        it("turns off for a sinking enemy with feet on sand, and stays off after the body senses reset") {
            val enemy = enemy(onSand = true)

            enemy.onDestroyQueued()
            enemy.body.reset()

            enemy.dropsItem shouldBe false
        }

        it("stays on for an enemy that is not on sand") {
            val enemy = enemy(onSand = false)

            enemy.onDestroyQueued()

            enemy.dropsItem shouldBe true
        }

        it("stays on for an enemy that stays at the surface, as DarkSerket does") {
            val enemy = enemy(onSand = true)
            enemy.addComponent(SandReactionComponent(SandReaction.SURFACE))

            enemy.onDestroyQueued()

            enemy.dropsItem shouldBe true
        }
    }
})
