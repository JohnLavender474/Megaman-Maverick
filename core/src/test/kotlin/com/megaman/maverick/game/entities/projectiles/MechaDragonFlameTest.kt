package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.BossRig
import com.megaman.maverick.game.entities.bosses.MechaDragon
import com.megaman.maverick.game.entities.state.MapObjectComponent
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import com.megaman.maverick.game.utils.extensions.getCenter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val PPM = ConstVals.PPM.toFloat()
private const val ARENA_BLOCK_ID = 456

class MechaDragonFlameTest : DescribeSpec({

    lateinit var rig: BossRig

    beforeEach { rig = BossRig() }

    afterEach { rig.dispose() }

    fun spawnFlame(): MechaDragonFlame {
        val flame = MegaEntityFactory.fetch(MechaDragonFlame::class)!!
        flame.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(5f * PPM, 5f * PPM),
                ConstKeys.TRAJECTORY pairTo Vector2(11f * PPM, 0f),
                ConstKeys.BLOCKS pairTo objectSetOf(ARENA_BLOCK_ID)
            )
        )
        rig.processSpawns()
        return flame
    }

    fun blockFixture(id: Int?): IFixture {
        val block = mockk<IGameEntity>(relaxed = true)
        every { block.getComponent(MapObjectComponent::class) } returns id?.let { MapObjectComponent(it) }
        val fixture = mockk<IFixture>()
        every { fixture.getEntity() } returns block
        return fixture
    }

    describe("the flame puff of the breath") {
        it("flies straight at its trajectory and lives 0.5 s") {
            val flame = spawnFlame()
            rig.ticks(0.45f)
            rig.destroyed.contains(flame, true) shouldBe false
            val body = flame.getComponent(BodyComponent::class)!!.body
            (body.getCenter().x - 5f * PPM) shouldBe (11f * PPM * rig.time plusOrMinus 0.5f * PPM)
            body.getCenter().y shouldBe (5f * PPM plusOrMinus 0.01f)
            rig.ticks(0.1f)
            rig.destroyed.contains(flame, true) shouldBe true
        }

        it("dies on an arena block and passes a block that is not one of the arena") {
            val flame = spawnFlame()
            flame.hitBlock(blockFixture(ARENA_BLOCK_ID + 1), mockk<IGameShape2D>(), mockk<IGameShape2D>())
            rig.destroyed.contains(flame, true) shouldBe false
            flame.hitBlock(blockFixture(ARENA_BLOCK_ID), mockk<IGameShape2D>(), mockk<IGameShape2D>())
            rig.destroyed.contains(flame, true) shouldBe true
        }

        it("dies with no explosion when a death fixture touches it") {
            val flame = spawnFlame()
            flame.explodeAndDie()
            rig.destroyed.contains(flame, true) shouldBe true
            rig.queued.isEmpty shouldBe true
        }
    }

    describe("the damage of the dragon fight to the player") {
        it("is 3 from the dragon body, legs and head, and 3 from a flame puff") {
            val dragon = MechaDragon(rig.services)
            dragon.init()
            val negotiator = PlayerDamageNegotiator(rig.player)
            negotiator.get(dragon) shouldBe 3
            negotiator.get(spawnFlame()) shouldBe 3
        }
    }
})
