package com.megaman.maverick.game.screens.menus

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.input.InputFrameBuilder
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.MegaPlayers
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class TestMegaMenuScreen(game: MegamanMaverickGame) : MegaMenuScreen(game) {
    fun navigation() = getNavigationDirection()
    fun selection() = selectionRequested()
}

class MegaMenuScreenTest : DescribeSpec({

    lateinit var players: MegaPlayers
    lateinit var builder: InputFrameBuilder
    lateinit var screen: TestMegaMenuScreen

    beforeEach {
        players = MegaPlayers()
        builder = InputFrameBuilder(MegaControllerButton.entries)
        val game = mockk<MegamanMaverickGame>(relaxed = true)
        every { game.players } returns players
        screen = TestMegaMenuScreen(game)
    }

    fun bits(vararg buttons: MegaControllerButton) = buttons.fold(0L) { held, button -> held or (1L shl button.index) }

    fun build(held: Long) {
        players.inputs[PlayerSlot(0)] = builder.build(held)
    }

    fun justPressed(vararg buttons: MegaControllerButton) = build(bits(*buttons))

    fun pressed(vararg buttons: MegaControllerButton) {
        build(bits(*buttons))
        build(bits(*buttons))
    }

    fun justReleased(vararg buttons: MegaControllerButton) {
        build(bits(*buttons))
        build(0L)
    }

    val dpad = listOf(
        MegaControllerButton.UP to Direction.UP,
        MegaControllerButton.DOWN to Direction.DOWN,
        MegaControllerButton.LEFT to Direction.LEFT,
        MegaControllerButton.RIGHT to Direction.RIGHT,
    )

    describe("MegaMenuScreen.getNavigationDirection") {
        it("gives the direction of each d-pad button that is just released") {
            dpad.forEach { (button, direction) ->
                justReleased(button)

                screen.navigation() shouldBe direction
            }
        }

        it("gives the first of UP, DOWN, LEFT and RIGHT for 2 releases in one frame") {
            for (first in dpad.indices) for (second in first + 1 until dpad.size) {
                justReleased(dpad[first].first, dpad[second].first)

                screen.navigation() shouldBe dpad[first].second
            }
        }

        it("gives null for a d-pad button that is just pressed or pressed") {
            dpad.forEach { (button, _) ->
                justPressed(button)
                screen.navigation() shouldBe null

                pressed(button)
                screen.navigation() shouldBe null

                build(0L)
                build(0L)
            }
        }
    }

    describe("MegaMenuScreen.selectionRequested") {
        it("is true for a release of START and for a release of A") {
            justReleased(MegaControllerButton.START)
            screen.selection() shouldBe true

            justReleased(MegaControllerButton.A)
            screen.selection() shouldBe true
        }

        it("is false for a release of B or SELECT and for a press of START") {
            justReleased(MegaControllerButton.B)
            screen.selection() shouldBe false

            justReleased(MegaControllerButton.SELECT)
            screen.selection() shouldBe false

            build(0L)
            justPressed(MegaControllerButton.START)
            screen.selection() shouldBe false
        }
    }
})
