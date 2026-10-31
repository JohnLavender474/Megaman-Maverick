package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.PlayerComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.CrusherComponent
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.getBlockFilters
import com.megaman.maverick.game.world.body.hasFixtureLabel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CameraEdgeBlockTest : DescribeSpec({

    fun spawned(): CameraEdgeBlock {
        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        val block = CameraEdgeBlock(services)
        block.init()
        block.onSpawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle().setSize(32f, 512f),
                ConstKeys.DIRECTION pairTo Direction.LEFT
            )
        )
        return block
    }

    describe("CameraEdgeBlock") {
        it("lets an enemy pass, and holds a player") {
            val block = spawned()
            val filters = block.body.getBlockFilters().shouldNotBeNull()
            val enemy = mockk<AbstractEnemy>(relaxed = true)
            filters.any { it.invoke(enemy, block) } shouldBe true
            val player = mockk<MegaGameEntity>(relaxed = true)
            every { player.hasComponent(PlayerComponent::class) } returns true
            filters.any { it.invoke(player, block) } shouldBe false
        }

        it("takes the flags of an abstract block") {
            val block = spawned()
            block.blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE) shouldBe true
            block.blockFixture.hasFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION) shouldBe true
        }

        it("is a crusher") {
            spawned().getComponent(CrusherComponent::class).shouldNotBeNull()
        }
    }
})
