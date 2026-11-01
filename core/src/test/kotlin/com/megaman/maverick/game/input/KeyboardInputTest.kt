package com.megaman.maverick.game.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputFrameBuilder
import com.maverick.game.engine.input.checkInputButtons
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.PlayerSettings
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

    describe("KeyboardInput") {
        it("sets the bit of each button whose key is down") {
            keysDown.add(MegaControllerButton.A.defaultKeyboardKey)
            keysDown.add(MegaControllerButton.LEFT.defaultKeyboardKey)

            KeyboardInput(PlayerSettings()).held() shouldBe
                ((1L shl MegaControllerButton.A.index) or (1L shl MegaControllerButton.LEFT.index))
        }

        it("gives no bits when no bound key is down") {
            keysDown.add(Input.Keys.P)

            KeyboardInput(PlayerSettings()).held() shouldBe 0L
        }

        it("reads the current code of each button") {
            val settings = PlayerSettings()
            val input = KeyboardInput(settings)
            settings.keyCodes[MegaControllerButton.B.index] = Input.Keys.Z
            keysDown.add(Input.Keys.Z)

            input.held() shouldBe (1L shl MegaControllerButton.B.index)
        }

        it("gives START for Enter") {
            keysDown.add(Input.Keys.ENTER)

            KeyboardInput(PlayerSettings()).held() shouldBe (1L shl MegaControllerButton.START.index)
        }

        it("gives START for numpad Enter") {
            keysDown.add(Input.Keys.NUMPAD_ENTER)

            KeyboardInput(PlayerSettings()).held() shouldBe (1L shl MegaControllerButton.START.index)
        }

        it("gives START for Enter and for the new key when START is bound to another key") {
            val settings = PlayerSettings()
            settings.keyCodes[MegaControllerButton.START.index] = Input.Keys.I
            val input = KeyboardInput(settings)

            keysDown.add(Input.Keys.ENTER)
            input.held() shouldBe (1L shl MegaControllerButton.START.index)

            keysDown.clear()
            keysDown.add(Input.Keys.I)
            input.held() shouldBe (1L shl MegaControllerButton.START.index)
        }

        it("gives the d-pad button for each arrow key") {
            val input = KeyboardInput(PlayerSettings())
            listOf(
                Input.Keys.UP to MegaControllerButton.UP,
                Input.Keys.DOWN to MegaControllerButton.DOWN,
                Input.Keys.LEFT to MegaControllerButton.LEFT,
                Input.Keys.RIGHT to MegaControllerButton.RIGHT,
            ).forEach { (key, button) ->
                keysDown.clear()
                keysDown.add(key)

                input.held() shouldBe (1L shl button.index)
            }
        }

        it("gives one UP bit for W and the up arrow together") {
            keysDown.add(Input.Keys.W)
            keysDown.add(Input.Keys.UP)

            KeyboardInput(PlayerSettings()).held() shouldBe (1L shl MegaControllerButton.UP.index)
        }

        it("gives no bit for Esc") {
            keysDown.add(Input.Keys.ESCAPE)

            KeyboardInput(PlayerSettings()).held() shouldBe 0L
        }

        it("keeps UP pressed with no second press when the up arrow joins and then replaces W") {
            val input = KeyboardInput(PlayerSettings())
            val builder = InputFrameBuilder(MegaControllerButton.entries)

            keysDown.add(Input.Keys.W)
            builder.build(input.held()).getStatus(MegaControllerButton.UP) shouldBe ButtonStatus.JUST_PRESSED

            keysDown.add(Input.Keys.UP)
            builder.build(input.held()).getStatus(MegaControllerButton.UP) shouldBe ButtonStatus.PRESSED

            keysDown.remove(Input.Keys.W)
            builder.build(input.held()).getStatus(MegaControllerButton.UP) shouldBe ButtonStatus.PRESSED
        }
    }

    describe("KeyboardInput.systemHeld") {
        it("gives the bit of BACK for Esc") {
            keysDown.add(Input.Keys.ESCAPE)

            KeyboardInput(PlayerSettings()).systemHeld() shouldBe (1L shl SystemButton.BACK.index)
        }

        it("gives 0 for each other key, also the Android BACK key and Enter") {
            Input.Keys.ESCAPE shouldBe 111
            Input.Keys.BACK shouldBe 4

            val input = KeyboardInput(PlayerSettings())
            for (key in 0..255) {
                if (key == Input.Keys.ESCAPE) continue
                keysDown.clear()
                keysDown.add(key)

                input.systemHeld() shouldBe 0L
            }

            keysDown.clear()
            keysDown.add(Input.Keys.BACK)
            input.systemHeld() shouldBe 0L

            keysDown.clear()
            keysDown.add(Input.Keys.ENTER)
            input.systemHeld() shouldBe 0L
        }

        it("keeps Esc and the key of A apart") {
            keysDown.add(Input.Keys.ESCAPE)
            keysDown.add(MegaControllerButton.A.defaultKeyboardKey)
            val input = KeyboardInput(PlayerSettings())

            input.held() shouldBe (1L shl MegaControllerButton.A.index)
            input.systemHeld() shouldBe (1L shl SystemButton.BACK.index)
        }

        it("gives 1 press edge for a held Esc and 1 release edge, through a real builder") {
            val input = KeyboardInput(PlayerSettings())
            val builder = InputFrameBuilder(SystemButton.entries)

            keysDown.add(Input.Keys.ESCAPE)
            builder.build(input.systemHeld()).getStatus(SystemButton.BACK) shouldBe ButtonStatus.JUST_PRESSED
            builder.build(input.systemHeld()).getStatus(SystemButton.BACK) shouldBe ButtonStatus.PRESSED
            builder.build(input.systemHeld()).getStatus(SystemButton.BACK) shouldBe ButtonStatus.PRESSED

            keysDown.remove(Input.Keys.ESCAPE)
            builder.build(input.systemHeld()).getStatus(SystemButton.BACK) shouldBe ButtonStatus.JUST_RELEASED
            builder.build(input.systemHeld()).getStatus(SystemButton.BACK) shouldBe ButtonStatus.RELEASED
        }

        it("gives a press of Esc in the system frame while a lock releases the slot frame") {
            val input = KeyboardInput(PlayerSettings())
            val slotBuilder = InputFrameBuilder(MegaControllerButton.entries)
            val systemBuilder = InputFrameBuilder(SystemButton.entries)

            keysDown.add(MegaControllerButton.A.defaultKeyboardKey)
            slotBuilder.build(input.held())

            slotBuilder.locked = true
            slotBuilder.build(input.held())
            keysDown.add(Input.Keys.ESCAPE)

            slotBuilder.build(input.held()).getStatus(MegaControllerButton.A) shouldBe ButtonStatus.RELEASED
            systemBuilder.build(input.systemHeld()).getStatus(SystemButton.BACK) shouldBe ButtonStatus.JUST_PRESSED
        }
    }

    describe("SystemButton") {
        it("has 1 entry, BACK, and passes the button check") {
            SystemButton.entries shouldBe listOf(SystemButton.BACK)

            checkInputButtons(SystemButton.entries)
        }
    }

    describe("KeyboardInput.isReservedKey") {
        it("is true for Esc, Enter, numpad Enter and the 4 arrow keys") {
            listOf(
                Input.Keys.ESCAPE,
                Input.Keys.ENTER,
                Input.Keys.NUMPAD_ENTER,
                Input.Keys.UP,
                Input.Keys.DOWN,
                Input.Keys.LEFT,
                Input.Keys.RIGHT,
            ).forEach { KeyboardInput.isReservedKey(it) shouldBe true }
        }

        it("is false for W, K and P") {
            listOf(Input.Keys.W, Input.Keys.K, Input.Keys.P).forEach { KeyboardInput.isReservedKey(it) shouldBe false }
        }

        it("is true for each key of the alias table") {
            val settings = PlayerSettings()
            settings.keyCodes.fill(-2)
            val input = KeyboardInput(settings)
            var aliases = 0

            for (key in 0..255) {
                keysDown.clear()
                keysDown.add(key)
                if (input.held() != 0L) {
                    aliases++
                    KeyboardInput.isReservedKey(key) shouldBe true
                }
            }

            aliases shouldBe 6
        }
    }
})
