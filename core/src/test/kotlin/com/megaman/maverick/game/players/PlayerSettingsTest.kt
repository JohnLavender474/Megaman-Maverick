package com.megaman.maverick.game.players

import com.badlogic.gdx.Input
import com.megaman.maverick.game.controllers.MegaControllerButton
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerSettingsTest : DescribeSpec({

    val reservedKeys = listOf(
        Input.Keys.ESCAPE,
        Input.Keys.ENTER,
        Input.Keys.NUMPAD_ENTER,
        Input.Keys.UP,
        Input.Keys.DOWN,
        Input.Keys.LEFT,
        Input.Keys.RIGHT,
    )

    fun PlayerSettings.keyOf(button: MegaControllerButton) = keyCodes[button.index]

    fun PlayerSettings.setKey(button: MegaControllerButton, keycode: Int) {
        keyCodes[button.index] = keycode
    }

    describe("captureKey") {
        it("refuses each reserved key for A and changes no code") {
            val settings = PlayerSettings()
            val codes = settings.keyCodes.toList()

            reservedKeys.forEach {
                settings.captureKey(MegaControllerButton.A, it) shouldBe KeyCapture.REFUSE
                settings.keyCodes.toList() shouldBe codes
            }
        }

        it("accepts a free key for A and changes only A") {
            val settings = PlayerSettings()
            val expected = settings.keyCodes.toList().toMutableList()
            expected[MegaControllerButton.A.index] = Input.Keys.I

            settings.captureKey(MegaControllerButton.A, Input.Keys.I) shouldBe KeyCapture.ACCEPT

            settings.keyCodes.toList() shouldBe expected
        }

        it("accepts the key of B for A, with the swap") {
            val settings = PlayerSettings()

            settings.captureKey(MegaControllerButton.A, Input.Keys.J) shouldBe KeyCapture.ACCEPT

            settings.keyOf(MegaControllerButton.A) shouldBe Input.Keys.J
            settings.keyOf(MegaControllerButton.B) shouldBe Input.Keys.K
        }

        it("keeps the key that A already has") {
            val settings = PlayerSettings()
            val codes = settings.keyCodes.toList()

            settings.captureKey(MegaControllerButton.A, Input.Keys.K) shouldBe KeyCapture.KEEP

            settings.keyCodes.toList() shouldBe codes
        }

        it("keeps Enter for START on Enter, and does not refuse it") {
            val settings = PlayerSettings()

            settings.captureKey(MegaControllerButton.START, Input.Keys.ENTER) shouldBe KeyCapture.KEEP
        }

        it("refuses numpad Enter for START on Enter") {
            val settings = PlayerSettings()

            settings.captureKey(MegaControllerButton.START, Input.Keys.NUMPAD_ENTER) shouldBe KeyCapture.REFUSE
            settings.keyOf(MegaControllerButton.START) shouldBe Input.Keys.ENTER
        }

        it("refuses Enter for START on I") {
            val settings = PlayerSettings()
            settings.setKey(MegaControllerButton.START, Input.Keys.I)

            settings.captureKey(MegaControllerButton.START, Input.Keys.ENTER) shouldBe KeyCapture.REFUSE
            settings.keyOf(MegaControllerButton.START) shouldBe Input.Keys.I
        }

        it("accepts a free key for START on Enter, and no other button gets Enter") {
            val settings = PlayerSettings()

            settings.captureKey(MegaControllerButton.START, Input.Keys.I) shouldBe KeyCapture.ACCEPT

            settings.keyOf(MegaControllerButton.START) shouldBe Input.Keys.I
            settings.keyCodes.contains(Input.Keys.ENTER) shouldBe false
        }

        it("accepts the key of A for START on I, with the swap") {
            val settings = PlayerSettings()
            settings.setKey(MegaControllerButton.START, Input.Keys.I)

            settings.captureKey(MegaControllerButton.START, Input.Keys.K) shouldBe KeyCapture.ACCEPT

            settings.keyOf(MegaControllerButton.START) shouldBe Input.Keys.K
            settings.keyOf(MegaControllerButton.A) shouldBe Input.Keys.I
        }

        it("refuses the key of A for START on Enter, because the swap would move Enter to A") {
            val settings = PlayerSettings()
            val codes = settings.keyCodes.toList()

            settings.captureKey(MegaControllerButton.START, Input.Keys.K) shouldBe KeyCapture.REFUSE

            settings.keyCodes.toList() shouldBe codes
        }

        it("does not move a reserved key from a hand-edited table") {
            val settings = PlayerSettings()
            settings.setKey(MegaControllerButton.UP, Input.Keys.UP)

            settings.captureKey(MegaControllerButton.UP, Input.Keys.S) shouldBe KeyCapture.REFUSE
            settings.captureKey(MegaControllerButton.DOWN, Input.Keys.UP) shouldBe KeyCapture.REFUSE
            settings.captureKey(MegaControllerButton.UP, Input.Keys.UP) shouldBe KeyCapture.KEEP
        }

        it("leaves 8 different codes after every accept") {
            val freeKeys = listOf(Input.Keys.I, Input.Keys.O, Input.Keys.P, Input.Keys.Z)
            val boundKeys = MegaControllerButton.entries.map { it.defaultKeyboardKey }

            MegaControllerButton.entries.forEach { button ->
                (freeKeys + boundKeys).forEach { key ->
                    val settings = PlayerSettings()

                    if (settings.captureKey(button, key) == KeyCapture.ACCEPT)
                        settings.keyCodes.toSet().size shouldBe 8
                }
            }
        }

        it("accepts P for A, because P is not reserved") {
            val settings = PlayerSettings()

            settings.captureKey(MegaControllerButton.A, Input.Keys.P) shouldBe KeyCapture.ACCEPT

            settings.keyOf(MegaControllerButton.A) shouldBe Input.Keys.P
        }
    }

    describe("labels") {
        it("gives the key names of the real Input.Keys") {
            keyLabel(Input.Keys.K) shouldBe "K"
            keyLabel(Input.Keys.ENTER) shouldBe "Enter"
            keyLabel(Input.Keys.SHIFT_LEFT) shouldBe "L-Shift"
        }

        it("gives the raw number for a keyboard code out of range, with no exception") {
            listOf(-1, 256, 999).forEach { keyLabel(it) shouldBe it.toString() }
        }

        it("gives the raw number for a keyboard code in range with no name") {
            val code = (1..255).first { Input.Keys.toString(it) == null }

            keyLabel(code) shouldBe code.toString()
        }

        it("gives L2 and R2 for the trigger codes and the number for other gamepad codes") {
            padLabel(1004) shouldBe "L2"
            padLabel(1005) shouldBe "R2"
            padLabel(3) shouldBe "3"
            keyLabel(1004) shouldBe "1004"
        }
    }
})
