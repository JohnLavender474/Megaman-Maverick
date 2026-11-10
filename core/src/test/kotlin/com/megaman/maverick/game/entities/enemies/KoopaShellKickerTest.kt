package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class ShellRig(slots: Int) {
    val players = MegaPlayers(slots)
    val engine = GameEngine(Array<GameSystem>().apply { add(UpdatablesSystem()) })
    val services = testServices(engine = engine, players = players)

    init {
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
    }

    fun shell(make: (MegaServices) -> AbstractEnemy): AbstractEnemy {
        val shell = make(services)
        engine.spawn(
            shell, props(ConstKeys.POSITION pairTo Vector2(0f, 0f), ConstKeys.CULL_OUT_OF_BOUNDS pairTo false)
        ) shouldBe true
        engine.update(0f)
        return shell
    }

    fun player(slot: Int, width: Float, left: Float, bottom: Float): TestHealthPlayer {
        val player = TestHealthPlayer(services)
        player.body.setSize(width, 48f).setPosition(left, bottom)
        for (key in arrayOf("${ConstKeys.LEFT}_${ConstKeys.SIDE}", "${ConstKeys.RIGHT}_${ConstKeys.SIDE}", ConstKeys.FEET))
            player.body.putProperty(key, Fixture(player.body, FixtureType.SIDE, GameRectangle().setSize(1f, 1f)))
        players.register(PlayerSlot(slot), player)
        engine.spawn(player, props()) shouldBe true
        return player
    }
}

class KoopaShellKickerTest : DescribeSpec({

    val rigs = ArrayList<ShellRig>()

    afterEach {
        rigs.forEach { it.engine.reset() }
        rigs.clear()
        MegaEntityFactory.reset()
    }

    for ((name, make) in listOf<Pair<String, (MegaServices) -> AbstractEnemy>>(
        "GreenKoopaShell" to { s -> GreenKoopaShell(s) },
        "RedKoopaShell" to { s -> RedKoopaShell(s) }
    )) {
        describe("a $name kicked with 2 players") {
            it("rolls away from the kicker on the left, while the other player stands nearer on the right") {
                val rig = ShellRig(2).also { rigs.add(it) }
                val shell = rig.shell(make)
                val bounds = shell.body.getBounds(GameRectangle())
                rig.player(0, 64f, bounds.getX() - 62f, bounds.getY())
                rig.player(1, 16f, bounds.getMaxX() + 1f, bounds.getY())
                rig.engine.update(0.01f)
                (shell as IFaceable).facing shouldBe Facing.RIGHT
                rig.engine.update(0.01f)
                shell.body.physics.velocity.x * 1f shouldBeGreaterThan 0f
            }

            it("rolls away from the kicker on the right, while the other player stands nearer on the left") {
                val rig = ShellRig(2).also { rigs.add(it) }
                val shell = rig.shell(make)
                val bounds = shell.body.getBounds(GameRectangle())
                rig.player(0, 16f, bounds.getX() - 17f, bounds.getY())
                rig.player(1, 64f, bounds.getMaxX() - 2f, bounds.getY())
                rig.engine.update(0.01f)
                (shell as IFaceable).facing shouldBe Facing.LEFT
                rig.engine.update(0.01f)
                shell.body.physics.velocity.x * -1f shouldBeGreaterThan 0f
            }
        }

        describe("a $name kicked with 1 player") {
            it("rolls away from that player, as before") {
                val rig = ShellRig(1).also { rigs.add(it) }
                val shell = rig.shell(make)
                val bounds = shell.body.getBounds(GameRectangle())
                rig.player(0, 32f, bounds.getMaxX() - 2f, bounds.getY())
                rig.engine.update(0.01f)
                (shell as IFaceable).facing shouldBe Facing.LEFT
                rig.engine.update(0.01f)
                shell.body.physics.velocity.x * -1f shouldBeGreaterThan 0f
            }
        }
    }
})
