package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestBodyPlayer
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f

private class CartRig(slots: Int) {
    val players = MegaPlayers(slots)
    val gravity = WorldGravity(players)
    val world = SimpleGridWorldContainer(ConstVals.PPM)
    val engine = GameEngine(Array<GameSystem>().apply {
        add(UpdatablesSystem())
        add(
            WorldSystem(
                ppm = ConstVals.PPM,
                fixedStep = STEP,
                worldContainerSupplier = { world },
                contactListener = ContactRuleDispatcher(
                    megaContactRules(players, gravity, GameMode.COOP.rules) { _, _ -> false }
                ),
                collisionHandler = MegaCollisionHandler(),
                contactFilter = MegaContactFilter()
            )
        )
    })
    val services = testServices(engine = engine, players = players, gravity = gravity)
    val cart: CartV2

    init {
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas

        Block(services).spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(-10f * PPM, -PPM, 40f * PPM, PPM)))
        cart = CartV2(services)
        cart.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(2f * PPM, 0f, 2f * PPM, PPM),
                ConstKeys.FACING pairTo Facing.RIGHT.name
            )
        ) shouldBe true
        repeat(30) { engine.update(STEP) }
        cart.body.physics.velocity.x = 0f
    }

    fun player(slot: Int, onCart: Boolean): TestBodyPlayer {
        val player = TestBodyPlayer(services)
        player.body.putProperty(ConstKeys.ENTITY, player)
        player.body.putProperty(ConstKeys.FEET, Fixture(player.body, FixtureType.FEET, GameRectangle().setSize(16f, player.body.getHeight() + 8f)))
        val x = if (onCart) cart.body.getX() + cart.body.getWidth() / 2f else 30f * PPM
        player.body.setCenter(x, cart.body.getMaxY() + player.body.getHeight() / 2f)
        players.register(PlayerSlot(slot), player)
        engine.spawn(player, props()) shouldBe true
        engine.update(STEP)
        player.existenceState.liveNow shouldBe true
        return player
    }

    fun kill(player: TestBodyPlayer) {
        player.destroy()
        engine.update(STEP)
        player.existenceState.liveNow shouldBe false
    }

    fun cartSpeedAfter(frames: Int): Float {
        repeat(frames) { engine.update(STEP) }
        return cart.body.physics.velocity.x
    }
}

class CartV2DeadPlayerTest : DescribeSpec({

    describe("a CartV2 with a player's feet on it") {
        it("moves while that player lives") {
            val rig = CartRig(2)
            rig.player(0, onCart = false)
            rig.player(1, onCart = true)
            rig.cartSpeedAfter(10) shouldBeGreaterThan 0f
        }

        it("does not move for a dead player's body while the other player lives") {
            val rig = CartRig(2)
            rig.player(0, onCart = false)
            rig.kill(rig.player(1, onCart = true))
            rig.cartSpeedAfter(10) shouldBe 0f
        }

        it("moves for the one player of a single player game, also when it is not live, as before") {
            val rig = CartRig(1)
            rig.kill(rig.player(0, onCart = true))
            rig.cartSpeedAfter(10) shouldBeGreaterThan 0f
        }
    }

})
