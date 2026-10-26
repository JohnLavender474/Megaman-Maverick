package com.megaman.maverick.game.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.maverick.game.engine.controller.buttons.ControllerButton
import com.maverick.game.engine.controller.buttons.ControllerButtons
import com.megaman.maverick.game.controllers.MegaControllerButton
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class KeyboardInputTest : DescribeSpec({

    val keysDown = HashSet<Int>()

    beforeEach {
        keysDown.clear()
        Gdx.input = mockk<Input>(relaxed = true) {
            every { isKeyPressed(any()) } answers { firstArg<Int>() in keysDown }
        }
    }

    fun defaultButtons() = ControllerButtons().also { buttons ->
        MegaControllerButton.entries.forEach { buttons.put(it, ControllerButton(it.defaultKeyboardKey)) }
    }

    describe("KeyboardInput") {
        it("sets the bit of each button whose key is down") {
            keysDown.add(MegaControllerButton.A.defaultKeyboardKey)
            keysDown.add(MegaControllerButton.LEFT.defaultKeyboardKey)

            KeyboardInput(defaultButtons()).held() shouldBe
                ((1L shl MegaControllerButton.A.index) or (1L shl MegaControllerButton.LEFT.index))
        }

        it("gives no bits when no bound key is down") {
            keysDown.add(Input.Keys.P)

            KeyboardInput(defaultButtons()).held() shouldBe 0L
        }

        it("reads the current code of each button") {
            val buttons = defaultButtons()
            val input = KeyboardInput(buttons)
            buttons.get(MegaControllerButton.B).keyboardCode = Input.Keys.Z
            keysDown.add(Input.Keys.Z)

            input.held() shouldBe (1L shl MegaControllerButton.B.index)
        }
    }
})
